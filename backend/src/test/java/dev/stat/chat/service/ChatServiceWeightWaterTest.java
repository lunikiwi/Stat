package dev.stat.chat.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.stat.chat.config.UserProfileConfig;
import dev.stat.chat.domain.HealthData;
import dev.stat.chat.domain.TrendData;
import dev.stat.chat.dto.ChatRequest;
import dev.stat.chat.dto.ChatResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ChatService weight and water function call handling.
 * Tests that log_weight and log_water function calls are properly executed.
 */
@ExtendWith(MockitoExtension.class)
class ChatServiceWeightWaterTest {

    @Mock
    private HealthDataClient healthDataClient;

    @Mock
    private NutritionApiClient nutritionApiClient;

    @Mock
    private LlmClient llmClient;

    @Mock
    private UserProfileConfig userProfileConfig;

    private ChatService chatService;

    @BeforeEach
    void setUp() {
        chatService = new ChatService(healthDataClient, nutritionApiClient, llmClient, userProfileConfig);

        // Setup default mocks
        when(userProfileConfig.name()).thenReturn("Max");
        when(userProfileConfig.goal()).thenReturn("Fettabbau");
        when(userProfileConfig.diet()).thenReturn("Allesesser");
        when(userProfileConfig.metrics()).thenReturn("26 Jahre, 78kg");
        when(userProfileConfig.persona()).thenReturn("Direkter Coach");
    }

    @Test
    void shouldExecuteLogWeightFunctionCall() {
        // Arrange: Mock health data
        when(healthDataClient.fetchCurrentHealthData())
                .thenReturn(new HealthData(60, 85, 120));
        when(healthDataClient.fetch7DayAverages())
                .thenReturn(new TrendData(82, 58, 2100));
        when(nutritionApiClient.fetchTodayNutrition())
                .thenReturn(new dev.stat.chat.domain.NutritionData(1850, 140, 180, 60));

        // Mock LLM response with log_weight function call
        String functionCallResponse = """
                {
                  "functionCall": {
                    "name": "log_weight",
                    "args": {
                      "weight_kg": 75.5
                    }
                  }
                }
                """;
        when(llmClient.chat(anyString(), anyString(), isNull())).thenReturn(functionCallResponse);

        ChatRequest request = new ChatRequest("Ich wiege jetzt 75.5kg", List.of(), null);

        // Act
        ChatResponse response = chatService.processChat(request);

        // Assert
        assertNotNull(response);
        assertTrue(response.reply().contains("75.5") && response.reply().contains("kg"),
                "Response should confirm weight logging");

        // Verify logWeight was called with correct value
        verify(healthDataClient).logWeight(75.5);
    }

    @Test
    void shouldExecuteLogWaterFunctionCall() {
        // Arrange: Mock health data
        when(healthDataClient.fetchCurrentHealthData())
                .thenReturn(new HealthData(60, 85, 120));
        when(healthDataClient.fetch7DayAverages())
                .thenReturn(new TrendData(82, 58, 2100));
        when(nutritionApiClient.fetchTodayNutrition())
                .thenReturn(new dev.stat.chat.domain.NutritionData(1850, 140, 180, 60));

        // Mock LLM response with log_water function call
        String functionCallResponse = """
                {
                  "functionCall": {
                    "name": "log_water",
                    "args": {
                      "amount_ml": 500
                    }
                  }
                }
                """;
        when(llmClient.chat(anyString(), anyString(), isNull())).thenReturn(functionCallResponse);

        ChatRequest request = new ChatRequest("Ich habe gerade 500ml Wasser getrunken", List.of(), null);

        // Act
        ChatResponse response = chatService.processChat(request);

        // Assert
        assertNotNull(response);
        assertTrue(response.reply().contains("500") && response.reply().contains("ml"),
                "Response should confirm water logging");

        // Verify logWater was called with correct value
        verify(healthDataClient).logWater(500);
    }

    @Test
    void shouldThrowExceptionWhenLogWeightFails() {
        // Arrange: Mock health data
        when(healthDataClient.fetchCurrentHealthData())
                .thenReturn(new HealthData(60, 85, 120));
        when(healthDataClient.fetch7DayAverages())
                .thenReturn(new TrendData(82, 58, 2100));
        when(nutritionApiClient.fetchTodayNutrition())
                .thenReturn(new dev.stat.chat.domain.NutritionData(1850, 140, 180, 60));

        // Mock LLM response with log_weight function call
        String functionCallResponse = """
                {
                  "functionCall": {
                    "name": "log_weight",
                    "args": {
                      "weight_kg": 75.5
                    }
                  }
                }
                """;
        when(llmClient.chat(anyString(), anyString(), isNull())).thenReturn(functionCallResponse);

        // Simulate InfluxDB write failure
        doThrow(new ExternalServiceException("InfluxDB", "Write failed"))
                .when(healthDataClient).logWeight(75.5);

        ChatRequest request = new ChatRequest("Ich wiege jetzt 75.5kg", List.of(), null);

        // Act & Assert
        ExternalServiceException exception = assertThrows(
                ExternalServiceException.class,
                () -> chatService.processChat(request)
        );

        assertTrue(exception.getMessage().contains("InfluxDB") ||
                   exception.getMessage().contains("weight"),
                   "Exception message should mention InfluxDB or weight: " + exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenLogWaterFails() {
        // Arrange: Mock health data
        when(healthDataClient.fetchCurrentHealthData())
                .thenReturn(new HealthData(60, 85, 120));
        when(healthDataClient.fetch7DayAverages())
                .thenReturn(new TrendData(82, 58, 2100));
        when(nutritionApiClient.fetchTodayNutrition())
                .thenReturn(new dev.stat.chat.domain.NutritionData(1850, 140, 180, 60));

        // Mock LLM response with log_water function call
        String functionCallResponse = """
                {
                  "functionCall": {
                    "name": "log_water",
                    "args": {
                      "amount_ml": 500
                    }
                  }
                }
                """;
        when(llmClient.chat(anyString(), anyString(), isNull())).thenReturn(functionCallResponse);

        // Simulate InfluxDB write failure
        doThrow(new ExternalServiceException("InfluxDB", "Write failed"))
                .when(healthDataClient).logWater(500);

        ChatRequest request = new ChatRequest("Ich habe 500ml Wasser getrunken", List.of(), null);

        // Act & Assert
        ExternalServiceException exception = assertThrows(
                ExternalServiceException.class,
                () -> chatService.processChat(request)
        );

        assertTrue(exception.getMessage().contains("InfluxDB") ||
                   exception.getMessage().contains("water"),
                   "Exception message should mention InfluxDB or water: " + exception.getMessage());
    }
}
