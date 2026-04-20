package dev.stat.chat.service;

import dev.stat.chat.config.UserProfileConfig;
import dev.stat.chat.domain.HealthData;
import dev.stat.chat.domain.NutritionEntry;
import dev.stat.chat.domain.TrendData;
import dev.stat.chat.dto.ChatRequest;
import dev.stat.chat.dto.ChatResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ChatService get_nutrition_details function call handling.
 * Tests that the AI can retrieve today's individual nutrition entries.
 */
@ExtendWith(MockitoExtension.class)
class ChatServiceNutritionDetailsTest {

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
    void shouldExecuteGetNutritionDetailsFunctionCall() {
        // Arrange: Mock health data
        when(healthDataClient.fetchCurrentHealthData())
                .thenReturn(new HealthData(60, 85, 120));
        when(healthDataClient.fetch7DayAverages())
                .thenReturn(new TrendData(82, 58, 2100));
        when(nutritionApiClient.fetchTodayNutrition())
                .thenReturn(new dev.stat.chat.domain.NutritionData(1850, 140, 180, 60));

        // Mock nutrition entries for today
        List<NutritionEntry> todayEntries = List.of(
                new NutritionEntry(Instant.parse("2026-04-20T08:00:00Z"), 500, 30, 60, 15),
                new NutritionEntry(Instant.parse("2026-04-20T12:30:00Z"), 700, 40, 80, 25),
                new NutritionEntry(Instant.parse("2026-04-20T18:00:00Z"), 650, 70, 40, 20)
        );
        when(healthDataClient.getNutritionEntries(any(LocalDate.class)))
                .thenReturn(todayEntries);

        // Mock LLM response with get_nutrition_details function call
        String functionCallResponse = """
                {
                  "functionCall": {
                    "name": "get_nutrition_details",
                    "args": {}
                  }
                }
                """;
        when(llmClient.chat(anyString(), anyString(), isNull())).thenReturn(functionCallResponse);

        ChatRequest request = new ChatRequest("Was habe ich heute schon gegessen?", List.of(), null);

        // Act
        ChatResponse response = chatService.processChat(request);

        // Assert
        assertNotNull(response);
        assertTrue(response.reply().contains("500") || response.reply().contains("700") || response.reply().contains("650"),
                "Response should contain calorie values from entries");

        // Verify getNutritionEntries was called with today's date
        verify(healthDataClient).getNutritionEntries(eq(LocalDate.now()));
    }

    @Test
    void shouldReturnEmptyListWhenNoEntriesExist() {
        // Arrange: Mock health data
        when(healthDataClient.fetchCurrentHealthData())
                .thenReturn(new HealthData(60, 85, 120));
        when(healthDataClient.fetch7DayAverages())
                .thenReturn(new TrendData(82, 58, 2100));
        when(nutritionApiClient.fetchTodayNutrition())
                .thenReturn(new dev.stat.chat.domain.NutritionData(0, 0, 0, 0));

        // Mock empty nutrition entries
        when(healthDataClient.getNutritionEntries(any(LocalDate.class)))
                .thenReturn(List.of());

        // Mock LLM response with get_nutrition_details function call
        String functionCallResponse = """
                {
                  "functionCall": {
                    "name": "get_nutrition_details",
                    "args": {}
                  }
                }
                """;
        when(llmClient.chat(anyString(), anyString(), isNull())).thenReturn(functionCallResponse);

        ChatRequest request = new ChatRequest("Was habe ich heute gegessen?", List.of(), null);

        // Act
        ChatResponse response = chatService.processChat(request);

        // Assert
        assertNotNull(response);
        assertTrue(response.reply().contains("keine") || response.reply().contains("0") || response.reply().contains("leer"),
                "Response should indicate no entries found");

        // Verify getNutritionEntries was called
        verify(healthDataClient).getNutritionEntries(eq(LocalDate.now()));
    }

    @Test
    void shouldThrowExceptionWhenGetNutritionDetailsFails() {
        // Arrange: Mock health data
        when(healthDataClient.fetchCurrentHealthData())
                .thenReturn(new HealthData(60, 85, 120));
        when(healthDataClient.fetch7DayAverages())
                .thenReturn(new TrendData(82, 58, 2100));
        when(nutritionApiClient.fetchTodayNutrition())
                .thenReturn(new dev.stat.chat.domain.NutritionData(1850, 140, 180, 60));

        // Mock LLM response with get_nutrition_details function call
        String functionCallResponse = """
                {
                  "functionCall": {
                    "name": "get_nutrition_details",
                    "args": {}
                  }
                }
                """;
        when(llmClient.chat(anyString(), anyString(), isNull())).thenReturn(functionCallResponse);

        // Simulate InfluxDB query failure
        doThrow(new ExternalServiceException("InfluxDB", "Query failed"))
                .when(healthDataClient).getNutritionEntries(any(LocalDate.class));

        ChatRequest request = new ChatRequest("Was habe ich heute gegessen?", List.of(), null);

        // Act & Assert
        ExternalServiceException exception = assertThrows(
                ExternalServiceException.class,
                () -> chatService.processChat(request)
        );

        assertTrue(exception.getMessage().contains("InfluxDB") ||
                   exception.getMessage().contains("nutrition"),
                "Exception message should mention InfluxDB or nutrition: " + exception.getMessage());
    }
}
