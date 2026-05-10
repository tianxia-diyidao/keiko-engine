package io.tenka.keiko.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * keiko-engine — content-agnostic flashcard study engine.
 *
 * <p>Engine ↔ content split mirrors the Python sibling project: the engine
 * (this codebase) is subject-blind; the active subject is loaded at boot
 * from {@code STUDY_SUBJECT} env var (default {@code us-conlaw}) and
 * supplies its cards.json + subject.toml manifest from
 * {@code src/main/resources/subjects/<id>/}.
 *
 * <p>Adding a new subject (calculus, organic chem, JLPT, etc.) is a
 * directory + manifest, not a fork.
 */
@SpringBootApplication
public class KeikoEngineApplication {
    public static void main(String[] args) {
        SpringApplication.run(KeikoEngineApplication.class, args);
    }
}
