package io.tenka.keiko.subject;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/** Smoke test: the active subject loads from us-conlaw/subject.toml at boot. */
@SpringBootTest
class SubjectLoaderTest {

    @Autowired Subject subject;

    @Test
    void defaultSubjectIsUsConLaw() {
        assertThat(subject.id()).isEqualTo("us-conlaw");
        assertThat(subject.name()).isEqualTo("US Federal Constitutional Law");
        assertThat(subject.brandMark()).isEqualTo("Lex");
        assertThat(subject.primaryLanguage()).isEqualTo("en");
        assertThat(subject.cardsResourcePath()).isEqualTo("subjects/us-conlaw/cards.json");
    }

    @Test
    void defaultDeckFilterIncludesHumanCurated() {
        assertThat(subject.defaultDeckFilter()).contains("human_curated");
    }
}
