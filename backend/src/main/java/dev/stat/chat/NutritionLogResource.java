package dev.stat.chat;

import dev.stat.chat.dto.NutritionLogRequest;
import dev.stat.chat.dto.NutritionLogResponse;
import dev.stat.chat.service.ChatService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.jboss.logging.Logger;

/**
 * REST resource for the nutrition logging endpoint.
 * Provides a quick-add feature for logging meals via natural language.
 *
 * This endpoint uses the ChatService with a "silent prompt" to extract
 * macros from the meal description and log them to InfluxDB.
 */
@Path("/api/nutrition/log")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class NutritionLogResource {

    private static final Logger LOG = Logger.getLogger(NutritionLogResource.class);

    @Inject
    ChatService chatService;

    /**
     * Logs a meal from a natural language description.
     *
     * Example request:
     * {
     *   "description": "200g Hähnchenbrust mit Reis und Gemüse"
     * }
     *
     * @param request The nutrition log request
     * @return Confirmation message with logged values
     */
    @POST
    public NutritionLogResponse logNutrition(@Valid NutritionLogRequest request) {
        LOG.infof("Received nutrition log request: %s", request.description());

        String message = chatService.processSilentNutritionLog(request);

        return new NutritionLogResponse(message);
    }
}
