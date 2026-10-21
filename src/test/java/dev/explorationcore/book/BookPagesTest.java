package dev.explorationcore.book;

import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BookPagesTest {
    @Test
    void serverNameIsBoldAndColored() {
        PreparedBook book = ExplorationBook.prepare(
                "Lowothra",
                new ExplorationTotals(1, 1, 1, 1, List.of(), 1, 1),
                List.of()
        );

        TextComponent page = (TextComponent) BookPages.components(book).get(0);
        TextComponent name = (TextComponent) page.children().get(0);
        assertEquals("Lowothra", name.content());
        assertEquals(TextColor.color(0x986A27), name.color());
        assertEquals(TextDecoration.State.TRUE, name.decoration(TextDecoration.BOLD));

        TextComponent rest = (TextComponent) page.children().get(2);
        assertEquals(ExplorationBook.SUBTITLE, rest.content().substring(0, rest.content().indexOf('\n')));
        assertEquals(TextDecoration.State.NOT_SET, rest.decoration(TextDecoration.BOLD));
        assertEquals(TextColor.color(0x704C2E), rest.color());

        TextComponent nether = (TextComponent) BookPages.components(book).get(1);
        assertEquals(TextDecoration.State.NOT_SET, nether.decoration(TextDecoration.BOLD));
        assertEquals(TextColor.color(0x704C2E), nether.color());
    }
}
