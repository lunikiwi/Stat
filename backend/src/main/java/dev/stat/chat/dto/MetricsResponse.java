package dev.stat.chat.dto;

/**
 * Response DTO for GET /api/metrics endpoint.
 * Contains aggregated health and nutrition data for the dashboard.
 */
public record MetricsResponse(
        HealthMetrics health,
        NutritionMetrics nutrition
) {
}
