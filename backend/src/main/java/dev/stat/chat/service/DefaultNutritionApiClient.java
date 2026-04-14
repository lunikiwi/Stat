package dev.stat.chat.service;

import dev.stat.chat.domain.NutritionData;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Default Spoonacular-backed implementation of NutritionApiClient.
 * Stub for now - will be implemented when Spoonacular integration is built.
 */
@ApplicationScoped
public class DefaultNutritionApiClient implements NutritionApiClient {

    @Override
    public NutritionData fetchTodayNutrition() {
        // TODO: implement Spoonacular REST client
        throw new UnsupportedOperationException("Spoonacular client not yet implemented");
    }
}
