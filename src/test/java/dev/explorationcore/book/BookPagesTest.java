package dev.explorationcore.book;

import dev.explorationcore.db.DiscoveryEntry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

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
        assertEquals(TextDecoration.State.NOT_SET, name.decoration(TextDecoration.ITALIC));
        assertNull(name.clickEvent());

        TextComponent subtitle = (TextComponent) page.children().get(2);
        assertEquals(ExplorationBook.SUBTITLE, subtitle.content());
        assertEquals(TextDecoration.State.NOT_SET, subtitle.decoration(TextDecoration.BOLD));
        assertEquals(TextDecoration.State.NOT_SET, subtitle.decoration(TextDecoration.ITALIC));
        assertEquals(TextColor.color(0x704C2E), subtitle.color());
        assertNull(subtitle.clickEvent());

        TextComponent nether = (TextComponent) BookPages.components(book).get(1).children().get(0);
        assertEquals(TextDecoration.State.NOT_SET, nether.decoration(TextDecoration.BOLD));
        assertEquals(TextColor.color(0x704C2E), nether.color());
    }

    @Test
    void categoryLabelIsTheOnlyLinkedPartOfACountedLine() {
        PreparedBook book = ExplorationBook.prepare(
                "Lowothra",
                new ExplorationTotals(2, 1, 1, 1, List.of(), 1, 1),
                List.of(new DiscoveryEntry("overworld", "region", null, "sakonotur", "Sakonotur"))
        );
        BookLine regions = null;
        for (BookLine line : book.pages().get(0)) {
            if (line.text().startsWith("Regions:")) {
                regions = line;
            }
        }
        assertEquals(3, regions.page());

        Component page = BookPages.components(book).get(0);
        TextComponent label = find(page, "Regions");
        TextComponent count = findPrefix(page, ": ");
        assertEquals(TextDecoration.State.FALSE, label.decoration(TextDecoration.UNDERLINED));
        assertEquals(ClickEvent.changePage(3), label.clickEvent());
        assertEquals(TextColor.color(ExplorationBook.LINK_COLOR), label.color());
        assertEquals(TextDecoration.State.NOT_SET, label.decoration(TextDecoration.ITALIC));
        assertEquals(TextDecoration.State.FALSE, count.decoration(TextDecoration.UNDERLINED));
        assertEquals(TextColor.color(ExplorationBook.BODY_COLOR), count.color());
        assertNull(count.clickEvent());

        TextComponent summary = find(BookPages.components(book).get(2), ExplorationBook.SUMMARY_LINK);
        assertEquals(TextDecoration.State.FALSE, summary.decoration(TextDecoration.UNDERLINED));
        assertEquals(ClickEvent.changePage(1), summary.clickEvent());
        assertEquals(TextColor.color(ExplorationBook.LINK_COLOR), summary.color());
    }

    private static TextComponent find(Component component, String content) {
        if (component instanceof TextComponent text && content.equals(text.content())) {
            return text;
        }
        for (Component child : component.children()) {
            TextComponent found = find(child, content);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private static TextComponent findPrefix(Component component, String prefix) {
        if (component instanceof TextComponent text && text.content().startsWith(prefix)) {
            return text;
        }
        for (Component child : component.children()) {
            TextComponent found = findPrefix(child, prefix);
            if (found != null) {
                return found;
            }
        }
        return null;
    }
}
