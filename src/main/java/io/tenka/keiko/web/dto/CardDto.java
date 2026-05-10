package io.tenka.keiko.web.dto;

import io.tenka.keiko.domain.Card;
import io.tenka.keiko.domain.Choice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Front-face card payload — strips {@code is_correct} from choices so the
 * client can't cheat by inspecting JSON before answering. Mirrors the
 * Python sibling's {@code _serialize_question}.
 */
public record CardDto(
        String id,
        String kind,
        String level,
        String stem,
        String grammarPoint,
        List<ChoiceDto> choices,
        String translationZhTw,
        String translationZhCn,
        String translationEn,
        String explanation,
        int difficulty,
        String tags,
        String provenance,
        String attribution
) {
    public record ChoiceDto(int position, String text) {}

    public static CardDto fromCard(Card c) {
        // Shuffle choices so the answer-position bias doesn't sneak in.
        List<Choice> shuffled = c.choices() == null
                ? List.of()
                : new ArrayList<>(c.choices());
        Collections.shuffle(shuffled, ThreadLocalRandom.current());
        List<ChoiceDto> dtoChoices = new ArrayList<>(shuffled.size());
        for (int i = 0; i < shuffled.size(); i++) {
            dtoChoices.add(new ChoiceDto(i + 1, shuffled.get(i).text()));
        }
        return new CardDto(
                c.id(), c.kind(), c.level(), c.stem(),
                c.grammarPoint(), dtoChoices,
                c.translationZhTw(), c.translationZhCn(), c.translationEn(),
                // EXPLANATION is intentionally excluded from /api/next/ —
                // it's revealed only on /api/submit/ response.
                null,
                c.difficulty(), c.tags(), c.provenance(), c.attribution()
        );
    }
}
