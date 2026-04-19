package dev.stat.chat.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.stat.chat.config.UserProfileConfig;
import dev.stat.chat.domain.HealthData;
import dev.stat.chat.domain.NutritionData;
import dev.stat.chat.dto.ChatRequest;
import dev.stat.chat.dto.ChatResponse;
import dev.stat.chat.dto.MetricsUsed;
import dev.stat.chat.dto.NutritionLogRequest;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

/**
 * Orchestrates the chat flow: fetches health data, nutrition data,
 * builds a super-prompt, and sends it to the LLM.
 * Handles Gemini Function Calling for structured actions like logging nutrition.
 *
 * This is a deep module: simple interface (one public method),
 * complex implementation (data aggregation, prompt building, LLM call, function execution).
 */
@ApplicationScoped
public class ChatService {

    private static final Logger LOG = Logger.getLogger(ChatService.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final HealthDataClient healthDataClient;
    private final NutritionApiClient nutritionApiClient;
    private final LlmClient llmClient;
    private final UserProfileConfig userProfileConfig;

    ChatService(HealthDataClient healthDataClient,
                NutritionApiClient nutritionApiClient,
                LlmClient llmClient,
                UserProfileConfig userProfileConfig) {
        this.healthDataClient = healthDataClient;
        this.nutritionApiClient = nutritionApiClient;
        this.llmClient = llmClient;
        this.userProfileConfig = userProfileConfig;
    }

    /**
     * Processes a chat request by:
     * 1. Fetching health data from InfluxDB
     * 2. Fetching nutrition data from Spoonacular
     * 3. Building a super-prompt with all context
     * 4. Sending to LLM and returning the reply
     * 5. If LLM returns a function call, execute it and return confirmation
     *
     * @throws ExternalServiceException if any external service fails (all-or-nothing)
     */
    public ChatResponse processChat(ChatRequest request) {
        HealthData health = healthDataClient.fetchCurrentHealthData();
        NutritionData nutrition = nutritionApiClient.fetchTodayNutrition();

        String systemContext = buildSystemContext();
        String prompt = buildPrompt(request, health, nutrition);
        String llmResponse = llmClient.chat(prompt, systemContext);

        // Check if response is a function call
        String reply = handleLlmResponse(llmResponse);

        var metrics = new MetricsUsed(
                health.bodyBattery(),
                health.sleepScore(),
                health.trainingLoadMinutes48h() + " mins"
        );

        return new ChatResponse(reply, metrics);
    }

    /**
     * Processes a silent nutrition log request by:
     * 1. Building a special "silent prompt" that instructs the LLM to extract macros
     * 2. Sending to LLM with function calling enabled
     * 3. Executing the log_nutrition function call
     * 4. Returning a confirmation message
     *
     * This is a "silent" operation - no chat history, minimal response.
     *
     * @param request The nutrition log request with meal description
     * @return Confirmation message from the LLM
     * @throws ExternalServiceException if LLM or InfluxDB fails
     */
    public String processSilentNutritionLog(NutritionLogRequest request) {
        String systemContext = buildSilentSystemContext();
        String prompt = buildSilentPrompt(request.description());

        LOG.infof("Processing silent nutrition log for: %s", request.description());

        String llmResponse = llmClient.chat(prompt, systemContext);

        // Handle the LLM response (should be a function call)
        String reply = handleLlmResponse(llmResponse);

        LOG.infof("Silent nutrition log completed: %s", reply);

        return reply;
    }

    /**
     * Builds a minimal system context for silent nutrition logging.
     */
    private String buildSilentSystemContext() {
        return """
                # Nutrition Extraction System

                ## Your Role
                You are a nutrition data extraction assistant. Your job is to:
                1. Parse the meal description
                2. Estimate the macronutrients (calories, protein, carbs, fat)
                3. Call the log_nutrition function with the extracted values
                4. Respond with a brief confirmation of the logged values

                ## Instructions
                - Be accurate in your estimations
                - Use the log_nutrition function to record the data
                - Keep your confirmation message concise (one sentence)
                - Format: "Mahlzeit erfasst! X kcal (Xg Protein, Xg Carbs, Xg Fett)"
                """;
    }

    /**
     * Builds the silent prompt for nutrition extraction.
     */
    private String buildSilentPrompt(String mealDescription) {
        return String.format("""
                Extrahiere die Makros aus folgendem Text und logge sie mit dem log_nutrition Tool.
                Antworte nur mit einer kurzen Bestätigung der Werte.

                Mahlzeit: %s
                """, mealDescription);
    }

    /**
     * Builds the system context instruction for the LLM.
     * This defines the AI coach's persona and the user's profile.
     */
    private String buildSystemContext() {
        var sb = new StringBuilder();
        sb.append("# AI Coach System Context\n\n");

        sb.append("## Your Role\n");
        sb.append(userProfileConfig.persona()).append("\n\n");

        sb.append("## User Profile\n");
        sb.append("- Name: ").append(userProfileConfig.name()).append("\n");
        sb.append("- Goal: ").append(userProfileConfig.goal()).append("\n");
        sb.append("- Diet: ").append(userProfileConfig.diet()).append("\n");
        sb.append("- Metrics: ").append(userProfileConfig.metrics()).append("\n\n");

        sb.append("## Instructions\n");
        sb.append("- Use the provided health and nutrition data to give personalized, actionable advice.\n");
        sb.append("- Be direct and data-driven in your responses.\n");
        sb.append("- Reference specific metrics when making recommendations.\n");
        sb.append("- Keep responses concise and focused on the user's goal.\n");

        return sb.toString();
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

    /**
     * Handles LLM response: either returns text directly or executes function calls.
     *
     * @throws ExternalServiceException if function execution fails (e.g., InfluxDB write error)
     */
    private String handleLlmResponse(String llmResponse) {
        // Check if response contains a function call
        if (llmResponse.startsWith("{") && llmResponse.contains("functionCall")) {
            try {
                JsonNode root = OBJECT_MAPPER.readTree(llmResponse);
                JsonNode functionCall = root.get("functionCall");

                if (functionCall != null) {
                    String functionName = functionCall.get("name").asText();
                    JsonNode args = functionCall.get("args");

                    if ("log_nutrition".equals(functionName)) {
                        return executeLogNutrition(args);
                    } else {
                        LOG.warnf("Unknown function call: %s", functionName);
                        return "Funktion nicht unterstützt: " + functionName;
                    }
                }
            } catch (ExternalServiceException e) {
                // Re-throw ExternalServiceException to propagate InfluxDB errors
                throw e;
            } catch (Exception e) {
                LOG.errorf(e, "Failed to parse function call from LLM response");
                return "Fehler beim Verarbeiten der Antwort.";
            }
        }

        // Regular text response
        return llmResponse;
    }

    /**
     * Executes the log_nutrition function call.
     */
    private String executeLogNutrition(JsonNode args) {
        try {
            int calories = args.get("calories").asInt();
            int protein = args.get("protein").asInt();
            int carbs = args.get("carbs").asInt();
            int fat = args.get("fat").asInt();

            healthDataClient.logNutrition(calories, protein, carbs, fat);

            LOG.infof("Function call executed: log_nutrition(%d, %d, %d, %d)",
                    calories, protein, carbs, fat);

            return String.format("Mahlzeit erfasst! %d kcal (%dg Protein, %dg Carbs, %dg Fett)",
                    calories, protein, carbs, fat);
        } catch (Exception e) {
            LOG.errorf(e, "Failed to execute log_nutrition function");
            throw new ExternalServiceException("InfluxDB",
                    "Failed to log nutrition data: " + e.getMessage(), e);
        }
    }
}
