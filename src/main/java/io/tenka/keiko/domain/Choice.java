package io.tenka.keiko.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * One of the (typically 4) answer choices on a card.
 *
 * <p>{@code isCorrect} is the source-of-truth correctness flag, but the
 * front-facing API serializer omits it (so the client can't cheat by
 * inspecting the JSON before answering). See ApiController for the
 * stripped-choice serialization.
 */
public record Choice(
        String text,
        @JsonProperty("is_correct") boolean isCorrect
) {}
