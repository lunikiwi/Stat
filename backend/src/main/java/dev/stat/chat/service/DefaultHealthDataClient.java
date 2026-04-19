package dev.stat.chat.service;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import dev.stat.chat.config.InfluxDBConfig;
import dev.stat.chat.domain.HealthData;
import dev.stat.chat.domain.NutritionData;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Default InfluxDB-backed implementation of HealthDataClient.
 * Executes Flux queries to aggregate health metrics according to spec:
 * - Sleep Score: previous night (last 24h)
 * - Training Load: sum of last 48h
 * - Body Battery: most recent value
 * - Nutrition Data: sum of today's nutrition values (since 00:00)
 *
 * Implements retry logic for transient connection failures.
 */
@ApplicationScoped
public class DefaultHealthDataClient implements HealthDataClient {

    private static final Logger LOG = Logger.getLogger(DefaultHealthDataClient.class);
    private static final int MAX_RETRIES = 2;
    private static final long RETRY_DELAY_MS = 500;

    @Inject
    InfluxDBClient influxDBClient;

    @Inject
    InfluxDBConfig influxDBConfig;

    @Override
    public HealthData fetchCurrentHealthData() {
        return executeWithRetry(() -> {
            QueryApi queryApi = influxDBClient.getQueryApi();
            String fluxQuery = buildFluxQuery();

            LOG.debugf("Executing Flux query: %s", fluxQuery);

            List<FluxTable> tables = queryApi.query(fluxQuery, influxDBConfig.getOrg());

            Map<String, Double> metrics = extractMetrics(tables);

            validateMetrics(metrics);

            return new HealthData(
                    metrics.get("body_battery").intValue(),
                    metrics.get("sleep_score").intValue(),
                    metrics.get("training_minutes").intValue()
            );
        }, "fetch health data");
    }

