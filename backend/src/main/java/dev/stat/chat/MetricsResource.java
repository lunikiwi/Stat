package dev.stat.chat;

import dev.stat.chat.domain.HealthData;
import dev.stat.chat.domain.NutritionData;
import dev.stat.chat.dto.HealthMetrics;
import dev.stat.chat.dto.MetricsResponse;
import dev.stat.chat.dto.NutritionMetrics;
import dev.stat.chat.service.HealthDataClient;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.jboss.logging.Logger;

/**
 * REST endpoint for GET /api/metrics.
 * Provides aggregated health and nutrition data for the dashboard view.
 */
@Path("/api/metrics")
public class MetricsResource {

    private static final Logger LOG = Logger.getLogger(MetricsResource.class);

    @Inject
    HealthDataClient healthDataClient;

    /**
     * Returns the latest health and nutrition metrics.
     *
     * @return MetricsResponse containing health and nutrition data
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public MetricsResponse getMetrics() {
        LOG.info("GET /api/metrics - fetching current metrics");

        // Fetch health data from InfluxDB
        HealthData healthData = healthDataClient.fetchCurrentHealthData();

        // Fetch nutrition data from InfluxDB
        NutritionData nutritionData = healthDataClient.fetchTodayNutrition();

        // Map domain objects to DTOs
        HealthMetrics health = new HealthMetrics(
                healthData.sleepScore(),
                healthData.bodyBattery(),
                healthData.trainingLoadMinutes48h()
        );

        NutritionMetrics nutrition = new NutritionMetrics(
                nutritionData.calories(),
                nutritionData.proteinGrams(),
                nutritionData.carbsGrams(),
                nutritionData.fatGrams()
        );

        return new MetricsResponse(health, nutrition);
    }
}
