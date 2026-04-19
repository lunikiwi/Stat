package dev.stat.chat.service;

import dev.stat.chat.dto.NutritionLogRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ChatService's silent nutrition logging functionality.
 * Tests the processSilentNutritionLog method in isolation.
 */
@ExtendWith(MockitoExtension.class)
class ChatServiceSilentNutritionTest {

    @Mock
    private HealthDataClient healthDataClient;

    @Mock
    private NutritionApiClient nutritionApiClient;

    @Mock
    private LlmClient llmClient;

    @Mock
    private dev.stat.chat.config.UserProfileConfig userProfileConfig;

    private ChatService chatService;

    @BeforeEach
    void setUp() {
        chatService = new ChatService(
                healthDataClient,
                nutritionApiClient,
                llmClient,
                userProfileConfig
        );
    }

    @Test
    void testProcessSilentNutritionLog_WithFunctionCall() {
        // Given
        String mealDescription = "200g Hähnchenbrust mit Reis";
        NutritionLogRequest request = new NutritionLogRequest(mealDescription);

        // Mock LLM response with function call
        String llmResponse = """
                {
                  "functionCall": {
                    "name": "log_nutrition",
                    "args": {
                      "calories": 450,
                      "protein": 35,
                      "carbs": 40,
                      "fat": 15
                    }
                  }
                }
                """;

        when(llmClient.chat(anyString(), anyString())).thenReturn(llmResponse);

        // When
        String result = chatService.processSilentNutritionLog(request);

        // Then
        assertNotNull(result);
        assertTrue(result.contains("Mahlzeit erfasst!"));
        assertTrue(result.contains("450 kcal"));
        assertTrue(result.contains("35g Protein"));
        assertTrue(result.contains("40g Carbs"));
        assertTrue(result.contains("15g Fett"));

        // Verify InfluxDB write was called
        verify(healthDataClient).logNutrition(450, 35, 40, 15);

        // Verify LLM was called with correct prompt
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> systemCaptor = ArgumentCaptor.forClass(String.class);
        verify(llmClient).chat(promptCaptor.capture(), systemCaptor.capture());

        String capturedPrompt = promptCaptor.getValue();
        assertTrue(capturedPrompt.contains("Extrahiere die Makros"));
        assertTrue(capturedPrompt.contains(mealDescription));
        assertTrue(capturedPrompt.contains("log_nutrition Tool"));

        String capturedSystem = systemCaptor.getValue();
        assertTrue(capturedSystem.contains("Nutrition Extraction System"));
        assertTrue(capturedSystem.contains("log_nutrition function"));
    }

    @Test
    void testProcessSilentNutritionLog_WithTextResponse() {
        // Given
        String mealDescription = "1 Apfel";
        NutritionLogRequest request = new NutritionLogRequest(mealDescription);

        // Mock LLM response with plain text (no function call)
        String llmResponse = "Mahlzeit erfasst! 95 kcal (0g Protein, 25g Carbs, 0g Fett)";

        when(llmClient.chat(anyString(), anyString())).thenReturn(llmResponse);

        // When
        String result = chatService.processSilentNutritionLog(request);

        // Then
        assertEquals(llmResponse, result);

        // Verify no InfluxDB write was called (since no function call)
        verify(healthDataClient, never()).logNutrition(anyInt(), anyInt(), anyInt(), anyInt());
    }

    @Test
    void testProcessSilentNutritionLog_InfluxDBFailure() {
        // Given
        String mealDescription = "Pizza";
        NutritionLogRequest request = new NutritionLogRequest(mealDescription);

        String llmResponse = """
                {
                  "functionCall": {
                    "name": "log_nutrition",
                    "args": {
                      "calories": 800,
                      "protein": 30,
                      "carbs": 100,
                      "fat": 30
                    }
                  }
                }
                """;

        when(llmClient.chat(anyString(), anyString())).thenReturn(llmResponse);
        doThrow(new RuntimeException("InfluxDB connection failed"))
                .when(healthDataClient).logNutrition(anyInt(), anyInt(), anyInt(), anyInt());

        // When/Then - The exception is thrown from executeLogNutrition and propagates up
        ExternalServiceException exception = assertThrows(
                ExternalServiceException.class,
                () -> chatService.processSilentNutritionLog(request)
        );
        assertTrue(exception.getMessage().contains("Failed to log nutrition data"));
        assertTrue(exception.getCause().getMessage().contains("InfluxDB connection failed"));
    }

    @Test
    void testProcessSilentNutritionLog_LlmFailure() {
        // Given
        String mealDescription = "Salat";
        NutritionLogRequest request = new NutritionLogRequest(mealDescription);

        when(llmClient.chat(anyString(), anyString()))
                .thenThrow(new ExternalServiceException("Gemini", "API timeout", null));

        // When/Then
        ExternalServiceException exception = assertThrows(
                ExternalServiceException.class,
                () -> chatService.processSilentNutritionLog(request)
        );
        assertTrue(exception.getMessage().contains("API timeout"));
    }

    @Test
    void testProcessSilentNutritionLog_ComplexMeal() {
        // Given
        String mealDescription = "2 Scheiben Vollkornbrot mit Erdnussbutter, 1 Banane und 300ml Milch";
        NutritionLogRequest request = new NutritionLogRequest(mealDescription);

        String llmResponse = """
                {
                  "functionCall": {
                    "name": "log_nutrition",
                    "args": {
                      "calories": 650,
                      "protein": 25,
                      "carbs": 85,
                      "fat": 20
                    }
                  }
                }
                """;

        when(llmClient.chat(anyString(), anyString())).thenReturn(llmResponse);

        // When
        String result = chatService.processSilentNutritionLog(request);

        // Then
        assertTrue(result.contains("650 kcal"));
        verify(healthDataClient).logNutrition(650, 25, 85, 20);
    }

    @Test
    void testProcessSilentNutritionLog_InvalidFunctionCallJson() {
        // Given
        String mealDescription = "Burger";
        NutritionLogRequest request = new NutritionLogRequest(mealDescription);

        // Mock LLM response with invalid JSON that starts with { and contains functionCall
        // This will trigger the JSON parsing attempt
        String llmResponse = "{ \"functionCall\": invalid json }";

        when(llmClient.chat(anyString(), anyString())).thenReturn(llmResponse);

        // When
        String result = chatService.processSilentNutritionLog(request);

        // Then - should return error message when JSON parsing fails
        assertTrue(result.contains("Fehler beim Verarbeiten der Antwort"));
    }
}