    /**
     * Executes an operation with retry logic for transient failures.
     * Retries up to MAX_RETRIES times with exponential backoff.
     */
    private <T> T executeWithRetry(InfluxOperation<T> operation, String operationName) {
        Exception lastException = null;

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                return operation.execute();
            } catch (Exception e) {
                lastException = e;

                if (attempt < MAX_RETRIES && isRetryableException(e)) {
                    long delay = RETRY_DELAY_MS * attempt;
                    LOG.warnf("InfluxDB %s failed (attempt %d/%d): %s. Retrying in %dms...",
                            operationName, attempt, MAX_RETRIES, e.getMessage(), delay);

                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                } else {
                    break;
                }
            }
        }

        LOG.errorf(lastException, "Failed to %s from InfluxDB after %d attempts", operationName, MAX_RETRIES);
        throw new ExternalServiceException("InfluxDB",
                "InfluxDB query failed: " + lastException.getMessage(), lastException);
    }

    /**
     * Determines if an exception is retryable (transient network/connection issues).
     */
    private boolean isRetryableException(Exception e) {
        String message = e.getMessage();
        if (message == null) {
            return false;
        }

        // Retry on connection-related errors
        return message.contains("Connection reset") ||
               message.contains("Connection refused") ||
               message.contains("timeout") ||
               message.contains("Broken pipe") ||
               message.contains("Socket closed");
    }

    @FunctionalInterface
    private interface InfluxOperation<T> {
        T execute() throws Exception;
    }

    /**
     * Builds the Flux query to aggregate all required health metrics.
     *
     * Query structure:
     * 1. Body Battery: most recent value (last 1h window)
     * 2. Sleep Score: last night's score (last 24h window)
     * 3. Training Load: sum of training minutes (last 48h window)
     */
    private String buildFluxQuery() {
        String bucket = influxDBConfig.getBucket();
        Instant now = Instant.now();

        // Body Battery: most recent value
        String bodyBatteryQuery = String.format("""
            bodyBattery = from(bucket: "%s")
              |> range(start: -1h)
              |> filter(fn: (r) => r["_measurement"] == "body_battery")
              |> filter(fn: (r) => r["_field"] == "value")
              |> last()
              |> set(key: "metric", value: "body_battery")
            """, bucket);

        // Sleep Score: previous night (last 24h)
        String sleepScoreQuery = String.format("""
            sleepScore = from(bucket: "%s")
              |> range(start: -24h)
              |> filter(fn: (r) => r["_measurement"] == "sleep_score")
              |> filter(fn: (r) => r["_field"] == "value")
              |> last()
              |> set(key: "metric", value: "sleep_score")
            """, bucket);

        // Training Load: sum of last 48h
        String trainingLoadQuery = String.format("""
            trainingLoad = from(bucket: "%s")
              |> range(start: -48h)
              |> filter(fn: (r) => r["_measurement"] == "training_minutes")
              |> filter(fn: (r) => r["_field"] == "value")
              |> sum()
              |> set(key: "metric", value: "training_minutes")
            """, bucket);

        // Union all queries
        return bodyBatteryQuery + "\n" +
               sleepScoreQuery + "\n" +
               trainingLoadQuery + "\n" +
               "union(tables: [bodyBattery, sleepScore, trainingLoad])";
    }

    /**
     * Extracts metric values from Flux query results.
     */
    private Map<String, Double> extractMetrics(List<FluxTable> tables) {
        return tables.stream()
                .flatMap(table -> table.getRecords().stream())
                .collect(Collectors.toMap(
                        record -> record.getValueByKey("metric").toString(),
                        record -> {
                            Object value = record.getValue();
                            if (value instanceof Number) {
                                return ((Number) value).doubleValue();
                            }
                            throw new IllegalStateException("Non-numeric value in InfluxDB result: " + value);
                        }
                ));
    }

    /**
     * Validates that all required metrics are present.
     */
    private void validateMetrics(Map<String, Double> metrics) {
        List<String> requiredMetrics = List.of("body_battery", "sleep_score", "training_minutes");
        List<String> missingMetrics = requiredMetrics.stream()
                .filter(metric -> !metrics.containsKey(metric))
                .toList();

        if (!missingMetrics.isEmpty()) {
            throw new ExternalServiceException(
                    "InfluxDB",
                    "InfluxDB returned incomplete data. Missing metrics: " + String.join(", ", missingMetrics)
            );
        }
    }

    /**
     * Fetches today's nutrition data from InfluxDB.
     * Aggregates nutrition_calories, nutrition_protein, nutrition_carbs, nutrition_fat
     * from today (since 00:00).
     *
     * @return today's aggregated nutrition data
     * @throws ExternalServiceException if InfluxDB query fails or data is incomplete
     */
    public NutritionData fetchTodayNutrition() {
        try {
            QueryApi queryApi = influxDBClient.getQueryApi();
            String fluxQuery = buildNutritionFluxQuery();

            LOG.debugf("Executing nutrition Flux query: %s", fluxQuery);

            List<FluxTable> tables = queryApi.query(fluxQuery, influxDBConfig.getOrg());

            Map<String, Double> metrics = extractMetrics(tables);

            validateNutritionMetrics(metrics);

            return new NutritionData(
                    metrics.get("nutrition_calories").intValue(),
                    metrics.get("nutrition_protein").intValue(),
                    metrics.get("nutrition_carbs").intValue(),
                    metrics.get("nutrition_fat").intValue()
            );

        } catch (Exception e) {
            LOG.errorf(e, "Failed to fetch nutrition data from InfluxDB");
            throw new ExternalServiceException("InfluxDB", "InfluxDB nutrition query failed: " + e.getMessage(), e);
        }
    }

    /**
     * Builds the Flux query to aggregate today's nutrition metrics.
     * Sums all nutrition values from today (since 00:00 local time).
     */
    private String buildNutritionFluxQuery() {
        String bucket = influxDBConfig.getBucket();

        // Calculate start of today in UTC
        ZonedDateTime startOfToday = LocalDate.now()
                .atStartOfDay(ZoneId.systemDefault())
                .withZoneSameInstant(ZoneId.of("UTC"));

        String startTime = startOfToday.toInstant().toString();

        // Calories
        String caloriesQuery = String.format("""
            calories = from(bucket: "%s")
              |> range(start: %s)
              |> filter(fn: (r) => r["_measurement"] == "nutrition_calories")
              |> filter(fn: (r) => r["_field"] == "value")
              |> sum()
              |> set(key: "metric", value: "nutrition_calories")
            """, bucket, startTime);

        // Protein
        String proteinQuery = String.format("""
            protein = from(bucket: "%s")
              |> range(start: %s)
              |> filter(fn: (r) => r["_measurement"] == "nutrition_protein")
              |> filter(fn: (r) => r["_field"] == "value")
              |> sum()
              |> set(key: "metric", value: "nutrition_protein")
            """, bucket, startTime);

        // Carbs
        String carbsQuery = String.format("""
            carbs = from(bucket: "%s")
              |> range(start: %s)
              |> filter(fn: (r) => r["_measurement"] == "nutrition_carbs")
              |> filter(fn: (r) => r["_field"] == "value")
              |> sum()
              |> set(key: "metric", value: "nutrition_carbs")
            """, bucket, startTime);

        // Fat
        String fatQuery = String.format("""
            fat = from(bucket: "%s")
              |> range(start: %s)
              |> filter(fn: (r) => r["_measurement"] == "nutrition_fat")
              |> filter(fn: (r) => r["_field"] == "value")
              |> sum()
              |> set(key: "metric", value: "nutrition_fat")
            """, bucket, startTime);

        // Union all queries
        return caloriesQuery + "\n" +
               proteinQuery + "\n" +
               carbsQuery + "\n" +
               fatQuery + "\n" +
               "union(tables: [calories, protein, carbs, fat])";
    }

    /**
     * Validates that all required nutrition metrics are present.
     */
    private void validateNutritionMetrics(Map<String, Double> metrics) {
        List<String> requiredMetrics = List.of("nutrition_calories", "nutrition_protein", "nutrition_carbs", "nutrition_fat");
        List<String> missingMetrics = requiredMetrics.stream()
                .filter(metric -> !metrics.containsKey(metric))
                .toList();

        if (!missingMetrics.isEmpty()) {
            throw new ExternalServiceException(
                    "InfluxDB",
                    "InfluxDB returned incomplete nutrition data. Missing metrics: " + String.join(", ", missingMetrics)
            );
       }
   }

   /**
    * Logs nutrition data to InfluxDB using Line Protocol.
    * Writes four measurements with the current timestamp:
    * - nutrition_calories
    * - nutrition_protein
    * - nutrition_carbs
    * - nutrition_fat
    *
    * @param calories total calories consumed
    * @param protein protein in grams
    * @param carbs carbohydrates in grams
    * @param fat fat in grams
    * @throws ExternalServiceException if InfluxDB write fails
    */
   @Override
   public void logNutrition(int calories, int protein, int carbs, int fat) {
       executeWithRetry(() -> {
           WriteApiBlocking writeApi = influxDBClient.getWriteApiBlocking();
           long timestamp = Instant.now().toEpochMilli();

           // Write each nutrition metric as a separate measurement
           String caloriesRecord = String.format("nutrition_calories value=%d %d", calories, timestamp);
           String proteinRecord = String.format("nutrition_protein value=%d %d", protein, timestamp);
           String carbsRecord = String.format("nutrition_carbs value=%d %d", carbs, timestamp);
           String fatRecord = String.format("nutrition_fat value=%d %d", fat, timestamp);

           writeApi.writeRecord(WritePrecision.MS, caloriesRecord);
           writeApi.writeRecord(WritePrecision.MS, proteinRecord);
           writeApi.writeRecord(WritePrecision.MS, carbsRecord);
           writeApi.writeRecord(WritePrecision.MS, fatRecord);

           LOG.infof("Logged nutrition: %d cal, %dg protein, %dg carbs, %dg fat", calories, protein, carbs, fat);

           return null; // Void operation
       }, "log nutrition data");
   }
}
