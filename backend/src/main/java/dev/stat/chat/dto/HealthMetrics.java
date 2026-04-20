package dev.stat.chat.dto;

/**
 * Health metrics for the dashboard.
 * Contains sleep score, body battery, training load, current weight, and today's water intake.
 */
public record HealthMetrics(
        int sleepScore,
        int bodyBattery,
        int trainingLoadMinutes48h,
        double currentWeightKg,
        int todayWaterMl
) {
}
