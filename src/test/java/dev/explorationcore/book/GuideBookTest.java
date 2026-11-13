package dev.explorationcore.book;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuideBookTest {
    @TempDir
    Path tempDir;

    @Test
    void indexLinksChaptersAndEachChapterReturnsToTheIndex() throws GuidesException {
        StringBuilder longBody = new StringBuilder();
        for (int line = 0; line < 11; line++) {
            longBody.append("Line ").append(line).append('\n');
        }
        PreparedBook book = GuideBook.prepare("Charidh", List.of(
                new GuideChapter("Welcome to {SERVER_NAME}", longBody.toString()),
                new GuideChapter("Lore", "Each region carries its own record.")
        ));

        List<BookLine> cover = book.pages().get(0);
        assertEquals(BookText.LINES_PER_PAGE, cover.size());
        assertTrue(texts(cover).stream().anyMatch(text -> text.contains("EXPLORATION")));
        assertTrue(texts(cover).stream().anyMatch(text -> text.contains("Field Guide")));
        for (BookLine line : cover) {
            assertTrue(BookText.width(line.text(), line.bold()) <= BookText.PAGE_WIDTH, line.text());
        }

        List<BookLine> index = book.pages().get(1);
        assertEquals(GuideBook.TITLE, index.get(0).text());
        assertTrue(index.get(0).bold());
        assertNull(index.get(0).color());
        assertEquals("Welcome to Charidh", link(index, "Welcome to Charidh").text());
        assertEquals(3, link(index, "Welcome to Charidh").page());
        assertEquals(5, link(index, "Lore").page());
        assertFalse(texts(index).contains("Regions & difficulty"));

        List<BookLine> first = book.pages().get(2);
        assertEquals("Welcome to Charidh", first.get(0).text());
        assertTrue(first.get(0).bold());
        assertTrue(texts(first).contains("Line 0"));
        assertEquals(GuideBook.RETURN_LINK, first.get(first.size() - 1).text());
        assertEquals(GuideBook.INDEX_PAGE, first.get(first.size() - 1).page());
        assertEquals(GuideBook.RETURN_LINK, book.pages().get(3).get(book.pages().get(3).size() - 1).text());

        List<BookLine> lore = book.pages().get(4);
        assertEquals("Lore", lore.get(0).text());
        assertTrue(lore.get(0).bold());
        assertEquals(BookText.LINES_PER_PAGE, lore.size());
        assertEquals(GuideBook.RETURN_LINK, lore.get(lore.size() - 1).text());
        assertEquals(GuideBook.INDEX_PAGE, lore.get(lore.size() - 1).page());
        assertTrue(texts(lore).contains(""));
    }

    @Test
    void bundledChaptersKeepMenuOrderAndFillTheServerName() throws Exception {
        try (InputStream in = GuideBook.class.getClassLoader().getResourceAsStream("guides.yml")) {
            assertNotNull(in);
            Path file = tempDir.resolve("guides.yml");
            Files.copy(in, file);
            PreparedBook book = GuideBook.prepare("Charidh", GuidesFile.load(file));
            assertEquals(List.of(
                    "Welcome",
                    "Discovery",
                    "Lore",
                    "Regions & Mobs",
                    "Structures",
                    "Death & Inventory",
                    "Navigation",
                    "Crates & Keys",
                    "Protection"
            ), chapterTitles(indexPages(book)));
            String joined = BookLine.join(book.pages().stream().flatMap(List::stream).toList()).replace("\n", "");
            assertTrue(joined.contains("be an explorer, not a grinder"));
            assertTrue(joined.contains("Exploration is how you progress on Charidh."));
            assertTrue(joined.contains("/keys"));
            assertTrue(joined.contains("Claiming land"));
            assertTrue(joined.contains("shipwrecks"));
            assertFalse(joined.contains("<bold>"));
            assertFalse(joined.contains("<dark_green>"));
            assertFalse(joined.contains("{SERVER_NAME}"));
        }
    }

    @Test
    void rejectsABookThatDoesNotFit() {
        StringBuilder body = new StringBuilder();
        for (int line = 0; line < 2000; line++) {
            body.append("line ").append(line).append('\n');
        }
        GuidesException exception = assertThrows(
                GuidesException.class,
                () -> GuideBook.prepare("Charidh", List.of(new GuideChapter("Long", body.toString())))
        );
        assertTrue(exception.getMessage().startsWith("guides.yml has "));
        assertTrue(exception.getMessage().endsWith(" pages"));
        int pages = Integer.parseInt(exception.getMessage().replace("guides.yml has ", "").replace(" pages", ""));
        assertTrue(pages > ExplorationBook.MAX_PAGES);
    }

    private static BookLine link(List<BookLine> lines, String text) {
        for (BookLine line : lines) {
            if (text.equals(line.text())) {
                return line;
            }
        }
        throw new AssertionError(text);
    }

    private static List<BookLine> indexPages(PreparedBook book) {
        List<BookLine> lines = new ArrayList<>();
        for (List<BookLine> page : book.pages()) {
            if (!page.isEmpty() && GuideBook.RETURN_LINK.equals(page.get(page.size() - 1).text())) {
                break;
            }
            lines.addAll(page);
        }
        return lines;
    }

    private static List<String> chapterTitles(List<BookLine> lines) {
        List<String> titles = new ArrayList<>();
        StringBuilder current = null;
        Integer page = null;
        for (BookLine line : lines) {
            if (line.page() == null) {
                continue;
            }
            if (page != null && page.equals(line.page())) {
                current.append(line.text());
                continue;
            }
            if (current != null) {
                titles.add(current.toString());
            }
            current = new StringBuilder(line.text());
            page = line.page();
        }
        if (current != null) {
            titles.add(current.toString());
        }
        return titles;
    }

    private static List<String> texts(List<BookLine> lines) {
        List<String> texts = new ArrayList<>();
        for (BookLine line : lines) {
            texts.add(line.text());
        }
        return texts;
    }
}
