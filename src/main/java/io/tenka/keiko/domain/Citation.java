package io.tenka.keiko.domain;

/**
 * One citation attached to a card — typically the leading SCOTUS case
 * + (optionally) a circuit-court or law-review supplement.
 *
 * <p>Renders on the back face as a clickable link. {@code type} drives
 * a small visual tag (SC ◆, CCA ▲, LR ◇).
 *
 * <p>URL conventions:
 * <ul>
 *   <li>SCOTUS recent: {@code https://www.supremecourt.gov/opinions/...}</li>
 *   <li>SCOTUS older: {@code https://supreme.justia.com/cases/federal/us/<vol>/<page>/}</li>
 *   <li>Cornell LII: {@code https://www.law.cornell.edu/supremecourt/text/<vol>/<page>}</li>
 *   <li>Law reviews: journal homepage or specific article URL</li>
 * </ul>
 */
public record Citation(
        String label,   // e.g. "Lujan v. Defenders of Wildlife, 504 U.S. 555 (1992)"
        String url,     // direct link
        String type     // "scotus" | "circuit" | "law-review" | "other"
) {}
