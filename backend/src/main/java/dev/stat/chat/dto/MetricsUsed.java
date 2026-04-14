package dev.stat.chat.dto;

/**
 * Health metrics that were used to generate the coaching reply.
 * Included in the response so the frontend can display context.
 */
public record MetricsUsed(
        int bodyBattery,
        int sleepScore,
        String trainingLoad48h
) {
}
