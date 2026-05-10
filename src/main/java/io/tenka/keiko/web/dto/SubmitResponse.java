package io.tenka.keiko.web.dto;

/** Response shape for POST /api/submit/. Reveals the credited correct
 * answer + the per-card explanation block (which the front-face DTO
 * deliberately withheld). */
public record SubmitResponse(
        boolean correct,
        String correctText,
        String explanation,
        String grammarPoint,
        String translationZhTw,
        String translationZhCn,
        String translationEn,
        String attribution
) {}
