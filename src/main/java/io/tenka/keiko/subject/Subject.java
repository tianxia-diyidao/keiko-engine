package io.tenka.keiko.subject;

import java.util.List;

/**
 * Active subject metadata, loaded from {@code subject.toml}.
 *
 * <p>Mirrors the Python sibling's {@code flashcards.subjects.Subject}.
 * Engine code reads this; templates pluck branding fields from it via the
 * {@code subject} model attribute.
 *
 * <p>Java record so the fields are immutable + getters are free + equals/
 * hashCode/toString are generated. {@code List<String>} fields are
 * defensively copied at construction.
 */
public record Subject(
        String id,
        String name,
        String brandMark,
        String subhead,
        String primaryLanguage,
        String defaultUiLanguage,
        List<String> explanationLanguages,
        List<String> defaultDeckFilter,
        String examDate,
        String cardsResourcePath
) {
    public Subject {
        // Defensive immutability for the lists.
        explanationLanguages = explanationLanguages == null
                ? List.of()
                : List.copyOf(explanationLanguages);
        defaultDeckFilter = defaultDeckFilter == null
                ? List.of("human_curated")
                : List.copyOf(defaultDeckFilter);
    }
}
