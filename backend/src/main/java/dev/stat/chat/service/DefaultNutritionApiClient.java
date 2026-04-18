package dev.stat.chat.service;

import dev.stat.chat.domain.NutritionData;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

/**
 * Default InfluxDB-backed implementation of NutritionApiClient.
 * Delegates to DefaultHealthDataClient to fetch today's nutrition data from InfluxDB.
 */
@ApplicationScoped
public class DefaultNutritionApiClient implements NutritionApiClient {

    private static final Logger LOG = Logger.getLogger(DefaultNutritionApiClient.class);

    @Inject
    DefaultHealthDataClient healthDataClient;

    @Override
    public NutritionData fetchTodayNutrition() {
        LOG.debug("Fetching today's nutrition data from InfluxDB");
        return healthDataClient.fetchTodayNutrition();
    }
}
