package dev.stat.chat.client;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.HeaderParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import java.util.List;

/**
 * Quarkus REST Client for LLM API (OpenAI/Gemini compatible).
 * Sends chat prompts and receives AI-generated responses.
 */
@RegisterRestClient(configKey = "llm")
@Path("/v1/chat")
public interface LlmRestClient {

    /**
     * Send a chat completion request to the LLM.
     *
     * @param authorization Bearer token for authentication
     * @param request Chat completion request
     * @return Chat completion response
     */
    @POST
    @Path("/completions")
    ChatCompletionResponse createChatCompletion(
            @HeaderParam("Authorization") String authorization,
            ChatCompletionRequest request
    );

    /**
     * Request to LLM API.
     */
    record ChatCompletionRequest(
            String model,
            List<Message> messages,
            double temperature,
            int maxTokens
    ) {}

    record Message(
            String role,
            String content
    ) {}

    /**
     * Response from LLM API.
     */
    record ChatCompletionResponse(
            String id,
            String object,
            long created,
            String model,
            List<Choice> choices
    ) {}

    record Choice(
            int index,
            Message message,
            String finishReason
    ) {}
}
