package dev.stat.chat.service;

import dev.stat.chat.client.SpoonacularRestClient;
import dev.stat.chat.domain.NutritionData;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Default Spoonacular-backed implementation of NutritionApiClient.
 * Fetches today's nutrition data with timeout and error handling.
 */
@ApplicationScoped
public class DefaultNutritionApiClient implements NutritionApiClient {

    private static final Logger LOG = Logger.getLogger(DefaultNutritionApiClient.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @RestClient
    SpoonacularRestClient spoonacularClient;

    @ConfigProperty(name = "stat.spoonacular.api-key")
    String apiKey;

    @ConfigProperty(name = "stat.spoonacular.username", defaultValue = "username")
    String username;

    @Override
    public NutritionData fetchTodayNutrition() {
        try {
            String today = LocalDate.now().format(DATE_FORMATTER);

            LOG.debugf("Fetching nutrition data from Spoonacular for date: %s", today);

            SpoonacularRestClient.SpoonacularDayResponse response =
                    spoonacularClient.getDayNutrition(username, today, apiKey);

            return mapToNutritionData(response);

        } catch (WebApplicationException e) {
            // HTTP errors (4xx, 5xx)
            int status = e.getResponse().getStatus();
            LOG.errorf(e, "Spoonacular API returned HTTP %d", status);

            if (status == 429) {
                throw new ExternalServiceException(
                        "Spoonacular API rate limit exceeded (HTTP 429)", e);
            } else if (status >= 500) {
                throw new ExternalServiceException(
                        "Spoonacular API server error (HTTP " + status + ")", e);
            } else {
                throw new ExternalServiceException(
                        "Spoonacular API request failed (HTTP " + status + ")", e);
            }

        } catch (ProcessingException e) {
            // Timeout or connection errors
            LOG.errorf(e, "Failed to connect to Spoonacular API");

            if (e.getCause() instanceof java.net.SocketTimeoutException ||
                e.getMessage().contains("timeout")) {
                throw new ExternalServiceException(
                        "Spoonacular API did not respond within timeout", e);
            } else {
                throw new ExternalServiceException(
                        "Spoonacular API connection failed: " + e.getMessage(), e);
            }

        } catch (Exception e) {
            LOG.errorf(e, "Unexpected error fetching nutrition data");
            throw new ExternalServiceException(
                    "Spoonacular API request failed: " + e.getMessage(), e);
        }
    }

    /**
     * Maps Spoonacular API response to NutritionData domain model.
     */
    private NutritionData mapToNutritionData(SpoonacularRestClient.SpoonacularDayResponse response) {
        if (response == null || response.nutritionSummary() == null) {
            throw new ExternalServiceException("Spoonacular API returned empty response");
        }

        Map<String, Double> nutrients = response.nutritionSummary().nutrients().stream()
                .collect(Collectors.toMap(
                        SpoonacularRestClient.Nutrient::name,
                        SpoonacularRestClient.Nutrient::amount
                ));

        int calories = nutrients.getOrDefault("Calories", 0.0).intValue();
        int protein = nutrients.getOrDefault("Protein", 0.0).intValue();
        int carbs = nutrients.getOrDefault("Carbohydrates", 0.0).intValue();
        int fat = nutrients.getOrDefault("Fat", 0.0).intValue();

        return new NutritionData(calories, protein, carbs, fat);
    }
}
