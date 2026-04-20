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

    /**
     * Sends the assembled super-prompt with system context to the LLM and returns the generated text.
     *
     * @param prompt the fully-assembled prompt including health data, nutrition, and chat history
     * @param systemContext the system instruction defining the AI coach's persona and user context
     * @return the LLM-generated coaching reply
     * @throws ExternalServiceException if the LLM API is unreachable or times out
     */
    String chat(String prompt, String systemContext);

    /**
     * Sends the assembled super-prompt with system context and optional image to the LLM.
     *
     * @param prompt the fully-assembled prompt including health data, nutrition, and chat history
     * @param systemContext the system instruction defining the AI coach's persona and user context
     * @param imageBase64 optional Base64-encoded image for vision analysis
     * @return the LLM-generated coaching reply
     * @throws ExternalServiceException if the LLM API is unreachable or times out
     */
    String chat(String prompt, String systemContext, String imageBase64);
}
