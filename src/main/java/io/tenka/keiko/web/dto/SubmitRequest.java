package io.tenka.keiko.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Body of POST /api/submit/. */
public record SubmitRequest(
        @NotBlank String questionId,
        @NotNull Integer choicePosition   // 1..N (matches CardDto.ChoiceDto.position)
) {}
