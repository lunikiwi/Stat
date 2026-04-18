package dev.stat.chat.config;

import io.smallrye.config.ConfigMapping;

/**
 * Configuration for user profile information.
 * These properties define the user's context for the AI coach.
 */
@ConfigMapping(prefix = "stat.user")
public interface UserProfileConfig {

    /**
     * User's name.
     */
    String name();

    /**
     * User's fitness/health goal (e.g., "Improve endurance and maintain healthy weight").
     */
    String goal();

    /**
     * User's dietary preferences and restrictions.
     */
    String diet();

    /**
     * User's relevant metrics (e.g., age, weight, activity level).
     */
    String metrics();

    /**
     * AI coach persona and communication style.
     */
    String persona();
}
