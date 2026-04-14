package dev.stat.chat.service;

import dev.stat.chat.domain.NutritionData;

/**
 * System boundary: fetches today's nutrition data from Spoonacular API.
 * This interface exists to decouple the chat logic from the external API
 * and to make the boundary mockable in tests.
 */
public interface NutritionApiClient {

    /**
     * Retrieves today's aggregated nutrition data (calories, protein, carbs, fats).
     *
     * @return today's nutrition totals
     * @throws ExternalServiceException if Spoonacular is unreachable or times out
     */
    NutritionData fetchTodayNutrition();
}
