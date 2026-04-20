package dev.stat.chat;

import dev.stat.chat.domain.HealthData;
import dev.stat.chat.domain.NutritionData;
import dev.stat.chat.domain.TrendData;
import dev.stat.chat.service.ExternalServiceException;
import dev.stat.chat.service.HealthDataClient;
import dev.stat.chat.service.LlmClient;
import dev.stat.chat.service.NutritionApiClient;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

/**
 * Integration tests for POST /api/chat.
 * System boundaries (InfluxDB, Spoonacular, LLM) are mocked.
 * Tests exercise the full HTTP path through the real Quarkus stack.
 */
@QuarkusTest
class ChatResourceTest {

    @InjectMock
    HealthDataClient healthDataClient;

    @InjectMock
    NutritionApiClient nutritionApiClient;

    @InjectMock
    LlmClient llmClient;

    @Test
    void happyPath_returnsReplyAndMetrics() {
        // Arrange: stub all external boundaries
        Mockito.when(healthDataClient.fetchCurrentHealthData())
                .thenReturn(new HealthData(45, 85, 120));

        Mockito.when(healthDataClient.fetch7DayAverages())
                .thenReturn(new TrendData(80, 50, 2000));

        Mockito.when(nutritionApiClient.fetchTodayNutrition())
                .thenReturn(new NutritionData(1800, 120, 200, 60));

        Mockito.when(llmClient.chat(Mockito.anyString(), Mockito.anyString(), Mockito.nullable(String.class)))
                .thenReturn("Iss jetzt 2 Bananen für schnelle Energie.");

        // Act & Assert
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "currentMessage": "Was soll ich jetzt essen?",
                          "chatHistory": [
                            {"role": "user", "content": "Hallo Coach"},
                            {"role": "assistant", "content": "Hallo! Wie kann ich helfen?"}
                          ]
                        }
                        """)
        .when()
                .post("/api/chat")
        .then()
                .statusCode(200)
                .body("reply", is("Iss jetzt 2 Bananen für schnelle Energie."))
                .body("metricsUsed.bodyBattery", equalTo(45))
                .body("metricsUsed.sleepScore", equalTo(85))
                .body("metricsUsed.trainingLoad48h", equalTo("120 mins"));
    }

    @Test
    void influxDbFailure_returns503WithErrorDetails() {
        // Arrange: InfluxDB throws ExternalServiceException
        Mockito.when(healthDataClient.fetchCurrentHealthData())
                .thenThrow(new ExternalServiceException("InfluxDB", "InfluxDB connection refused"));

        // Act & Assert
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "currentMessage": "Wie ist meine Form?"
                        }
                        """)
        .when()
                .post("/api/chat")
        .then()
                .statusCode(503)
                .body("error", is("EXTERNAL_API_FAILURE"))
                .body("message", is("InfluxDB connection refused"));
    }

    @Test
    void spoonacularFailure_returns503WithErrorDetails() {
        // Arrange: InfluxDB succeeds, Spoonacular fails
        Mockito.when(healthDataClient.fetchCurrentHealthData())
                .thenReturn(new HealthData(60, 90, 90));

        Mockito.when(healthDataClient.fetch7DayAverages())
                .thenReturn(new TrendData(85, 55, 2100));

        Mockito.when(nutritionApiClient.fetchTodayNutrition())
                .thenThrow(new ExternalServiceException(
                        "Spoonacular", "Spoonacular API did not respond within 5000ms."));

        // Act & Assert
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "currentMessage": "Was habe ich heute gegessen?"
                        }
                        """)
        .when()
                .post("/api/chat")
        .then()
                .statusCode(503)
                .body("error", is("EXTERNAL_API_FAILURE"))
                .body("message", is("Spoonacular API did not respond within 5000ms."));
    }

    @Test
    void llmFailure_returns503WithErrorDetails() {
        // Arrange: InfluxDB and Spoonacular succeed, LLM fails
        Mockito.when(healthDataClient.fetchCurrentHealthData())
                .thenReturn(new HealthData(70, 92, 60));

        Mockito.when(healthDataClient.fetch7DayAverages())
                .thenReturn(new TrendData(88, 60, 2200));

        Mockito.when(nutritionApiClient.fetchTodayNutrition())
                .thenReturn(new NutritionData(2200, 150, 250, 70));

        Mockito.when(llmClient.chat(Mockito.anyString(), Mockito.anyString(), Mockito.nullable(String.class)))
                .thenThrow(new ExternalServiceException("LLM", "OpenAI API rate limit exceeded"));

        // Act & Assert
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "currentMessage": "Gib mir einen Trainingsplan"
                        }
                        """)
        .when()
                .post("/api/chat")
        .then()
                .statusCode(503)
                .body("error", is("EXTERNAL_API_FAILURE"))
                .body("message", is("OpenAI API rate limit exceeded"));
    }

    @Test
    void missingCurrentMessage_returns400() {
        // Act & Assert: send request without currentMessage
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "chatHistory": []
                        }
                        """)
        .when()
                .post("/api/chat")
        .then()
                .statusCode(400);
    }

    @Test
    void functionCall_logsNutritionAndReturnsConfirmation() {
        // Arrange: stub all external boundaries
        Mockito.when(healthDataClient.fetchCurrentHealthData())
                .thenReturn(new HealthData(45, 85, 120));

        Mockito.when(healthDataClient.fetch7DayAverages())
                .thenReturn(new TrendData(80, 50, 2000));

        Mockito.when(nutritionApiClient.fetchTodayNutrition())
                .thenReturn(new NutritionData(1800, 120, 200, 60));

        // LLM returns a function call instead of text
        Mockito.when(llmClient.chat(Mockito.anyString(), Mockito.anyString(), Mockito.nullable(String.class)))
                .thenReturn("{\"functionCall\":{\"name\":\"log_nutrition\",\"args\":{\"calories\":500,\"protein\":30,\"carbs\":60,\"fat\":20}}}");

        // Act & Assert
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "currentMessage": "Ich habe gerade 2 Bananen gegessen"
                        }
                        """)
        .when()
                .post("/api/chat")
        .then()
                .statusCode(200)
                .body("reply", is("Mahlzeit erfasst! 500 kcal (30g Protein, 60g Carbs, 20g Fett)"));

        // Verify that logNutrition was called
        Mockito.verify(healthDataClient).logNutrition(500, 30, 60, 20);
    }

    @Test
    void imageProvided_sendsImageToLlmAndAnalyzesMeal() {
        // Arrange: stub all external boundaries
        Mockito.when(healthDataClient.fetchCurrentHealthData())
                .thenReturn(new HealthData(50, 80, 100));

        Mockito.when(healthDataClient.fetch7DayAverages())
                .thenReturn(new TrendData(75, 48, 1900));

        Mockito.when(nutritionApiClient.fetchTodayNutrition())
                .thenReturn(new NutritionData(1500, 100, 180, 50));

        // LLM analyzes image and returns function call to log nutrition
        // Mock the 3-parameter chat method (with image)
        Mockito.when(llmClient.chat(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenReturn("{\"functionCall\":{\"name\":\"log_nutrition\",\"args\":{\"calories\":650,\"protein\":35,\"carbs\":80,\"fat\":25}}}");

        // Act & Assert
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "currentMessage": "Was ist das für eine Mahlzeit?",
                          "chatHistory": [],
                          "imageBase64": "/9j/4AAQSkZJRgABAQEAYABgAAD/2wBDAAgGBgcGBQgHBwcJCQgKDBQNDAsLDBkSEw8UHRofHh0aHBwgJC4nICIsIxwcKDcpLDAxNDQ0Hyc5PTgyPC4zNDL/2wBDAQkJCQwLDBgNDRgyIRwhMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjL/wAARCAABAAEDASIAAhEBAxEB/8QAFQABAQAAAAAAAAAAAAAAAAAAAAv/xAAUEAEAAAAAAAAAAAAAAAAAAAAA/8QAFQEBAQAAAAAAAAAAAAAAAAAAAAX/xAAUEQEAAAAAAAAAAAAAAAAAAAAA/9oADAMBAAIRAxEAPwCwAA8A/9k="
                        }
                        """)
        .when()
                .post("/api/chat")
        .then()
                .statusCode(200)
                .body("reply", is("Mahlzeit erfasst! 650 kcal (35g Protein, 80g Carbs, 25g Fett)"));

        // Verify that logNutrition was called with analyzed values
        Mockito.verify(healthDataClient).logNutrition(650, 35, 80, 25);
    }
}
