package dev.stat.chat;

import dev.stat.chat.dto.ChatRequest;
import dev.stat.chat.dto.ChatResponse;
import dev.stat.chat.service.ChatService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * REST resource for the chat endpoint.
 * Thin controller that delegates to ChatService.
 */
@Path("/api/chat")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ChatResource {

    @Inject
    ChatService chatService;

    @POST
    public ChatResponse chat(@Valid ChatRequest request) {
        return chatService.processChat(request);
    }
}
