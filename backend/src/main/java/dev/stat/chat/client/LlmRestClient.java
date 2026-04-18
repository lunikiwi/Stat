package dev.stat.chat.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.List;
import java.util.Map;

/**
 * Quarkus REST Client for Google AI Studio (Gemini) API.
 * Sends chat prompts and receives AI-generated responses.
 * Supports Function Calling for structured outputs.
 */
@RegisterRestClient(configKey = "llm")
@Path("/v1beta/models")
public interface LlmRestClient {

    /**
     * Send a chat completion request to Gemini.
     *
     * @param model The model name (e.g., "gemini-2.5-flash")
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
     * Request to Gemini API with optional function declarations and system instruction.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    record ChatCompletionRequest(
            List<Content> contents,
            GenerationConfig generationConfig,
            List<Tool> tools,
            Content systemInstruction
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record Content(
            List<Part> parts
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record Part(
            String text,
            FunctionCall functionCall
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record GenerationConfig(
            double temperature,
            int maxOutputTokens
    ) {}

    /**
     * Tool definition for function calling.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    record Tool(
            List<FunctionDeclaration> functionDeclarations
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record FunctionDeclaration(
            String name,
            String description,
            Schema parameters
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record Schema(
            String type,
            Map<String, Property> properties,
            List<String> required
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record Property(
            String type,
            String description
    ) {}

    /**
     * Function call from Gemini (in response).
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    record FunctionCall(
            String name,
            Map<String, Object> args
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
