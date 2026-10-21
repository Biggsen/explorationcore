package dev.explorationcore.book;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
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
        for (String page : book.pages()) {
            for (String line : BookText.displayedLines(page)) {
                if (!line.isBlank()) {
                    assertTrue(js.contains(line), line);
                }
            }
        }
        for (String notice : book.notices()) {
            assertTrue(js.contains(notice), notice);
        }
        assertTrue(js.contains("\"pageWidth\": 114"));
        assertTrue(js.contains("\"linesPerPage\": 14"));
        assertTrue(js.contains("\"bold\": true"));
        assertTrue(js.contains("986A27"));
        assertTrue(js.contains("704C2E"));
        assertTrue(js.contains("Exploration Record"));
    }
}
