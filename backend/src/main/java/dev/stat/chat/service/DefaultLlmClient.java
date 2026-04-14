package dev.stat.chat.service;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Default OpenAI/Gemini-backed implementation of LlmClient.
 * Stub for now - will be implemented when LLM integration is built.
 */
@ApplicationScoped
public class DefaultLlmClient implements LlmClient {

    @Override
    public String chat(String prompt) {
        // TODO: implement LLM REST client
        throw new UnsupportedOperationException("LLM client not yet implemented");
    }
}
