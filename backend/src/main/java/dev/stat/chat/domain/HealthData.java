package dev.stat.chat.domain;

/**
 * Aggregated health data from InfluxDB (Garmin).
 * Contains the data windows defined in the spec:
 * - Sleep: previous night's score
 * - Training Load: last 48h duration in minutes
 * - Body Battery: most recent value
 */
public record HealthData(
        int bodyBattery,
        int sleepScore,
        int trainingLoadMinutes48h
) {
}
