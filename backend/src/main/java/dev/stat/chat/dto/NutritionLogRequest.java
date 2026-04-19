package dev.stat.chat.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for the Quick-Add nutrition logging endpoint.
 * Contains a free-text description of a meal that will be processed by the LLM.
 */
public record NutritionLogRequest(
        @NotBlank(message = "Description cannot be blank")
        String description
) {
}
