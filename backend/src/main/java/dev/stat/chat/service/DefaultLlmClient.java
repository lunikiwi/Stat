package dev.stat.chat.service;

import dev.stat.chat.client.LlmRestClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.util.List;

/**
 * Default Google Gemini-backed implementation of LlmClient.
 * Sends prompts to Gemini API with timeout and error handling.
 */
@ApplicationScoped
public class DefaultLlmClient implements LlmClient {

    private static final Logger LOG = Logger.getLogger(DefaultLlmClient.class);

    @RestClient
    LlmRestClient llmRestClient;

    @ConfigProperty(name = "stat.llm.api-key")
    String apiKey;

    @ConfigProperty(name = "stat.llm.model")
    String model;

    @ConfigProperty(name = "stat.llm.temperature", defaultValue = "0.7")
    double temperature;

    @ConfigProperty(name = "stat.llm.max-tokens", defaultValue = "500")
    int maxTokens;

    @Override
    public String chat(String prompt) {
        try {
            LOG.debugf("Sending prompt to Gemini (model: %s)", model);

            // Build Gemini API request
            LlmRestClient.ChatCompletionRequest request = new LlmRestClient.ChatCompletionRequest(
                    List.of(new LlmRestClient.Content(
                            List.of(new LlmRestClient.Part(prompt))
                    )),
                    new LlmRestClient.GenerationConfig(temperature, maxTokens)
            );

            // Call Gemini API (API key is passed as query parameter)
            LlmRestClient.ChatCompletionResponse response =
                    llmRestClient.generateContent(model, apiKey, request);

            return extractResponseContent(response);

        } catch (WebApplicationException e) {
            // HTTP errors (4xx, 5xx)
            int status = e.getResponse().getStatus();
            LOG.errorf(e, "Gemini API returned HTTP %d", status);

            if (status == 429) {
                throw new ExternalServiceException(
                        "LLM", "Gemini API rate limit exceeded (HTTP 429)", e);
            } else if (status >= 500) {
                throw new ExternalServiceException(
                        "LLM", "Gemini API server error (HTTP " + status + ")", e);
            } else if (status == 401 || status == 403) {
                throw new ExternalServiceException(
                        "LLM", "Gemini API authentication failed (HTTP " + status + ")", e);
            } else {
                throw new ExternalServiceException(
                        "LLM", "Gemini API request failed (HTTP " + status + ")", e);
            }

        } catch (ProcessingException e) {
            // Timeout or connection errors
            LOG.errorf(e, "Failed to connect to Gemini API");

            if (e.getCause() instanceof java.net.SocketTimeoutException ||
                e.getMessage().contains("timeout")) {
                throw new ExternalServiceException(
                        "LLM", "Gemini API did not respond within timeout", e);
            } else {
                throw new ExternalServiceException(
                        "LLM", "Gemini API connection failed: " + e.getMessage(), e);
            }

        } catch (Exception e) {
            LOG.errorf(e, "Unexpected error calling Gemini API");
            throw new ExternalServiceException(
                    "LLM", "Gemini API request failed: " + e.getMessage(), e);
        }
    }

    /**
     * Extracts the assistant's message content from the Gemini response.
     */
    private String extractResponseContent(LlmRestClient.ChatCompletionResponse response) {
        if (response == null || response.candidates() == null || response.candidates().isEmpty()) {
            throw new ExternalServiceException("LLM", "Gemini API returned empty response");
        }

        LlmRestClient.Candidate firstCandidate = response.candidates().get(0);
        if (firstCandidate.content() == null ||
            firstCandidate.content().parts() == null ||
            firstCandidate.content().parts().isEmpty()) {
            throw new ExternalServiceException("LLM", "Gemini API response missing content");
        }

        LlmRestClient.Part firstPart = firstCandidate.content().parts().get(0);
        if (firstPart.text() == null) {
            throw new ExternalServiceException("LLM", "Gemini API response missing text");
        }

        return firstPart.text();
    }
}
