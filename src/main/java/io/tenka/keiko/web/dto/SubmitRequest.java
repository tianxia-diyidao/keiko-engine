package io.tenka.keiko.web.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Body of POST /api/submit/.
 *
 * <p>v0.2: graded by TEXT match, not position. The DTO sent on /api/next/
 * shuffles choices for anti-bias and renumbers them 1..4 in shuffle
 * order — so a position from the client doesn't map back cleanly to the
 * server's stored (un-shuffled) choice list. Sending the chosen TEXT
 * sidesteps the whole shuffle-coordinate-translation problem.
 */
public record SubmitRequest(
        @NotBlank String questionId,
        @NotBlank String chosenText
) {}
