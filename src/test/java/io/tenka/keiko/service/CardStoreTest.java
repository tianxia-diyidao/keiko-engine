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
    void initialBankIsFiftyCards() {
        assertThat(cardStore.all()).hasSize(50);
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
