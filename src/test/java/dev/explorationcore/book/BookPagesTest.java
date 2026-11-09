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
                new ExplorationTotals(1, 0, 1, 1, 1, 0, List.of(), 1, 1),
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
                new ExplorationTotals(2, 0, 1, 1, 1, 0, List.of(), 1, 1),
                List.of(new DiscoveryEntry("overworld", "region", null, "sakonotur", "Sakonotur"))
        );
        BookLine regions = null;
        for (BookLine line : book.pages().get(0)) {
            if (line.text().startsWith("Land Regions")) {
                regions = line;
            }
        }
        assertEquals(3, regions.page());

        Component page = BookPages.components(book).get(0);
        TextComponent label = find(page, "Land Regions");
        TextComponent count = findCount(page);
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

    @Test
    void chapterHeadingOnALaterPageIsBold() {
        PreparedBook book = new PreparedBook(List.of(
                List.of(BookLine.heading("Guides"), BookLine.link("Lore", 2)),
                List.of(BookLine.heading("Lore"), BookLine.of("Each region."), BookLine.link("< Guides", 1))
        ), List.of());

        TextComponent heading = (TextComponent) BookPages.components(book).get(1).children().get(0);
        assertEquals("Lore", heading.content());
        assertEquals(TextDecoration.State.TRUE, heading.decoration(TextDecoration.BOLD));
        assertEquals(TextColor.color(ExplorationBook.SERVER_NAME_COLOR), heading.color());
        assertEquals(TextDecoration.State.NOT_SET, heading.decoration(TextDecoration.ITALIC));
        assertNull(heading.clickEvent());
    }

    @Test
    void indexMarkKeepsThePrefixColor() {
        PreparedBook book = new PreparedBook(List.of(
                List.of(BookLine.heading("Guides"), BookLine.link(GuideBook.INDEX_MARK + "Lore", 2))
        ), List.of());
        Component page = BookPages.components(book).get(0);

        TextComponent mark = find(page, GuideBook.INDEX_MARK);
        TextComponent title = find(page, "Lore");
        assertEquals(TextColor.color(ExplorationBook.SERVER_NAME_COLOR), mark.color());
        assertEquals(ClickEvent.changePage(2), mark.clickEvent());
        assertEquals(TextDecoration.State.FALSE, mark.decoration(TextDecoration.UNDERLINED));
        assertEquals(TextColor.color(ExplorationBook.LINK_COLOR), title.color());
        assertEquals(ClickEvent.changePage(2), title.clickEvent());
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

    private static TextComponent findCount(Component component) {
        if (component instanceof TextComponent text && text.content().matches(" +\\d+/\\d+")) {
            return text;
        }
        for (Component child : component.children()) {
            TextComponent found = findCount(child);
            if (found != null) {
                return found;
            }
        }
        return null;
    }
}
