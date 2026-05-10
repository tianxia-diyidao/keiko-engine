package io.tenka.keiko.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.tenka.keiko.domain.Card;
import io.tenka.keiko.subject.Subject;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Loads the active subject's cards.json into memory at startup and exposes
 * read access by id and by deck filter.
 *
 * <p>In-memory for v0.1. SQLite/JPA persistence + per-card review state
 * is queued for a follow-up PR (mirrors the Python sibling's ReviewStat
 * table). The cards themselves are immutable once loaded — content is
 * managed via cards.json edits + redeploy, never via the running app.
 */
@Service
public class CardStore {

    private static final Logger log = LoggerFactory.getLogger(CardStore.class);

    private final ObjectMapper jsonMapper;
    private final Subject subject;

    private List<Card> allCards = List.of();
    private Map<String, Card> byId = Map.of();

    public CardStore(ObjectMapper jsonMapper, Subject subject) {
        this.jsonMapper = jsonMapper;
        this.subject = subject;
    }

    @PostConstruct
    public void load() throws IOException {
        ClassPathResource res = new ClassPathResource(subject.cardsResourcePath());
        if (!res.exists()) {
            log.warn("No cards.json at classpath:{} — starting with empty deck",
                    subject.cardsResourcePath());
            return;
        }
        try (InputStream in = res.getInputStream()) {
            CardsFile file = jsonMapper.readValue(in, CardsFile.class);
            this.allCards = file.cards == null ? List.of() : List.copyOf(file.cards);
            Map<String, Card> idx = new HashMap<>(allCards.size() * 2);
            for (Card c : allCards) {
                if (c.id() != null) idx.put(c.id(), c);
            }
            this.byId = Map.copyOf(idx);
        }
        log.info("Loaded {} cards from {} for subject '{}'",
                allCards.size(), subject.cardsResourcePath(), subject.id());
    }

    public List<Card> all() {
        return allCards;
    }

    public Optional<Card> findById(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    /** Cards whose provenance falls in the active subject's default deck filter. */
    public List<Card> defaultDeck() {
        Set<String> ok = Set.copyOf(subject.defaultDeckFilter());
        return allCards.stream()
                .filter(c -> c.provenance() != null && ok.contains(c.provenance()))
                .toList();
    }

    /** Full pool, including experimental cards. */
    public List<Card> experimentalDeck() {
        return allCards;
    }

    /** Tiny wrapper for the {"cards": [...]} envelope used by cards.json. */
    private static final class CardsFile {
        public List<Card> cards;
    }
}
