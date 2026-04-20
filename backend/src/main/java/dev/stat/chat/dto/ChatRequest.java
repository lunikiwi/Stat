package dev.stat.chat.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

/**
 * Incoming chat request from the frontend.
 * The frontend is responsible for maintaining conversation state.
 * Optional imageBase64 field for vision-based meal tracking.
 */
public record ChatRequest(
        @NotBlank(message = "currentMessage must not be blank")
        String currentMessage,
        List<ChatMessage> chatHistory,
        String imageBase64
) {
}
