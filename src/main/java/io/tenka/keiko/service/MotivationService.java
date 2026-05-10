package io.tenka.keiko.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Loads motivation pools (per-language) from {@code classpath:motivation/<lang>.json}.
 *
 * <p>Cross-subject by language per CLAUDE.md §8.4 — a calculus student
 * also benefits from 千里之行,始於足下. Pools live separately from any
 * specific subject's content directory.
 *
 * <p>v0.1 randomly picks one entry per request. Fisher-Yates shuffle +
 * server-backed cursor (matching the Python sibling's PR #6 design)
 * comes in a follow-up PR alongside per-user session state.
 */
@Service
public class MotivationService {

    private static final Logger log = LoggerFactory.getLogger(MotivationService.class);

    private final ObjectMapper jsonMapper;
    private List<Entry> zhEntries = List.of();
    private List<Entry> jpEntries = List.of();

    public MotivationService(ObjectMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @PostConstruct
    public void load() {
        zhEntries = loadPool("motivation/zh.json");
        jpEntries = loadPool("motivation/jp.json");
        log.info("Loaded motivation pools: zh={}, jp={}", zhEntries.size(), jpEntries.size());
    }

    private List<Entry> loadPool(String resourcePath) {
        ClassPathResource res = new ClassPathResource(resourcePath);
        if (!res.exists()) return List.of();
        try (InputStream in = res.getInputStream()) {
            Pool pool = jsonMapper.readValue(in, Pool.class);
            return pool.entries == null ? List.of() : List.copyOf(pool.entries);
        } catch (IOException e) {
            log.warn("Failed to load {}: {}", resourcePath, e.getMessage());
            return List.of();
        }
    }

    public Optional<Entry> randomFor(String langPreference) {
        // Prefer the requested language, but fall back to whichever pool
        // has entries — the engine should always say something.
        List<Entry> primary = "jp".equalsIgnoreCase(langPreference) ? jpEntries : zhEntries;
        List<Entry> fallback = "jp".equalsIgnoreCase(langPreference) ? zhEntries : jpEntries;
        List<Entry> source = !primary.isEmpty() ? primary : fallback;
        if (source.isEmpty()) return Optional.empty();
        int idx = ThreadLocalRandom.current().nextInt(source.size());
        return Optional.of(source.get(idx));
    }

    public record Entry(String text, String tip) {}

    private static final class Pool {
        public List<Entry> entries;
    }
}
