package dev.stat.chat.dto;

/**
 * Successful chat response containing the LLM-generated reply
 * and the health metrics that informed it.
 */
public record ChatResponse(
        String reply,
        MetricsUsed metricsUsed
) {
}
