package dev.stat.chat.service;

import dev.stat.chat.domain.HealthData;
import dev.stat.chat.domain.NutritionData;
import dev.stat.chat.dto.ChatRequest;
import dev.stat.chat.dto.ChatResponse;
import dev.stat.chat.dto.MetricsUsed;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Orchestrates the chat flow: fetches health data, nutrition data,
 * builds a super-prompt, and sends it to the LLM.
 *
 * This is a deep module: simple interface (one public method),
 * complex implementation (data aggregation, prompt building, LLM call).
 */
@ApplicationScoped
public class ChatService {

    private final HealthDataClient healthDataClient;
    private final NutritionApiClient nutritionApiClient;
    private final LlmClient llmClient;

    ChatService(HealthDataClient healthDataClient,
                NutritionApiClient nutritionApiClient,
                LlmClient llmClient) {
        this.healthDataClient = healthDataClient;
        this.nutritionApiClient = nutritionApiClient;
        this.llmClient = llmClient;
    }

    /**
     * Processes a chat request by:
     * 1. Fetching health data from InfluxDB
     * 2. Fetching nutrition data from Spoonacular
     * 3. Building a super-prompt with all context
     * 4. Sending to LLM and returning the reply
     *
     * @throws ExternalServiceException if any external service fails (all-or-nothing)
     */
    public ChatResponse processChat(ChatRequest request) {
        HealthData health = healthDataClient.fetchCurrentHealthData();
        NutritionData nutrition = nutritionApiClient.fetchTodayNutrition();

        String prompt = buildPrompt(request, health, nutrition);
        String reply = llmClient.chat(prompt);

        var metrics = new MetricsUsed(
                health.bodyBattery(),
                health.sleepScore(),
                health.trainingLoadMinutes48h() + " mins"
        );

        return new ChatResponse(reply, metrics);
    }

    private String buildPrompt(ChatRequest request, HealthData health, NutritionData nutrition) {
        var sb = new StringBuilder();
        sb.append("You are an AI personal coach. Use the following data to give contextual advice.\n\n");

        sb.append("## Current Health Metrics\n");
        sb.append("- Body Battery: ").append(health.bodyBattery()).append("\n");
        sb.append("- Sleep Score (last night): ").append(health.sleepScore()).append("\n");
        sb.append("- Training Load (48h): ").append(health.trainingLoadMinutes48h()).append(" mins\n\n");

        sb.append("## Today's Nutrition\n");
        sb.append("- Calories: ").append(nutrition.calories()).append("\n");
        sb.append("- Protein: ").append(nutrition.proteinGrams()).append("g\n");
        sb.append("- Carbs: ").append(nutrition.carbsGrams()).append("g\n");
        sb.append("- Fat: ").append(nutrition.fatGrams()).append("g\n\n");

        if (request.chatHistory() != null && !request.chatHistory().isEmpty()) {
            sb.append("## Conversation History\n");
            for (var msg : request.chatHistory()) {
                sb.append(msg.role()).append(": ").append(msg.content()).append("\n");
            }
            sb.append("\n");
        }

        sb.append("## Current Question\n");
        sb.append("user: ").append(request.currentMessage()).append("\n");

        return sb.toString();
    }
}
