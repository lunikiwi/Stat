package dev.stat.chat.client;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.List;

/**
 * Quarkus REST Client for Google AI Studio (Gemini) API.
 * Sends chat prompts and receives AI-generated responses.
 */
@RegisterRestClient(configKey = "llm")
@Path("/v1beta/models")
public interface LlmRestClient {

    /**
     * Send a chat completion request to Gemini.
     *
     * @param model The model name (e.g., "gemini-1.5-flash")
     * @param apiKey API key for authentication (passed as query parameter)
     * @param request Chat completion request
     * @return Chat completion response
     */
    @POST
    @Path("/{model}:generateContent")
    ChatCompletionResponse generateContent(
            @jakarta.ws.rs.PathParam("model") String model,
            @QueryParam("key") String apiKey,
            ChatCompletionRequest request
    );

    /**
     * Request to Gemini API.
     */
    record ChatCompletionRequest(
            List<Content> contents,
            GenerationConfig generationConfig
    ) {}

    record Content(
            List<Part> parts
    ) {}

    record Part(
            String text
    ) {}

    record GenerationConfig(
            double temperature,
            int maxOutputTokens
    ) {}

    /**
     * Response from Gemini API.
     */
    record ChatCompletionResponse(
            List<Candidate> candidates
    ) {}

    record Candidate(
            Content content,
            String finishReason
    ) {}
}
