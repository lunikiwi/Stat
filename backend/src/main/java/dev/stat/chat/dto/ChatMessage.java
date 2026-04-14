package dev.stat.chat.dto;

/**
 * A single message in the conversation history.
 * Role is either "user" or "assistant".
 */
public record ChatMessage(String role, String content) {
}
