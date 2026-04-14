package dev.stat.chat.service;

/**
 * System boundary: sends a fully-assembled prompt to the LLM and returns the reply.
 * This interface exists to decouple the chat logic from the specific LLM provider
 * and to make the boundary mockable in tests.
 */
public interface LlmClient {

    /**
     * Sends the assembled super-prompt to the LLM and returns the generated text.
     *
     * @param prompt the fully-assembled prompt including health data, nutrition, and chat history
     * @return the LLM-generated coaching reply
     * @throws ExternalServiceException if the LLM API is unreachable or times out
     */
    String chat(String prompt);
}
