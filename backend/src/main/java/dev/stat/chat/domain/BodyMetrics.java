package dev.stat.chat.domain;

/**
 * Body metrics data: current weight and today's water intake.
 * Fetched from InfluxDB.
 */
public record BodyMetrics(
        double currentWeightKg,
        int todayWaterMl
) {
}
