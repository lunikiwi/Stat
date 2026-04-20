package dev.stat.chat;

import dev.stat.chat.domain.BodyMetrics;
import dev.stat.chat.domain.HealthData;
import dev.stat.chat.domain.NutritionData;
import dev.stat.chat.service.ExternalServiceException;
import dev.stat.chat.service.HealthDataClient;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

/**
 * Integration tests for GET /api/metrics.
 * System boundaries (InfluxDB) are mocked.
 * Tests verify that the endpoint returns the correct JSON structure as per spec.
 */
@QuarkusTest
class MetricsResourceTest {

    @InjectMock
    HealthDataClient healthDataClient;

    @Test
    void happyPath_returnsHealthAndNutritionMetrics() {
        // Arrange: stub InfluxDB responses
        Mockito.when(healthDataClient.fetchCurrentHealthData())
                .thenReturn(new HealthData(45, 85, 120));

        Mockito.when(healthDataClient.fetchTodayNutrition())
                .thenReturn(new NutritionData(1850, 140, 180, 60));

        Mockito.when(healthDataClient.fetchBodyMetrics())
                .thenReturn(new BodyMetrics(75.5, 2000));

        // Act & Assert: verify JSON structure matches spec exactly
        given()
        .when()
                .get("/api/metrics")
        .then()
                .statusCode(200)
                .body("health.sleepScore", equalTo(85))
                .body("health.bodyBattery", equalTo(45))
                .body("health.trainingLoadMinutes48h", equalTo(120))
                .body("health.currentWeightKg", equalTo(75.5f))
                .body("health.todayWaterMl", equalTo(2000))
                .body("nutrition.calories", equalTo(1850))
                .body("nutrition.protein", equalTo(140))
                .body("nutrition.carbs", equalTo(180))
                .body("nutrition.fat", equalTo(60));
    }

    @Test
    void influxDbHealthFailure_returns503() {
        // Arrange: InfluxDB health query fails
        Mockito.when(healthDataClient.fetchCurrentHealthData())
                .thenThrow(new ExternalServiceException("InfluxDB", "Connection timeout"));

        // Act & Assert
        given()
        .when()
                .get("/api/metrics")
        .then()
                .statusCode(503);
    }

    @Test
    void influxDbNutritionFailure_returns503() {
        // Arrange: InfluxDB health succeeds, nutrition fails
        Mockito.when(healthDataClient.fetchCurrentHealthData())
                .thenReturn(new HealthData(60, 90, 90));

        Mockito.when(healthDataClient.fetchTodayNutrition())
                .thenThrow(new ExternalServiceException("InfluxDB", "Query failed"));

        // Act & Assert
        given()
        .when()
                .get("/api/metrics")
        .then()
                .statusCode(503);
    }

    @Test
    void influxDbBodyMetricsFailure_returns503() {
        // Arrange: InfluxDB health and nutrition succeed, body metrics fail
        Mockito.when(healthDataClient.fetchCurrentHealthData())
                .thenReturn(new HealthData(60, 90, 90));

        Mockito.when(healthDataClient.fetchTodayNutrition())
                .thenReturn(new NutritionData(1850, 140, 180, 60));

        Mockito.when(healthDataClient.fetchBodyMetrics())
                .thenThrow(new ExternalServiceException("InfluxDB", "Query failed"));

        // Act & Assert
        given()
        .when()
                .get("/api/metrics")
        .then()
                .statusCode(503);
    }
}
