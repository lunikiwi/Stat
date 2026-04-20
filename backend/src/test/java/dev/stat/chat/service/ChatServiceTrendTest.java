package dev.stat.chat.service;

import dev.stat.chat.config.UserProfileConfig;
import dev.stat.chat.domain.HealthData;
import dev.stat.chat.domain.NutritionData;
import dev.stat.chat.domain.TrendData;
import dev.stat.chat.dto.ChatRequest;
import dev.stat.chat.dto.ChatResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ChatService's trend analysis functionality.
 * Tests that 7-day averages are properly integrated into the system prompt.
 */
@ExtendWith(MockitoExtension.class)
class ChatServiceTrendTest {

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
        chatService = new ChatService(
                healthDataClient,
                nutritionApiClient,
                llmClient,
                userProfileConfig
        );
    }

    private void setupUserProfileConfig() {
        when(userProfileConfig.persona()).thenReturn("You are a personal AI coach.");
        when(userProfileConfig.name()).thenReturn("Test User");
        when(userProfileConfig.goal()).thenReturn("Build muscle");
        when(userProfileConfig.diet()).thenReturn("High protein");
        when(userProfileConfig.metrics()).thenReturn("Weight: 80kg");
    }

    @Test
    void shouldInclude7DayAveragesInPrompt() {
        // Given
        setupUserProfileConfig();
        HealthData currentHealth = new HealthData(70, 85, 60);
        NutritionData currentNutrition = new NutritionData(2000, 150, 200, 70);
        TrendData trends = new TrendData(75, 65, 2100);

        when(healthDataClient.fetchCurrentHealthData()).thenReturn(currentHealth);
        when(healthDataClient.fetch7DayAverages()).thenReturn(trends);
        when(nutritionApiClient.fetchTodayNutrition()).thenReturn(currentNutrition);
        when(llmClient.chat(anyString(), anyString(), Mockito.nullable(String.class))).thenReturn("Great progress!");

        ChatRequest request = new ChatRequest("How am I doing?", List.of(), null);

        // When
        ChatResponse response = chatService.processChat(request);

        // Then
        assertNotNull(response);

        // Verify that fetch7DayAverages was called
        verify(healthDataClient).fetch7DayAverages();

        // Capture the prompt sent to LLM
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(llmClient).chat(promptCaptor.capture(), anyString(), Mockito.nullable(String.class));

        String capturedPrompt = promptCaptor.getValue();

        // Verify prompt contains 7-day averages
        assertTrue(capturedPrompt.contains("7-Day Averages"), "Prompt should contain 7-day averages section");
        assertTrue(capturedPrompt.contains("Sleep Score: 75"), "Prompt should contain avg sleep score");
        assertTrue(capturedPrompt.contains("Body Battery: 65"), "Prompt should contain avg body battery");
        assertTrue(capturedPrompt.contains("Calories: 2100"), "Prompt should contain avg calories");

        // Verify prompt contains current data
        assertTrue(capturedPrompt.contains("Current Health Metrics"));
        assertTrue(capturedPrompt.contains("Body Battery: 70"));
        assertTrue(capturedPrompt.contains("Sleep Score (last night): 85"));
    }

    @Test
    void shouldIncludeTrendAnalysisInstructionsInSystemContext() {
        // Given
        setupUserProfileConfig();
        HealthData currentHealth = new HealthData(70, 85, 60);
        NutritionData currentNutrition = new NutritionData(2000, 150, 200, 70);
        TrendData trends = new TrendData(75, 65, 2100);

        when(healthDataClient.fetchCurrentHealthData()).thenReturn(currentHealth);
        when(healthDataClient.fetch7DayAverages()).thenReturn(trends);
        when(nutritionApiClient.fetchTodayNutrition()).thenReturn(currentNutrition);
        when(llmClient.chat(anyString(), anyString(), Mockito.nullable(String.class))).thenReturn("Analysis complete");

        ChatRequest request = new ChatRequest("Analyze my trends", List.of(), null);

        // When
        chatService.processChat(request);

        // Then
        ArgumentCaptor<String> systemCaptor = ArgumentCaptor.forClass(String.class);
        verify(llmClient).chat(anyString(), systemCaptor.capture(), Mockito.nullable(String.class));

        String capturedSystem = systemCaptor.getValue();

        // Verify system context contains trend analysis instructions
        assertTrue(capturedSystem.contains("Trend Analysis"), "System context should mention trend analysis");
        assertTrue(capturedSystem.contains("7-day averages"), "System context should mention 7-day averages");
        assertTrue(capturedSystem.contains("trends"), "System context should mention trends");
    }

    @Test
    void shouldHandleFetch7DayAveragesFailure() {
        // Given
        when(healthDataClient.fetchCurrentHealthData()).thenReturn(new HealthData(70, 85, 60));
        when(healthDataClient.fetch7DayAverages())
                .thenThrow(new ExternalServiceException("InfluxDB", "Failed to fetch trends", null));

        ChatRequest request = new ChatRequest("How am I doing?", List.of(), null);

        // When/Then - Should propagate the exception
        ExternalServiceException exception = assertThrows(
                ExternalServiceException.class,
                () -> chatService.processChat(request)
        );

        assertTrue(exception.getMessage().contains("Failed to fetch trends"));
    }
}
