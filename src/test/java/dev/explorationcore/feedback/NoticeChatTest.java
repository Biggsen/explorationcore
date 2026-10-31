package dev.explorationcore.feedback;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class NoticeChatTest {
    @Test
    void defaultPrefixIsAGoldMarkBeforeTheSentence() {
        Component line = NoticeChat.line("§6⌖ §r", "You found Little Ivory!");

        TextComponent root = (TextComponent) line;
        assertEquals("", root.content());
        TextComponent mark = (TextComponent) root.children().get(0);
        assertEquals("⌖ ", mark.content());
        assertEquals(NamedTextColor.GOLD, mark.color());
        TextComponent sentence = (TextComponent) root.children().get(1);
        assertEquals("You found Little Ivory!", sentence.content());
        assertNull(sentence.color());
    }

    @Test
    void blankPrefixLeavesTheSentenceAlone() {
        Component line = NoticeChat.line("   ", "You found Little Ivory!");

        TextComponent sentence = (TextComponent) line;
        assertEquals("You found Little Ivory!", sentence.content());
        assertEquals(0, sentence.children().size());
    }
}
