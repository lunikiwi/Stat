package dev.stat.chat.domain;

/**
 * Represents 7-day average trends for key health metrics.
 * Used to provide historical context to the AI coach for trend analysis.
 */
public record TrendData(
        int avgSleepScore,
        int avgBodyBattery,
        int avgCalories
) {
}
