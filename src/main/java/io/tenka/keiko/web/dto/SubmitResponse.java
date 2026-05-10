package io.tenka.keiko.web.dto;

import io.tenka.keiko.domain.Citation;

import java.util.List;

/** Response shape for POST /api/submit/. Reveals the credited correct
 * answer + the per-card explanation block (which the front-face DTO
 * deliberately withheld) + structured citations for the back face. */
public record SubmitResponse(
        boolean correct,
        String correctText,
        String explanation,
        String grammarPoint,
        String translationZhTw,
        String translationZhCn,
        String translationEn,
        String attribution,
        List<Citation> citations
) {}
