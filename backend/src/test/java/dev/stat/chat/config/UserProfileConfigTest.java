package dev.stat.chat.config;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for UserProfileConfig.
 * Verifies that user profile properties are correctly loaded from application.properties.
 */
@QuarkusTest
class UserProfileConfigTest {

    @Inject
    UserProfileConfig config;

    @Test
    void shouldLoadUserName() {
        assertNotNull(config.name(), "User name should not be null");
        assertFalse(config.name().isBlank(), "User name should not be blank");
    }

    @Test
    void shouldLoadUserGoal() {
        assertNotNull(config.goal(), "User goal should not be null");
        assertTrue(config.goal().contains("Fettabbau") || config.goal().contains("endurance"),
                "User goal should contain meaningful content");
    }

    @Test
    void shouldLoadUserDiet() {
        assertNotNull(config.diet(), "User diet should not be null");
        assertFalse(config.diet().isBlank(), "User diet should not be blank");
    }

    @Test
    void shouldLoadUserMetrics() {
        assertNotNull(config.metrics(), "User metrics should not be null");
        assertFalse(config.metrics().isBlank(), "User metrics should not be blank");
    }

    @Test
    void shouldLoadUserPersona() {
        assertNotNull(config.persona(), "User persona should not be null");
        assertTrue(config.persona().contains("Coach") || config.persona().contains("coach"),
                "User persona should describe coach behavior");
    }
}
