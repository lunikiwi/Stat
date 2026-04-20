package dev.stat.chat.service;

import dev.stat.chat.domain.BodyMetrics;
import dev.stat.chat.domain.HealthData;
import dev.stat.chat.domain.NutritionData;
import dev.stat.chat.domain.NutritionEntry;
import dev.stat.chat.domain.TrendData;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * System boundary: fetches aggregated health metrics from InfluxDB.
 * This interface exists to decouple the chat logic from the InfluxDB client
 * and to make the boundary mockable in tests.
 */
public interface HealthDataClient {

    /**
     * Retrieves aggregated health data:
     * - Sleep score from previous night
     * - Training load (minutes) from last 48 hours
     * - Most recent Body Battery value
     *
     * @return aggregated health data
     * @throws ExternalServiceException if InfluxDB is unreachable or query fails
     */
    HealthData fetchCurrentHealthData();

    /**
     * Fetches today's nutrition data from InfluxDB.
     * Aggregates nutrition_calories, nutrition_protein, nutrition_carbs, nutrition_fat
     * from today (since 00:00).
     *
     * @return today's aggregated nutrition data
     * @throws ExternalServiceException if InfluxDB query fails or data is incomplete
     */
    NutritionData fetchTodayNutrition();

    /**
     * Logs nutrition data to InfluxDB using Line Protocol.
     * Writes four measurements: nutrition_calories, nutrition_protein,
     * nutrition_carbs, and nutrition_fat with the current timestamp.
     *
     * @param calories total calories consumed
     * @param protein protein in grams
     * @param carbs carbohydrates in grams
     * @param fat fat in grams
     * @throws ExternalServiceException if InfluxDB write fails
     */
    void logNutrition(int calories, int protein, int carbs, int fat);

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
    TrendData fetch7DayAverages();

    /**
     * Logs body weight to InfluxDB using Line Protocol.
     * Writes body_weight measurement with the current timestamp.
     *
     * @param kg body weight in kilograms
     * @throws ExternalServiceException if InfluxDB write fails
     */
    void logWeight(double kg);

    /**
     * Logs water intake to InfluxDB using Line Protocol.
     * Writes water_intake measurement with the current timestamp.
     *
     * @param ml water intake in milliliters
     * @throws ExternalServiceException if InfluxDB write fails
     */
    void logWater(int ml);

    /**
     * Fetches current body metrics from InfluxDB:
     * - Most recent body weight (last 7 days)
     * - Today's total water intake (since 00:00)
     *
     * @return current body metrics
     * @throws ExternalServiceException if InfluxDB query fails or data is incomplete
     */
    BodyMetrics fetchBodyMetrics();

    /**
     * Fetches nutrition entries for a specific date from InfluxDB.
     * Groups nutrition data points by their exact timestamp to form individual meals/entries.
     *
     * @param date the date to fetch entries for
     * @return list of nutrition entries, grouped by timestamp
     * @throws ExternalServiceException if InfluxDB query fails
     */
    List<NutritionEntry> getNutritionEntries(LocalDate date);

    /**
     * Deletes a nutrition entry from InfluxDB by its exact timestamp.
     * Removes all nutrition measurements (calories, protein, carbs, fat) with the given timestamp.
     *
     * @param timestamp the exact timestamp of the entry to delete
     * @throws ExternalServiceException if InfluxDB delete fails
     */
    void deleteNutritionEntry(Instant timestamp);
}
