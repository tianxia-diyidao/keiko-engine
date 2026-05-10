package io.tenka.keiko.service;

import io.tenka.keiko.domain.Card;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/** Verifies that the us-conlaw cards.json loads with the expected size + integrity. */
@SpringBootTest
class CardStoreTest {

    @Autowired CardStore cardStore;

    @Test
    void cardBankSizeMatchesStandingRule() {
        // Standing rule (CLAUDE.md §5.2): every PR adds 50 validated cards.
        // PR #1 (initial): 50. PR #3 (this): +50. Bump in lockstep with each
        // 50-card additions PR going forward.
        assertThat(cardStore.all()).hasSize(150);
    }

    @Test
    void everyCardHasAtLeastOneCitationWithUrl() {
        // PR #3 (v0.3) feature: structured citations with links. Every card
        // gets at least one (the lead SCOTUS opinion).
        for (Card c : cardStore.all()) {
            assertThat(c.citations())
                    .as("card %s should have citations", c.id())
                    .isNotNull()
                    .isNotEmpty();
            c.citations().forEach(cite -> {
                assertThat(cite.url())
                        .as("card %s citation '%s' must have a non-blank url", c.id(), cite.label())
                        .isNotBlank();
                assertThat(cite.label())
                        .as("card %s citation must have a non-blank label", c.id())
                        .isNotBlank();
            });
        }
    }

    @Test
    void everyCardHasExactlyOneCorrectChoice() {
        for (Card c : cardStore.all()) {
            long correctCount = c.choices().stream().filter(ch -> ch.isCorrect()).count();
            assertThat(correctCount)
                    .as("card %s should have exactly 1 correct choice", c.id())
                    .isEqualTo(1L);
        }
    }

    @Test
    void everyCardHasFourChoices() {
        for (Card c : cardStore.all()) {
            assertThat(c.choices())
                    .as("card %s", c.id())
                    .hasSize(4);
        }
    }

    @Test
    void allCardsAreHumanCuratedSoDefaultDeckEqualsAll() {
        // Initial bank: every card is human_curated → default deck == full pool.
        assertThat(cardStore.defaultDeck()).hasSameSizeAs(cardStore.all());
    }
}
