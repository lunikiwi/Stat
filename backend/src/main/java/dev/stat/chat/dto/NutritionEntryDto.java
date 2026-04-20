package dev.stat.chat.dto;

import java.time.Instant;

/**
 * DTO for a single nutrition entry (meal) in the journal.
 */
public record NutritionEntryDto(
        Instant timestamp,
        int calories,
        int proteinGrams,
        int carbsGrams,
        int fatGrams
) {
}
