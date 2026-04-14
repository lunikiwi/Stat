package dev.stat.chat.client;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * Quarkus REST Client for Spoonacular API.
 * Fetches daily nutrition data (macros and calories).
 */
@RegisterRestClient(configKey = "spoonacular")
@Path("/mealplanner")
public interface SpoonacularRestClient {

    /**
     * Get nutrition summary for a specific day.
     *
     * @param username Spoonacular username (configured)
     * @param date Date in format YYYY-MM-DD
     * @param apiKey Spoonacular API key
     * @return Daily nutrition summary
     */
    @GET
    @Path("/{username}/day/{date}")
    SpoonacularDayResponse getDayNutrition(
            @PathParam("username") String username,
            @PathParam("date") String date,
            @QueryParam("apiKey") String apiKey
    );

    /**
     * Response from Spoonacular API for daily nutrition.
     */
    record SpoonacularDayResponse(
            NutritionSummary nutritionSummary
    ) {}

    record NutritionSummary(
            java.util.List<Nutrient> nutrients
    ) {}

    record Nutrient(
            String name,
            double amount
    ) {}
}
