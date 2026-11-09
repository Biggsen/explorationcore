package dev.explorationcore.book;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BookPreviewDumpTest {
    @Test
    void previewFileIsThePreparedBook(@TempDir Path dir) throws Exception {
        Path bookJs = dir.resolve("book.js");
        BookPreviewDump.writePreview(bookJs);
        String js = Files.readString(bookJs);

        assertTrue(js.startsWith("window.EXPLORATION_BOOK = "));
        PreparedBook book = BookPreviewDump.sample();
        assertFalse(book.pages().isEmpty());
        boolean linked = false;
        for (List<BookLine> page : book.pages()) {
            for (BookLine line : page) {
                for (String displayed : BookText.displayedLines(line.text())) {
                    if (!displayed.isBlank()) {
                        assertTrue(js.contains(displayed), displayed);
                    }
                }
                if (line.page() != null) {
                    linked = true;
                    assertTrue(js.contains("\"page\": " + line.page() + " "), line.text());
                }
            }
        }
        assertTrue(linked);
        for (String notice : book.notices()) {
            assertTrue(js.contains(notice), notice);
        }
        assertTrue(js.contains("\"pageWidth\": 114"));
        assertTrue(js.contains("\"linesPerPage\": 14"));
        assertTrue(js.contains("\"bold\": true"));
        assertTrue(js.contains("986A27"));
        assertTrue(js.contains("704C2E"));
        assertTrue(js.contains("\"linkColor\": \"365B83\""));
        assertTrue(js.contains("Exploration Record"));
    }

    @Test
    void guidesPreviewBoldsChapterHeadings() throws Exception {
        PreparedBook book = GuideBook.prepare(
                "Lowothra",
                GuidesFile.load(Path.of("src/main/resources/guides.yml"))
        );
        String js = BookPreviewDump.render(book, GuideBook.TITLE, "Lowothra");

        assertTrue(js.contains("\"title\": \"Guides\""));
        assertTrue(js.contains("\"text\": \"Lore\", \"bold\": true"));
        assertTrue(js.contains("\"text\": \"< Guides\", \"color\": \"704C2E\", \"page\": 1"));
        assertTrue(js.contains("⌖ Welcome"));
        assertTrue(js.contains("Lowothra"));
    }

    @Test
    void emptyScenarioHasNoPageTargets() {
        PreparedBook book = JournalFixtures.book("Lowothra", "empty");
        for (List<BookLine> page : book.pages()) {
            for (BookLine line : page) {
                assertNull(line.page());
            }
        }
    }
}
