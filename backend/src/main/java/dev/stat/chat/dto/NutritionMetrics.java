package dev.stat.chat.dto;

/**
 * Nutrition metrics for the dashboard.
 * Contains today's aggregated nutrition data.
 */
public record NutritionMetrics(
        int calories,
        int protein,
        int carbs,
        int fat
) {
}
