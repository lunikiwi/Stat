package dev.stat.chat.dto;

/**
 * Health metrics for the dashboard.
 * Contains sleep score, body battery, and training load.
 */
public record HealthMetrics(
        int sleepScore,
        int bodyBattery,
        int trainingLoadMinutes48h
) {
}
