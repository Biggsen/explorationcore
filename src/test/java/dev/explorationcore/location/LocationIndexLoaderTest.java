package dev.explorationcore.location;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LocationIndexLoaderTest {
    private static final String INDEX =
            """
            # mc-plugin-manager: generator-version=001; plugin=explorationcore
            world:
              warriotos:
                kind: region
                name: Warriotos
                color: '&a'
              cherrybrook:
                kind: village
                name: Cherrybrook
            world_nether:
              ash:
                kind: region
                name: Ash
                color: '&6'
            """;

    @Test
    void keepsGeneratorCommentAndQuotedColor() {
        LocationIndex index = LocationIndexLoader.parse(INDEX);
        assertEquals(new LocationIndex.Entry("region", "Warriotos", "&a"), index.get("world", "warriotos"));
        assertEquals(new LocationIndex.Entry("village", "Cherrybrook", null), index.get("world", "cherrybrook"));
        assertEquals(new LocationIndex.Entry("region", "Ash", "&6"), index.get("world_nether", "ash"));
        assertNull(index.get("world", "missing"));
    }

    @Test
    void missingFileIsAnEmptyIndex(@TempDir Path dir) {
        LocationIndexLoader loader = new LocationIndexLoader(dir.resolve("location").resolve("index.yml"));
        LocationIndex index = loader.current();
        assertEquals("-", LocationResolver.resolve(index, "world", List.of("warriotos"), LocationResolver.CURRENT_REGION));
    }

    @Test
    void reparsesOnlyWhenLastModifiedChanges(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("index.yml");
        Files.writeString(
                file,
                """
                world:
                  warriotos:
                    kind: region
                    name: Warriotos
                    color: '&a'
                """);
        LocationIndexLoader loader = new LocationIndexLoader(file);
        assertEquals("&aWarriotos", region(loader));

        long modified = Files.getLastModifiedTime(file).toMillis();
        Files.writeString(
                file,
                """
                world:
                  warriotos:
                    kind: region
                    name: Renamed
                    color: '&e'
                """);
        Files.setLastModifiedTime(file, java.nio.file.attribute.FileTime.fromMillis(modified));
        assertEquals("&aWarriotos", region(loader));

        Files.setLastModifiedTime(file, java.nio.file.attribute.FileTime.fromMillis(modified + 5_000));
        assertEquals("&eRenamed", region(loader));
    }

    @Test
    void unreadableYamlResolvesAsEmpty(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("index.yml");
        Files.writeString(file, "world: [\n");
        LocationIndexLoader loader = new LocationIndexLoader(file);
        assertEquals("-", region(loader));
    }

    private static String region(LocationIndexLoader loader) {
        return LocationResolver.resolve(loader.current(), "world", List.of("warriotos"), LocationResolver.CURRENT_REGION);
    }
}
