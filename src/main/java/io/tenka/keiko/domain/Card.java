package io.tenka.keiko.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * One flashcard. Schema mirrors the Python sibling's cards.json shape so
 * subject directories stay portable across both engines (same TOML, same
 * JSON, swap implementations freely).
 *
 * <p>{@code @JsonInclude(NON_DEFAULT)} keeps the wire payload tight —
 * sentence-ordering fields and translations are omitted when they're
 * empty/zero defaults (most cards in the us-conlaw subject won't use
 * sentence-ordering or non-English translations).
 */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public record Card(
        String id,
        String kind,                                 // "grammar" | "sentence_order" | etc.
        String level,                                // optional difficulty band
        String stem,
        @JsonProperty("grammar_point") String grammarPoint,
        List<Choice> choices,
        @JsonProperty("translation_zh_tw") String translationZhTw,
        @JsonProperty("translation_zh_cn") String translationZhCn,
        @JsonProperty("translation_en") String translationEn,
        String explanation,
        int difficulty,
        String tags,
        String source,
        String provenance,                           // human_curated | experimental | etc.
        String attribution,
        // PR #2 (v0.3): structured citations rendered as clickable links on
        // the back face. Primary source = SCOTUS opinions (supremecourt.gov
        // for recent, supreme.justia.com for older). Supplement with circuit
        // court opinions where the law is still developing, and top law-
        // review notes for scholarly treatment.
        List<Citation> citations
) {
    /** A choice is "correct" if its isCorrect flag is true. */
    public Choice correctChoice() {
        if (choices == null) return null;
        return choices.stream().filter(Choice::isCorrect).findFirst().orElse(null);
    }
}
