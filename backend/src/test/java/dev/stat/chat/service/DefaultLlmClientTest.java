package dev.stat.chat.service;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DefaultLlmClient using WireMock.
 * Tests the OpenAI/Gemini API integration with timeout and error handling.
 */
@QuarkusTest
@TestProfile(DefaultLlmClientTest.LlmTestProfile.class)
class DefaultLlmClientTest {

    private static WireMockServer wireMockServer;

    @Inject
    LlmClient llmClient;

    @BeforeAll
    static void startWireMock() {
        wireMockServer = new WireMockServer(WireMockConfiguration.options().port(9998));
        wireMockServer.start();
    }

    @AfterAll
    static void stopWireMock() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @BeforeEach
    void resetWireMock() {
        wireMockServer.resetAll();
    }

    @Test
    void shouldSendPromptToLlmAndReceiveResponse() {
        // Arrange: Mock LLM API response (OpenAI format)
        wireMockServer.stubFor(post(urlPathEqualTo("/v1/chat/completions"))
                .withHeader("Authorization", equalTo("Bearer test-key"))
                .withHeader("Content-Type", equalTo("application/json"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "id": "chatcmpl-123",
                                  "object": "chat.completion",
                                  "created": 1677652288,
                                  "model": "gpt-4",
                                  "choices": [{
                                    "index": 0,
                                    "message": {
                                      "role": "assistant",
                                      "content": "Da du in 2 Stunden Intervalle fährst und heute erst 120g Carbs hattest, empfehle ich dir jetzt sofort 2 Bananen."
                                    },
                                    "finish_reason": "stop"
                                  }]
                                }
                                """)));

        // Act
        String prompt = "User context: Body Battery 45, Sleep Score 85. What should I eat?";
        String result = llmClient.chat(prompt);

        // Assert
        assertNotNull(result);
        assertTrue(result.contains("Bananen") || result.contains("Carbs"));

        // Verify the request was made with correct headers
        wireMockServer.verify(postRequestedFor(urlPathEqualTo("/v1/chat/completions"))
                .withHeader("Authorization", equalTo("Bearer test-key")));
    }

    @Test
    void shouldThrowExceptionWhenLlmTimesOut() {
        // Arrange: Simulate timeout (delay > 5000ms)
        wireMockServer.stubFor(post(urlPathEqualTo("/v1/chat/completions"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withFixedDelay(6000))); // 6 seconds > 5 second timeout

        // Act & Assert
        ExternalServiceException exception = assertThrows(
                ExternalServiceException.class,
                () -> llmClient.chat("test prompt")
        );

        assertTrue(exception.getMessage().contains("LLM") ||
                   exception.getMessage().contains("timeout"));
    }

    @Test
    void shouldThrowExceptionWhenLlmReturns429RateLimit() {
        // Arrange: Simulate rate limiting
        wireMockServer.stubFor(post(urlPathEqualTo("/v1/chat/completions"))
                .willReturn(aResponse()
                        .withStatus(429)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\": {\"message\": \"Rate limit exceeded\"}}")));

        // Act & Assert
        ExternalServiceException exception = assertThrows(
                ExternalServiceException.class,
                () -> llmClient.chat("test prompt")
        );

        assertTrue(exception.getMessage().contains("LLM") ||
                   exception.getMessage().contains("429") ||
                   exception.getMessage().contains("rate limit"));
    }

    @Test
    void shouldThrowExceptionWhenLlmReturns500() {
        // Arrange: Simulate server error
        wireMockServer.stubFor(post(urlPathEqualTo("/v1/chat/completions"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error")));

        // Act & Assert
        ExternalServiceException exception = assertThrows(
                ExternalServiceException.class,
                () -> llmClient.chat("test prompt")
        );

        assertTrue(exception.getMessage().contains("LLM"));
    }

    @Test
    void shouldThrowExceptionWhenLlmReturnsInvalidJson() {
        // Arrange: Return invalid JSON
        wireMockServer.stubFor(post(urlPathEqualTo("/v1/chat/completions"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{ invalid json }")));

        // Act & Assert
        ExternalServiceException exception = assertThrows(
                ExternalServiceException.class,
                () -> llmClient.chat("test prompt")
        );

        assertTrue(exception.getMessage().contains("LLM"));
    }

    @Test
    void shouldIncludePromptInRequest() {
        // Arrange
        wireMockServer.stubFor(post(urlPathEqualTo("/v1/chat/completions"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "choices": [{
                                    "message": {
                                      "content": "Response"
                                    }
                                  }]
                                }
                                """)));

        // Act
        String prompt = "Custom test prompt with health data";
        llmClient.chat(prompt);

        // Assert: Verify the prompt was included in the request body
        wireMockServer.verify(postRequestedFor(urlPathEqualTo("/v1/chat/completions"))
                .withRequestBody(containing("Custom test prompt")));
    }

    /**
     * Test profile to configure WireMock URL for LLM.
     */
    public static class LlmTestProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "stat.llm.base-url", "http://localhost:9998",
                    "stat.llm.api-key", "test-key",
                    "stat.llm.model", "gpt-4",
                    "stat.external.timeout-ms", "5000"
            );
        }
    }
}
