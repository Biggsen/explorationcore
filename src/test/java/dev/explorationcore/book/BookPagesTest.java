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
    void commandsAreDarkGreen() {
        String crates = "You can check them with /keys and open crates with /cc.";
        String bolt = "Give friends access using /bolt trust add <player>.";
        String fraction = "A single map covers 1/16 of the entire world.";
        String lore = "To read a region's record, use /lore [region name]";
        assertEquals("/keys/cc", greenText(crates));
        assertEquals("/bolt trust add <player>", greenText(bolt));
        assertEquals("/lore [region name]", greenText(lore));
        assertEquals("", greenText(fraction));

        BookText.Wrapped keys = BookText.wrapPlain("with /keys.").get(0);
        PreparedBook book = new PreparedBook(List.of(List.of(
                BookLine.heading("Crates"),
                BookLine.of(keys.text(), keys.green())
        )), List.of());
        TextComponent command = find(BookPages.components(book).get(0), "/keys");
        assertEquals(TextColor.color(GuideBook.COMMAND_COLOR), command.color());
        assertEquals(TextDecoration.State.NOT_SET, command.decoration(TextDecoration.ITALIC));
    }

    private static String greenText(String text) {
        StringBuilder green = new StringBuilder();
        for (BookText.Wrapped line : BookText.wrapPlain(text)) {
            List<Integer> spans = line.green();
            for (int index = 0; index + 1 < spans.size(); index += 2) {
                green.append(line.text(), spans.get(index), spans.get(index + 1));
            }
        }
        return green.toString();
    }

    @Test
    void guidesCoverUsesTitlePageColors() throws GuidesException {
        PreparedBook book = GuideBook.prepare("Lowothra", List.of(new GuideChapter("Welcome", "Hello.")));
        Component page = BookPages.components(book).get(0);

        TextComponent title = findContaining(page, "EXPLORATION");
        assertEquals(TextColor.color(ExplorationBook.SERVER_NAME_COLOR), title.color());
        assertEquals(TextDecoration.State.TRUE, title.decoration(TextDecoration.BOLD));
        assertNull(title.clickEvent());

        TextComponent subtitle = findContaining(page, "Field Guide");
        assertEquals(TextColor.color(ExplorationBook.SERVER_NAME_COLOR), subtitle.color());
        assertEquals(TextDecoration.State.NOT_SET, subtitle.decoration(TextDecoration.BOLD));

        TextComponent motto = findContaining(page, "Where the journey");
        assertEquals(TextColor.color(ExplorationBook.BODY_COLOR), motto.color());
        assertEquals(TextDecoration.State.NOT_SET, motto.decoration(TextDecoration.ITALIC));
        assertNull(motto.clickEvent());
        assertNull(findContaining(page, "Travel. Discover."));
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

    private static TextComponent findContaining(Component component, String content) {
        if (component instanceof TextComponent text && text.content().contains(content)) {
            return text;
        }
        for (Component child : component.children()) {
            TextComponent found = findContaining(child, content);
            if (found != null) {
                return found;
            }
        }
        return null;
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
