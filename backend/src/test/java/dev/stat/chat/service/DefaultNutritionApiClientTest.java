package dev.stat.chat.service;

import dev.stat.chat.domain.NutritionData;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.InjectMock;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DefaultNutritionApiClient.
 * Tests the delegation to DefaultHealthDataClient for InfluxDB-based nutrition data.
 */
@QuarkusTest
class DefaultNutritionApiClientTest {

    @Inject
    NutritionApiClient nutritionApiClient;

    @InjectMock
    DefaultHealthDataClient healthDataClient;

    @Test
    void shouldFetchTodayNutritionFromInfluxDB() {
        // Arrange: Mock InfluxDB response via DefaultHealthDataClient
        NutritionData mockData = new NutritionData(2100, 150, 220, 70);
        when(healthDataClient.fetchTodayNutrition()).thenReturn(mockData);

        // Act
        NutritionData result = nutritionApiClient.fetchTodayNutrition();

        // Assert
        assertNotNull(result);
        assertEquals(2100, result.calories());
        assertEquals(150, result.proteinGrams());
        assertEquals(220, result.carbsGrams());
        assertEquals(70, result.fatGrams());

        // Verify delegation
        verify(healthDataClient, times(1)).fetchTodayNutrition();
    }

    @Test
    void shouldPropagateExceptionFromHealthDataClient() {
        // Arrange: Simulate InfluxDB failure
        when(healthDataClient.fetchTodayNutrition())
                .thenThrow(new ExternalServiceException("InfluxDB", "InfluxDB query failed"));

        // Act & Assert
        ExternalServiceException exception = assertThrows(
                ExternalServiceException.class,
                () -> nutritionApiClient.fetchTodayNutrition()
        );

        assertTrue(exception.getMessage().contains("InfluxDB"));
        verify(healthDataClient, times(1)).fetchTodayNutrition();
    }
}
