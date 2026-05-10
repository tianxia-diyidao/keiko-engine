package io.tenka.keiko;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * keiko-engine — content-agnostic flashcard study engine.
 *
 * <p>Lives at the package root by convention so Spring's component-scan
 * (and {@code @SpringBootTest}'s auto-configuration discovery) reaches
 * every sibling package: domain, service, subject, web.
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
