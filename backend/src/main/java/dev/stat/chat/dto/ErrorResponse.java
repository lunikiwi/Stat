package dev.stat.chat.dto;

/**
 * Error response returned when an external service is unavailable.
 */
public record ErrorResponse(
        String error,
        String message
) {
}
