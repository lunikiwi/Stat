package dev.stat.chat.domain;

/**
 * Today's aggregated nutrition data from Spoonacular.
 * Total macros and calories consumed today.
 */
public record NutritionData(
        int calories,
        int proteinGrams,
        int carbsGrams,
        int fatGrams
) {
}
