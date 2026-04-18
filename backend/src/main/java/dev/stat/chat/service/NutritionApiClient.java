package dev.stat.chat.service;

import dev.stat.chat.domain.NutritionData;

/**
 * System boundary: fetches today's nutrition data from InfluxDB.
 * This interface exists to decouple the chat logic from the data source
 * and to make the boundary mockable in tests.
 */
public interface NutritionApiClient {

    /**
     * Retrieves today's aggregated nutrition data (calories, protein, carbs, fats).
     *
     * @return today's nutrition totals (since 00:00)
     * @throws ExternalServiceException if InfluxDB is unreachable or query fails
     */
    NutritionData fetchTodayNutrition();
}
