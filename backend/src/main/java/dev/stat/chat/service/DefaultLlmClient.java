package dev.stat.chat.service;

import dev.stat.chat.client.LlmRestClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Map;

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
        return chat(prompt, null);
    }

    @Override
    public String chat(String prompt, String systemContext) {
        try {
            LOG.debugf("Sending prompt to Gemini (model: %s)", model);

            // Build system instruction if provided
            LlmRestClient.Content systemInstruction = null;
            if (systemContext != null && !systemContext.isBlank()) {
                systemInstruction = new LlmRestClient.Content(
                        List.of(new LlmRestClient.Part(systemContext, null))
                );
            }

            // Build Gemini API request with function declarations and optional system instruction
            LlmRestClient.ChatCompletionRequest request = new LlmRestClient.ChatCompletionRequest(
                    List.of(new LlmRestClient.Content(
                            List.of(new LlmRestClient.Part(prompt, null))
                    )),
                    new LlmRestClient.GenerationConfig(temperature, maxTokens),
                    buildFunctionDeclarations(),
                    systemInstruction
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
     * Builds function declarations for Gemini Function Calling.
     * Defines the log_nutrition tool for logging nutrition data.
     */
    private List<LlmRestClient.Tool> buildFunctionDeclarations() {
        LlmRestClient.FunctionDeclaration logNutrition = new LlmRestClient.FunctionDeclaration(
                "log_nutrition",
                "Logs nutrition data (calories, protein, carbs, fat) to the database. Use this when the user mentions eating food.",
                new LlmRestClient.Schema(
                        "object",
                        Map.of(
                                "calories", new LlmRestClient.Property("integer", "Total calories consumed"),
                                "protein", new LlmRestClient.Property("integer", "Protein in grams"),
                                "carbs", new LlmRestClient.Property("integer", "Carbohydrates in grams"),
                                "fat", new LlmRestClient.Property("integer", "Fat in grams")
                        ),
                        List.of("calories", "protein", "carbs", "fat")
                )
        );

        return List.of(new LlmRestClient.Tool(List.of(logNutrition)));
    }

    /**
     * Extracts the assistant's message content from the Gemini response.
     * Returns the text content or a JSON representation of function calls.
     * Logs a warning if the response was truncated due to max tokens.
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

        // Check if response was truncated due to max tokens
        if ("MAX_TOKENS".equals(firstCandidate.finishReason())) {
            LOG.warnf("Gemini response was truncated due to max_tokens limit (%d). " +
                    "Consider increasing stat.llm.max-tokens in application.properties.", maxTokens);
        }

        LlmRestClient.Part firstPart = firstCandidate.content().parts().get(0);

        // Check if response contains a function call
        if (firstPart.functionCall() != null) {
            // Return function call as JSON string for ChatService to parse
            return String.format("{\"functionCall\":{\"name\":\"%s\",\"args\":%s}}",
                    firstPart.functionCall().name(),
                    toJsonString(firstPart.functionCall().args()));
        }

        if (firstPart.text() == null) {
            throw new ExternalServiceException("LLM", "Gemini API response missing text");
        }

        return firstPart.text();
    }

    /**
     * Simple JSON serialization for function call arguments.
     */
    private String toJsonString(Map<String, Object> args) {
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : args.entrySet()) {
            if (!first) json.append(",");
            json.append("\"").append(entry.getKey()).append("\":");
            Object value = entry.getValue();
            if (value instanceof String) {
                json.append("\"").append(value).append("\"");
            } else {
                json.append(value);
            }
            first = false;
        }
        json.append("}");
        return json.toString();
    }
}
