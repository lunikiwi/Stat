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
 * Default OpenAI/Gemini-backed implementation of LlmClient.
 * Sends prompts to LLM API with timeout and error handling.
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
            LOG.debugf("Sending prompt to LLM (model: %s)", model);

            LlmRestClient.ChatCompletionRequest request = new LlmRestClient.ChatCompletionRequest(
                    model,
                    List.of(new LlmRestClient.Message("user", prompt)),
                    temperature,
                    maxTokens
            );

            String authorization = "Bearer " + apiKey;
            LlmRestClient.ChatCompletionResponse response =
                    llmRestClient.createChatCompletion(authorization, request);

            return extractResponseContent(response);

        } catch (WebApplicationException e) {
            // HTTP errors (4xx, 5xx)
            int status = e.getResponse().getStatus();
            LOG.errorf(e, "LLM API returned HTTP %d", status);

            if (status == 429) {
                throw new ExternalServiceException(
                        "LLM API rate limit exceeded (HTTP 429)", e);
            } else if (status >= 500) {
                throw new ExternalServiceException(
                        "LLM API server error (HTTP " + status + ")", e);
            } else if (status == 401) {
                throw new ExternalServiceException(
                        "LLM API authentication failed (HTTP 401)", e);
            } else {
                throw new ExternalServiceException(
                        "LLM API request failed (HTTP " + status + ")", e);
            }

        } catch (ProcessingException e) {
            // Timeout or connection errors
            LOG.errorf(e, "Failed to connect to LLM API");

            if (e.getCause() instanceof java.net.SocketTimeoutException ||
                e.getMessage().contains("timeout")) {
                throw new ExternalServiceException(
                        "LLM API did not respond within timeout", e);
            } else {
                throw new ExternalServiceException(
                        "LLM API connection failed: " + e.getMessage(), e);
            }

        } catch (Exception e) {
            LOG.errorf(e, "Unexpected error calling LLM API");
            throw new ExternalServiceException(
                    "LLM API request failed: " + e.getMessage(), e);
        }
    }

    /**
     * Extracts the assistant's message content from the LLM response.
     */
    private String extractResponseContent(LlmRestClient.ChatCompletionResponse response) {
        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new ExternalServiceException("LLM API returned empty response");
        }

        LlmRestClient.Choice firstChoice = response.choices().get(0);
        if (firstChoice.message() == null || firstChoice.message().content() == null) {
            throw new ExternalServiceException("LLM API response missing message content");
        }

        return firstChoice.message().content();
    }
}
