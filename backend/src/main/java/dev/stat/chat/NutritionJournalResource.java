package dev.stat.chat;

import dev.stat.chat.domain.NutritionEntry;
import dev.stat.chat.dto.NutritionEntryDto;
import dev.stat.chat.service.HealthDataClient;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * REST endpoint for managing nutrition journal entries.
 * Allows viewing and deleting nutrition entries for a specific date.
 */
@Path("/api/nutrition/entries")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class NutritionJournalResource {

    private static final Logger LOG = Logger.getLogger(NutritionJournalResource.class);

    @Inject
    HealthDataClient healthDataClient;

    /**
     * GET /api/nutrition/entries
     * Returns all nutrition entries for today, grouped by timestamp.
     *
     * @return list of nutrition entries
     */
    @GET
    public List<NutritionEntryDto> getTodayEntries() {
        LOG.info("GET /api/nutrition/entries - fetching today's nutrition entries");

        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        List<NutritionEntry> entries = healthDataClient.getNutritionEntries(today);

        // Map domain objects to DTOs
        return entries.stream()
                .map(entry -> new NutritionEntryDto(
                        entry.timestamp(),
                        entry.calories(),
                        entry.proteinGrams(),
                        entry.carbsGrams(),
                        entry.fatGrams()
                ))
                .toList();
    }

    /**
     * DELETE /api/nutrition/entries/{timestamp}
     * Deletes a nutrition entry by its exact timestamp.
     *
     * @param timestampStr the ISO-8601 timestamp string
     * @return 204 No Content on success
     */
    @DELETE
    @Path("/{timestamp}")
    public Response deleteEntry(@PathParam("timestamp") String timestampStr) {
        LOG.infof("DELETE /api/nutrition/entries/%s - deleting nutrition entry", timestampStr);

        try {
            Instant timestamp = Instant.parse(timestampStr);
            healthDataClient.deleteNutritionEntry(timestamp);

            LOG.infof("Successfully deleted nutrition entry at %s", timestamp);
            return Response.noContent().build();

        } catch (Exception e) {
            LOG.errorf(e, "Failed to delete nutrition entry at %s", timestampStr);
            throw e;
        }
    }
}
