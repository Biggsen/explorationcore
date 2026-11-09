package dev.explorationcore.book;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuidesFileTest {
    @TempDir
    Path tempDir;

    @Test
    void readsChaptersInFileOrder() throws Exception {
        Path file = write("""
                chapters:
                  - title: "  Lore  "
                    text: |-
                      Each region carries its own record.
                  - title: "Death & keep-inventory"
                    text: ""
                """);

        var chapters = GuidesFile.load(file);

        assertEquals(2, chapters.size());
        assertEquals("Lore", chapters.get(0).title());
        assertEquals("Each region carries its own record.", chapters.get(0).text());
        assertEquals("Death & keep-inventory", chapters.get(1).title());
        assertEquals("", chapters.get(1).text());
        assertTrue(chapters.stream().noneMatch(chapter -> chapter.title().equals("Intro")));
    }

    @Test
    void rejectsAMissingFile() {
        GuidesException exception = assertThrows(
                GuidesException.class,
                () -> GuidesFile.load(tempDir.resolve("guides.yml"))
        );
        assertEquals("guides.yml is missing", exception.getMessage());
    }

    @Test
    void rejectsMalformedYaml() throws Exception {
        Path file = write("chapters: [");
        GuidesException exception = assertThrows(GuidesException.class, () -> GuidesFile.load(file));
        assertEquals("guides.yml is malformed", exception.getMessage());
    }

    @Test
    void rejectsABlankTitle() throws Exception {
        Path file = write("""
                chapters:
                  - title: " "
                    text: hello
                """);
        GuidesException exception = assertThrows(GuidesException.class, () -> GuidesFile.load(file));
        assertEquals("chapters[0].title must be a non-blank string", exception.getMessage());
    }

    @Test
    void rejectsAMissingText() throws Exception {
        Path file = write("""
                chapters:
                  - title: Lore
                """);
        GuidesException exception = assertThrows(GuidesException.class, () -> GuidesFile.load(file));
        assertEquals("chapters[0].text is missing", exception.getMessage());
    }

    private Path write(String yaml) throws Exception {
        Path file = tempDir.resolve("guides.yml");
        Files.writeString(file, yaml);
        return file;
    }
}
