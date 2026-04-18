package dev.stat.chat.domain;

/**
 * Today's aggregated nutrition data from InfluxDB.
 * Total macros and calories consumed today (since 00:00).
 */
public record NutritionData(
        int calories,
        int proteinGrams,
        int carbsGrams,
        int fatGrams
) {
}
