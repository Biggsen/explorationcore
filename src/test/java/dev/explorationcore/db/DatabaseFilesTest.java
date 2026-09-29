package dev.explorationcore.db;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DatabaseFilesTest {
    @Test
    void resolvesRelativePathsAgainstThePluginDataFolder() {
        Path dataFolder = Path.of("C:/servers/plugins/ExplorationCore");
        assertEquals(
                dataFolder.resolve("exploration.db").normalize(),
                DatabaseFiles.resolve(dataFolder, "exploration.db")
        );
        assertEquals(
                dataFolder.resolve("exploration.db").normalize(),
                DatabaseFiles.resolve(dataFolder, "  ")
        );
    }

    @Test
    void keepsAbsolutePaths() {
        Path dataFolder = Path.of("C:/servers/plugins/ExplorationCore");
        Path absolute = Path.of("D:/ledger/exploration.db");
        assertEquals(absolute.normalize(), DatabaseFiles.resolve(dataFolder, absolute.toString()));
    }
}
