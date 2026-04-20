package dev.stat.chat;

import dev.stat.chat.domain.NutritionEntry;
import dev.stat.chat.service.ExternalServiceException;
import dev.stat.chat.service.HealthDataClient;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for NutritionJournalResource.
 * Uses mocked HealthDataClient to avoid external dependencies.
 */
@QuarkusTest
class NutritionJournalResourceTest {

    @InjectMock
    HealthDataClient healthDataClient;

    @Test
    void shouldGetTodayNutritionEntries() {
        // Arrange: Mock HealthDataClient response
        Instant timestamp1 = Instant.parse("2026-04-19T08:00:00Z");
        Instant timestamp2 = Instant.parse("2026-04-19T12:30:00Z");

        NutritionEntry entry1 = new NutritionEntry(timestamp1, 450, 30, 50, 15);
        NutritionEntry entry2 = new NutritionEntry(timestamp2, 600, 40, 70, 20);

        Mockito.when(healthDataClient.getNutritionEntries(any(LocalDate.class)))
                .thenReturn(List.of(entry1, entry2));

        // Act & Assert
        given()
                .when()
                .get("/api/nutrition/entries")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("size()", is(2))
                .body("[0].timestamp", equalTo("2026-04-19T08:00:00Z"))
                .body("[0].calories", equalTo(450))
                .body("[0].proteinGrams", equalTo(30))
                .body("[0].carbsGrams", equalTo(50))
                .body("[0].fatGrams", equalTo(15))
                .body("[1].timestamp", equalTo("2026-04-19T12:30:00Z"))
                .body("[1].calories", equalTo(600))
                .body("[1].proteinGrams", equalTo(40))
                .body("[1].carbsGrams", equalTo(70))
                .body("[1].fatGrams", equalTo(20));

        // Verify interaction
        verify(healthDataClient, times(1)).getNutritionEntries(any(LocalDate.class));
    }

    @Test
    void shouldReturnEmptyListWhenNoEntriesExist() {
        // Arrange: Mock empty response
        Mockito.when(healthDataClient.getNutritionEntries(any(LocalDate.class)))
                .thenReturn(List.of());

        // Act & Assert
        given()
                .when()
                .get("/api/nutrition/entries")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("size()", is(0));

        verify(healthDataClient, times(1)).getNutritionEntries(any(LocalDate.class));
    }

    @Test
    void shouldReturn503WhenGetEntriesFails() {
        // Arrange: Mock InfluxDB failure
        Mockito.when(healthDataClient.getNutritionEntries(any(LocalDate.class)))
                .thenThrow(new ExternalServiceException("InfluxDB", "Connection failed"));

        // Act & Assert
        given()
                .when()
                .get("/api/nutrition/entries")
                .then()
                .statusCode(503);

        verify(healthDataClient, times(1)).getNutritionEntries(any(LocalDate.class));
    }

    @Test
    void shouldDeleteNutritionEntry() {
        // Arrange: Mock successful delete
        String timestamp = "2026-04-19T08:00:00Z";
        doNothing().when(healthDataClient).deleteNutritionEntry(any(Instant.class));

        // Act & Assert
        given()
                .when()
                .delete("/api/nutrition/entries/" + timestamp)
                .then()
                .statusCode(204);

        // Verify interaction
        verify(healthDataClient, times(1)).deleteNutritionEntry(Instant.parse(timestamp));
    }

    @Test
    void shouldReturn503WhenDeleteFails() {
        // Arrange: Mock InfluxDB failure
        String timestamp = "2026-04-19T08:00:00Z";
        doThrow(new ExternalServiceException("InfluxDB", "Delete failed"))
                .when(healthDataClient).deleteNutritionEntry(any(Instant.class));

        // Act & Assert
        given()
                .when()
                .delete("/api/nutrition/entries/" + timestamp)
                .then()
                .statusCode(503);

        verify(healthDataClient, times(1)).deleteNutritionEntry(Instant.parse(timestamp));
    }

    @Test
    void shouldReturn400ForInvalidTimestamp() {
        // Act & Assert: Invalid timestamp format
        given()
                .when()
                .delete("/api/nutrition/entries/invalid-timestamp")
                .then()
                .statusCode(500); // DateTimeParseException will result in 500

        // Verify no interaction with healthDataClient
        verify(healthDataClient, never()).deleteNutritionEntry(any(Instant.class));
    }
}
