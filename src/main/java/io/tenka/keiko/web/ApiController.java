package io.tenka.keiko.web;

import io.tenka.keiko.domain.Card;
import io.tenka.keiko.domain.Choice;
import io.tenka.keiko.service.CardStore;
import io.tenka.keiko.service.MotivationService;
import io.tenka.keiko.service.PickerService;
import io.tenka.keiko.subject.Subject;
import io.tenka.keiko.web.dto.CardDto;
import io.tenka.keiko.web.dto.SubmitRequest;
import io.tenka.keiko.web.dto.SubmitResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * JSON API for the SPA. Mirrors the Python sibling's {@code /api/next/},
 * {@code /api/submit/}, {@code /api/pool-summary/}.
 *
 * <p>v0.1: random picker, no per-user session yet (review-state persistence
 * comes in a follow-up PR). For now {@code /api/submit/} just grades and
 * returns; it doesn't persist anything cross-request.
 */
@RestController
@RequestMapping("/api")
public class ApiController {

    private final CardStore cardStore;
    private final PickerService picker;
    private final MotivationService motivation;
    private final Subject subject;

    public ApiController(CardStore cardStore, PickerService picker,
                         MotivationService motivation, Subject subject) {
        this.cardStore = cardStore;
        this.picker = picker;
        this.motivation = motivation;
        this.subject = subject;
    }

    /**
     * GET /api/next — pick the next card.
     *
     * @param deck "default" (trusted) or "experimental" (everything)
     * @param seen comma-separated card ids the client has already shown
     */
    @GetMapping("/next")
    public ResponseEntity<?> next(
            @RequestParam(defaultValue = "default") String deck,
            @RequestParam(required = false) String seen) {
        Set<String> seenSet = new HashSet<>();
        if (seen != null && !seen.isBlank()) {
            for (String id : seen.split(",")) {
                String s = id.trim();
                if (!s.isEmpty()) seenSet.add(s);
            }
        }
        Optional<Card> picked = picker.pickNext(deck, seenSet);
        if (picked.isEmpty()) {
            int poolSize = picker.poolSize(deck);
            // {"exhausted": true} if the deck has cards but client has seen
            // them all; {"empty": true} if the deck itself is empty.
            String key = (poolSize > 0 && !seenSet.isEmpty()) ? "exhausted" : "empty";
            return ResponseEntity.ok(java.util.Map.of(key, true, "deck_size", poolSize));
        }
        return ResponseEntity.ok(CardDto.fromCard(picked.get()));
    }

    /**
     * POST /api/submit — record an answer and return correctness + the
     * per-card explanation (revealed only here, not in /api/next).
     */
    @PostMapping("/submit")
    public ResponseEntity<?> submit(@Valid @RequestBody SubmitRequest req) {
        Optional<Card> maybe = cardStore.findById(req.questionId());
        if (maybe.isEmpty()) {
            return ResponseEntity.badRequest().body(
                    java.util.Map.of("error", "unknown question_id: " + req.questionId()));
        }
        Card card = maybe.get();
        Choice correct = card.correctChoice();
        if (correct == null) {
            return ResponseEntity.internalServerError().body(
                    java.util.Map.of("error", "card has no correct choice: " + card.id()));
        }
        // Front face shuffles choices, so the client sends back position not id.
        // We don't actually need the position to grade — we just check whether the
        // client's chosen-text equals correct-text. But the position-only API is
        // simpler for v0.1; the client maps position → text from the rendered DTO.
        // For a clean grade pass, we resolve position by matching it back via the
        // ORIGINAL choices list (which is what stores correctness).
        // v0.1 punt: compare by index over ORIGINAL list. Front face will need to
        // remember the rendered shuffle order; the smoke test confirms the path.
        // (Adaptive picker + per-user session in the follow-up will rework this.)
        int pos = req.choicePosition();
        Choice chosen = (pos >= 1 && pos <= card.choices().size())
                ? card.choices().get(pos - 1)
                : null;
        boolean correctness = chosen != null && chosen.isCorrect();

        return ResponseEntity.ok(new SubmitResponse(
                correctness,
                correct.text(),
                card.explanation(),
                card.grammarPoint(),
                card.translationZhTw(),
                card.translationZhCn(),
                card.translationEn(),
                card.attribution()
        ));
    }

    /** GET /api/pool-summary — counts per deck, used by the masthead. */
    @GetMapping("/pool-summary")
    public ResponseEntity<?> poolSummary() {
        return ResponseEntity.ok(java.util.Map.of(
                "default", picker.poolSize("default"),
                "experimental", picker.poolSize("experimental")
        ));
    }

    /** GET /api/motivation — random pithy quote in the subject's preferred
     *  language pool. Decorative; safe to ignore on error. */
    @GetMapping("/motivation")
    public ResponseEntity<?> motivation() {
        // For non-Japanese-content subjects (us-conlaw, etc.) we still
        // serve the cross-subject ZH/JP pools — they're motivational
        // wisdom, not subject content. Pick the pool matching the
        // subject's primary language; fall back to ZH otherwise.
        String pref = "ja".equals(subject.primaryLanguage()) ? "jp" : "zh";
        return motivation.randomFor(pref)
                .map(e -> ResponseEntity.ok((Object) java.util.Map.of(
                        "text", e.text(),
                        "tip", e.tip() == null ? "" : e.tip()
                )))
                .orElseGet(() -> ResponseEntity.ok(java.util.Map.of()));
    }
}
