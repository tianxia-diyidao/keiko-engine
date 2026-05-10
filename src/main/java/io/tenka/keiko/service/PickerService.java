package io.tenka.keiko.service;

import io.tenka.keiko.domain.Card;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Picks the next card to show.
 *
 * <p>v0.1 strategy: uniform random pick over the active deck, excluding
 * card ids the client has already shown this session (passed via the
 * {@code seen} query param on /api/next/).
 *
 * <p>Follow-up PR: port the Python sibling's adaptive picker (EWMA-weighted
 * recency + difficulty modulator). Keeping the strategy interface narrow
 * here so swapping in the adaptive impl is a one-class change.
 */
@Service
public class PickerService {

    private final CardStore cardStore;

    public PickerService(CardStore cardStore) {
        this.cardStore = cardStore;
    }

    /**
     * Pick the next card. Returns empty if the deck is empty OR the
     * client has already seen everything in it.
     *
     * @param deck   "default" (trusted) or "experimental" (everything)
     * @param seen   ids the client has already shown — excluded from pick
     */
    public Optional<Card> pickNext(String deck, Set<String> seen) {
        List<Card> pool = "experimental".equals(deck)
                ? cardStore.experimentalDeck()
                : cardStore.defaultDeck();
        List<Card> remaining = new ArrayList<>(pool.size());
        for (Card c : pool) {
            if (seen == null || !seen.contains(c.id())) {
                remaining.add(c);
            }
        }
        if (remaining.isEmpty()) return Optional.empty();
        Collections.shuffle(remaining, ThreadLocalRandom.current());
        return Optional.of(remaining.get(0));
    }

    /** Pool size for the named deck. Used by the masthead readout. */
    public int poolSize(String deck) {
        return "experimental".equals(deck)
                ? cardStore.experimentalDeck().size()
                : cardStore.defaultDeck().size();
    }
}
