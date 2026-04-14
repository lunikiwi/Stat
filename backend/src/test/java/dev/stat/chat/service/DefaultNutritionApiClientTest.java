package dev.stat.chat.service;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import dev.stat.chat.domain.NutritionData;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DefaultNutritionApiClient using WireMock.
 * Tests the Spoonacular API integration with timeout and error handling.
 */
@QuarkusTest
@TestProfile(DefaultNutritionApiClientTest.SpoonacularTestProfile.class)
class DefaultNutritionApiClientTest {

    private static WireMockServer wireMockServer;

    @Inject
    NutritionApiClient nutritionApiClient;

    @BeforeAll
    static void startWireMock() {
        wireMockServer = new WireMockServer(WireMockConfiguration.options().port(9999));
        wireMockServer.start();
    }

    @AfterAll
    static void stopWireMock() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @BeforeEach
    void resetWireMock() {
        wireMockServer.resetAll();
    }

    @Test
    void shouldFetchTodayNutritionFromSpoonacular() {
        // Arrange: Mock Spoonacular API response
        wireMockServer.stubFor(get(urlPathEqualTo("/mealplanner/username/day/2026-04-14"))
                .withQueryParam("apiKey", equalTo("test-key"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "nutritionSummary": {
                                    "nutrients": [
                                      {"name": "Calories", "amount": 2100},
                                      {"name": "Protein", "amount": 150},
                                      {"name": "Carbohydrates", "amount": 220},
                                      {"name": "Fat", "amount": 70}
                                    ]
                                  }
                                }
                                """)));

        // Act
        NutritionData result = nutritionApiClient.fetchTodayNutrition();

        // Assert
        assertNotNull(result);
        assertEquals(2100, result.calories());
        assertEquals(150, result.proteinGrams());
        assertEquals(220, result.carbsGrams());
        assertEquals(70, result.fatGrams());

        // Verify the request was made
        wireMockServer.verify(getRequestedFor(urlPathEqualTo("/mealplanner/username/day/2026-04-14")));
    }

    @Test
    void shouldThrowExceptionWhenSpoonacularTimesOut() {
        // Arrange: Simulate timeout (delay > 5000ms)
        wireMockServer.stubFor(get(urlPathEqualTo("/mealplanner/username/day/2026-04-14"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withFixedDelay(6000))); // 6 seconds > 5 second timeout

        // Act & Assert
        ExternalServiceException exception = assertThrows(
                ExternalServiceException.class,
                () -> nutritionApiClient.fetchTodayNutrition()
        );

        assertTrue(exception.getMessage().contains("Spoonacular") ||
                   exception.getMessage().contains("timeout"));
    }

    @Test
    void shouldThrowExceptionWhenSpoonacularReturns429RateLimit() {
        // Arrange: Simulate rate limiting
        wireMockServer.stubFor(get(urlPathEqualTo("/mealplanner/username/day/2026-04-14"))
                .willReturn(aResponse()
                        .withStatus(429)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"message\": \"Rate limit exceeded\"}")));

        // Act & Assert
        ExternalServiceException exception = assertThrows(
                ExternalServiceException.class,
                () -> nutritionApiClient.fetchTodayNutrition()
        );

        assertTrue(exception.getMessage().contains("Spoonacular") ||
                   exception.getMessage().contains("429") ||
                   exception.getMessage().contains("rate limit"));
    }

    @Test
    void shouldThrowExceptionWhenSpoonacularReturns500() {
        // Arrange: Simulate server error
        wireMockServer.stubFor(get(urlPathEqualTo("/mealplanner/username/day/2026-04-14"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error")));

        // Act & Assert
        ExternalServiceException exception = assertThrows(
                ExternalServiceException.class,
                () -> nutritionApiClient.fetchTodayNutrition()
        );

        assertTrue(exception.getMessage().contains("Spoonacular"));
    }

    @Test
    void shouldThrowExceptionWhenSpoonacularReturnsInvalidJson() {
        // Arrange: Return invalid JSON
        wireMockServer.stubFor(get(urlPathEqualTo("/mealplanner/username/day/2026-04-14"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{ invalid json }")));

        // Act & Assert
        ExternalServiceException exception = assertThrows(
                ExternalServiceException.class,
                () -> nutritionApiClient.fetchTodayNutrition()
        );

        assertTrue(exception.getMessage().contains("Spoonacular"));
    }

    /**
     * Test profile to configure WireMock URL for Spoonacular.
     */
    public static class SpoonacularTestProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "stat.spoonacular.base-url", "http://localhost:9999",
                    "stat.spoonacular.api-key", "test-key",
                    "stat.external.timeout-ms", "5000"
            );
        }
    }
}
