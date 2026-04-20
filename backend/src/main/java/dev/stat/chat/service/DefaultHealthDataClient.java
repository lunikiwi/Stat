package dev.stat.chat.service;

import com.influxdb.client.DeleteApi;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import dev.stat.chat.config.InfluxDBConfig;
import dev.stat.chat.domain.BodyMetrics;
import dev.stat.chat.domain.HealthData;
import dev.stat.chat.domain.NutritionData;
import dev.stat.chat.domain.NutritionEntry;
import dev.stat.chat.domain.TrendData;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
     * 1. Body Battery: most recent value (last 24h window to ensure data availability)
     * 2. Sleep Score: last night's score (last 24h window)
     * 3. Training Load: sum of training minutes (last 48h window)
     */
    private String buildFluxQuery() {
        String bucket = influxDBConfig.getBucket();
        Instant now = Instant.now();

        // Body Battery: most recent value (24h window to handle infrequent updates)
        String bodyBatteryQuery = String.format("""
            bodyBattery = from(bucket: "%s")
              |> range(start: -24h)
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

   /**
    * Fetches 7-day average trends for key health metrics.
    * Calculates the mean values over the last 7 days for:
    * - Sleep score
    * - Body Battery
    * - Calorie intake
    *
    * @return 7-day average trend data
    * @throws ExternalServiceException if InfluxDB query fails or data is incomplete
    */
   @Override
   public TrendData fetch7DayAverages() {
       return executeWithRetry(() -> {
           QueryApi queryApi = influxDBClient.getQueryApi();
           String fluxQuery = build7DayAveragesFluxQuery();

           LOG.debugf("Executing 7-day averages Flux query: %s", fluxQuery);

           List<FluxTable> tables = queryApi.query(fluxQuery, influxDBConfig.getOrg());

           Map<String, Double> metrics = extractMetrics(tables);

           validate7DayAverageMetrics(metrics);

           return new TrendData(
                   metrics.get("avg_sleep_score").intValue(),
                   metrics.get("avg_body_battery").intValue(),
                   metrics.get("avg_calories").intValue()
           );
       }, "fetch 7-day averages");
   }

   /**
    * Builds the Flux query to calculate 7-day averages for trend analysis.
    * Calculates mean values over the last 7 days for:
    * - Sleep score
    * - Body Battery
    * - Calorie intake
    */
   private String build7DayAveragesFluxQuery() {
       String bucket = influxDBConfig.getBucket();

       // Average Sleep Score over last 7 days
       String avgSleepQuery = String.format("""
           avgSleep = from(bucket: "%s")
             |> range(start: -7d)
             |> filter(fn: (r) => r["_measurement"] == "sleep_score")
             |> filter(fn: (r) => r["_field"] == "value")
             |> mean()
             |> set(key: "metric", value: "avg_sleep_score")
           """, bucket);

       // Average Body Battery over last 7 days
       String avgBodyBatteryQuery = String.format("""
           avgBodyBattery = from(bucket: "%s")
             |> range(start: -7d)
             |> filter(fn: (r) => r["_measurement"] == "body_battery")
             |> filter(fn: (r) => r["_field"] == "value")
             |> mean()
             |> set(key: "metric", value: "avg_body_battery")
           """, bucket);

       // Average Calories over last 7 days
       // First aggregate daily totals, then calculate mean
       String avgCaloriesQuery = String.format("""
           avgCalories = from(bucket: "%s")
             |> range(start: -7d)
             |> filter(fn: (r) => r["_measurement"] == "nutrition_calories")
             |> filter(fn: (r) => r["_field"] == "value")
             |> aggregateWindow(every: 1d, fn: sum, createEmpty: false)
             |> mean()
             |> set(key: "metric", value: "avg_calories")
           """, bucket);

       // Union all queries
       return avgSleepQuery + "\n" +
              avgBodyBatteryQuery + "\n" +
              avgCaloriesQuery + "\n" +
              "union(tables: [avgSleep, avgBodyBattery, avgCalories])";
   }

   /**
    * Validates that all required 7-day average metrics are present.
    */
   private void validate7DayAverageMetrics(Map<String, Double> metrics) {
       List<String> requiredMetrics = List.of("avg_sleep_score", "avg_body_battery", "avg_calories");
       List<String> missingMetrics = requiredMetrics.stream()
               .filter(metric -> !metrics.containsKey(metric))
               .toList();

       if (!missingMetrics.isEmpty()) {
           throw new ExternalServiceException(
                   "InfluxDB",
                   "InfluxDB returned incomplete 7-day average data. Missing metrics: " + String.join(", ", missingMetrics)
           );
       }
   }

   /**
    * Logs body weight to InfluxDB using Line Protocol.
    * Writes body_weight measurement with the current timestamp.
    *
    * @param kg body weight in kilograms
    * @throws ExternalServiceException if InfluxDB write fails
    */
   @Override
   public void logWeight(double kg) {
       executeWithRetry(() -> {
           WriteApiBlocking writeApi = influxDBClient.getWriteApiBlocking();
           long timestamp = Instant.now().toEpochMilli();

           String weightRecord = String.format("body_weight value=%.1f %d", kg, timestamp);
           writeApi.writeRecord(WritePrecision.MS, weightRecord);

           LOG.infof("Logged weight: %.1f kg", kg);

           return null; // Void operation
       }, "log weight data");
   }

   /**
    * Logs water intake to InfluxDB using Line Protocol.
    * Writes water_intake measurement with the current timestamp.
    *
    * @param ml water intake in milliliters
    * @throws ExternalServiceException if InfluxDB write fails
    */
   @Override
   public void logWater(int ml) {
       executeWithRetry(() -> {
           WriteApiBlocking writeApi = influxDBClient.getWriteApiBlocking();
           long timestamp = Instant.now().toEpochMilli();

           String waterRecord = String.format("water_intake value=%d %d", ml, timestamp);
           writeApi.writeRecord(WritePrecision.MS, waterRecord);

           LOG.infof("Logged water intake: %d ml", ml);

           return null; // Void operation
       }, "log water intake data");
   }

   /**
    * Fetches current body metrics from InfluxDB:
    * - Most recent body weight (last 7 days)
    * - Today's total water intake (since 00:00)
    *
    * @return current body metrics
    * @throws ExternalServiceException if InfluxDB query fails or data is incomplete
    */
   @Override
   public BodyMetrics fetchBodyMetrics() {
       return executeWithRetry(() -> {
           QueryApi queryApi = influxDBClient.getQueryApi();
           String fluxQuery = buildBodyMetricsFluxQuery();

           LOG.debugf("Executing body metrics Flux query: %s", fluxQuery);

           List<FluxTable> tables = queryApi.query(fluxQuery, influxDBConfig.getOrg());

           Map<String, Double> metrics = extractMetrics(tables);

           validateBodyMetrics(metrics);

           return new BodyMetrics(
                   metrics.get("current_weight"),
                   metrics.get("today_water").intValue()
           );
       }, "fetch body metrics");
   }

   /**
    * Builds the Flux query to fetch current body metrics.
    * - Current weight: most recent value (last 7 days)
    * - Today's water: sum of water intake since 00:00
    */
   private String buildBodyMetricsFluxQuery() {
       String bucket = influxDBConfig.getBucket();

       // Calculate start of today in UTC
       ZonedDateTime startOfToday = LocalDate.now()
               .atStartOfDay(ZoneId.systemDefault())
               .withZoneSameInstant(ZoneId.of("UTC"));

       String startTime = startOfToday.toInstant().toString();

       // Current weight: most recent value (7-day window)
       String weightQuery = String.format("""
           currentWeight = from(bucket: "%s")
             |> range(start: -7d)
             |> filter(fn: (r) => r["_measurement"] == "body_weight")
             |> filter(fn: (r) => r["_field"] == "value")
             |> last()
             |> set(key: "metric", value: "current_weight")
           """, bucket);

       // Today's water intake: sum since 00:00
       String waterQuery = String.format("""
           todayWater = from(bucket: "%s")
             |> range(start: %s)
             |> filter(fn: (r) => r["_measurement"] == "water_intake")
             |> filter(fn: (r) => r["_field"] == "value")
             |> sum()
             |> set(key: "metric", value: "today_water")
           """, bucket, startTime);

       // Union both queries
       return weightQuery + "\n" +
              waterQuery + "\n" +
              "union(tables: [currentWeight, todayWater])";
   }

   /**
    * Validates that all required body metrics are present.
    */
   private void validateBodyMetrics(Map<String, Double> metrics) {
       List<String> requiredMetrics = List.of("current_weight", "today_water");
       List<String> missingMetrics = requiredMetrics.stream()
               .filter(metric -> !metrics.containsKey(metric))
               .toList();

       if (!missingMetrics.isEmpty()) {
           throw new ExternalServiceException(
                   "InfluxDB",
                   "InfluxDB returned incomplete body metrics data. Missing metrics: " + String.join(", ", missingMetrics)
           );
       }
   }

   /**
    * Fetches nutrition entries for a specific date from InfluxDB.
    * Groups nutrition data points by their exact timestamp to form individual meals/entries.
    *
    * @param date the date to fetch entries for
    * @return list of nutrition entries, grouped by timestamp
    * @throws ExternalServiceException if InfluxDB query fails
    */
   @Override
   public List<NutritionEntry> getNutritionEntries(LocalDate date) {
       return executeWithRetry(() -> {
           QueryApi queryApi = influxDBClient.getQueryApi();
           String fluxQuery = buildNutritionEntriesFluxQuery(date);

           LOG.debugf("Executing nutrition entries Flux query: %s", fluxQuery);

           List<FluxTable> tables = queryApi.query(fluxQuery, influxDBConfig.getOrg());

           // Group records by timestamp
           Map<Instant, Map<String, Double>> groupedByTimestamp = new LinkedHashMap<>();

           for (FluxTable table : tables) {
               for (FluxRecord record : table.getRecords()) {
                   Instant timestamp = record.getTime();
                   String measurement = record.getMeasurement();
                   Object value = record.getValue();

                   if (timestamp != null && measurement != null && value instanceof Number) {
                       groupedByTimestamp
                               .computeIfAbsent(timestamp, k -> new LinkedHashMap<>())
                               .put(measurement, ((Number) value).doubleValue());
                   }
               }
           }

           // Convert grouped data to NutritionEntry objects
           List<NutritionEntry> entries = new ArrayList<>();
           for (Map.Entry<Instant, Map<String, Double>> entry : groupedByTimestamp.entrySet()) {
               Instant timestamp = entry.getKey();
               Map<String, Double> metrics = entry.getValue();

               // Only create entry if we have all four nutrition metrics
               if (metrics.containsKey("nutrition_calories") &&
                   metrics.containsKey("nutrition_protein") &&
                   metrics.containsKey("nutrition_carbs") &&
                   metrics.containsKey("nutrition_fat")) {

                   entries.add(new NutritionEntry(
                           timestamp,
                           metrics.get("nutrition_calories").intValue(),
                           metrics.get("nutrition_protein").intValue(),
                           metrics.get("nutrition_carbs").intValue(),
                           metrics.get("nutrition_fat").intValue()
                   ));
               }
           }

           LOG.debugf("Found %d nutrition entries for date %s", entries.size(), date);
           return entries;

       }, "fetch nutrition entries");
   }

   /**
    * Builds the Flux query to fetch all nutrition entries for a specific date.
    * Returns individual data points with their timestamps (not aggregated).
    */
   private String buildNutritionEntriesFluxQuery(LocalDate date) {
       String bucket = influxDBConfig.getBucket();

       // Calculate start and end of the specified date in UTC
       ZonedDateTime startOfDay = date.atStartOfDay(ZoneId.systemDefault())
               .withZoneSameInstant(ZoneId.of("UTC"));
       ZonedDateTime endOfDay = startOfDay.plusDays(1);

       String startTime = startOfDay.toInstant().toString();
       String endTime = endOfDay.toInstant().toString();

       // Query all nutrition measurements for the date
       return String.format("""
           from(bucket: "%s")
             |> range(start: %s, stop: %s)
             |> filter(fn: (r) => r["_measurement"] =~ /^nutrition_/)
             |> filter(fn: (r) => r["_field"] == "value")
             |> sort(columns: ["_time"])
           """, bucket, startTime, endTime);
   }

   /**
    * Deletes a nutrition entry from InfluxDB by its exact timestamp.
    * Removes all nutrition measurements (calories, protein, carbs, fat) with the given timestamp.
    *
    * @param timestamp the exact timestamp of the entry to delete
    * @throws ExternalServiceException if InfluxDB delete fails
    */
   @Override
   public void deleteNutritionEntry(Instant timestamp) {
       executeWithRetry(() -> {
           DeleteApi deleteApi = influxDBClient.getDeleteApi();

           // Convert Instant to OffsetDateTime (required by InfluxDB DeleteApi)
           OffsetDateTime start = timestamp.atOffset(ZoneId.of("UTC").getRules().getOffset(timestamp));
           OffsetDateTime stop = start.plusNanos(1); // Delete only this exact timestamp

           LOG.debugf("Deleting nutrition entry at timestamp %s", timestamp);

           // InfluxDB Delete API doesn't support OR or regex operators
           // We need to make separate delete calls for each measurement
           String[] measurements = {"nutrition_calories", "nutrition_protein", "nutrition_carbs", "nutrition_fat"};

           for (String measurement : measurements) {
               String predicate = "_measurement=\"" + measurement + "\"";
               try {
                   deleteApi.delete(start, stop, predicate, influxDBConfig.getBucket(), influxDBConfig.getOrg());
                   LOG.debugf("Deleted %s at timestamp %s", measurement, timestamp);
               } catch (Exception e) {
                   LOG.warnf("Failed to delete %s at timestamp %s: %s", measurement, timestamp, e.getMessage());
                   // Continue with other measurements even if one fails
               }
           }

           LOG.infof("Deleted nutrition entry at timestamp %s", timestamp);

           return null; // Void operation
       }, "delete nutrition entry");
   }
}
