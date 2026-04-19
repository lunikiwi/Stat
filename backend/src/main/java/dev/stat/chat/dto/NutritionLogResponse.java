package dev.stat.chat.dto;

/**
 * Response DTO for the nutrition logging endpoint.
 * Contains a confirmation message from the LLM.
 */
public record NutritionLogResponse(
        String message
) {
}
