package dev.stat.chat.domain;

import java.time.Instant;

/**
 * Represents a single nutrition entry (meal) with all macronutrients.
 * Entries are grouped by their exact timestamp.
 */
public record NutritionEntry(
        Instant timestamp,
        int calories,
        int proteinGrams,
        int carbsGrams,
        int fatGrams
) {
}
