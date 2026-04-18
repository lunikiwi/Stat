package dev.stat.chat.service;

import dev.stat.chat.domain.HealthData;
import dev.stat.chat.domain.NutritionData;

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
}
