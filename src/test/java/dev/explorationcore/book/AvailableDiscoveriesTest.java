package dev.explorationcore.book;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AvailableDiscoveriesTest {
    @TempDir
    Path tempDir;

    @Test
    void availableTotalIncludesWaterAndSoulsAndLeavesStructuresOut() {
        ExplorationTotals totals = new ExplorationTotals(
                1,
                2,
                3,
                4,
                5,
                6,
                List.of(new StructureTotal("shipwreck", "Shipwrecks", 100)),
                7,
                8
        );

        assertEquals(36, totals.available());
    }

    @Test
    void placeholderIsAnIntegerAndFollowsTheFile() throws Exception {
        Path file = tempDir.resolve("totals.yml");
        Logger logger = logger();
        write(file, 1, 2, 0, null);
        Files.setLastModifiedTime(file, FileTime.fromMillis(1_700_000_000_000L));
        AvailableDiscoveries available = new AvailableDiscoveries(file, logger);

        assertEquals("3", available.value());
        assertTrue(available.value().matches("[0-9]+"));

        write(file, 9, 2, 0, null);
        Files.setLastModifiedTime(file, FileTime.fromMillis(1_700_000_000_000L));
        assertEquals("3", available.value());

        Files.setLastModifiedTime(file, FileTime.fromMillis(1_700_000_001_000L));
        assertEquals("11", available.value());
        assertTrue(records(logger).isEmpty());
    }

    @Test
    void soulsAndWaterRaiseTheTotalAndASameClockSizeChangeIsSeen() throws Exception {
        Path file = tempDir.resolve("totals.yml");
        Logger logger = logger();
        write(file, 1, 0, 0, null);
        Files.setLastModifiedTime(file, FileTime.fromMillis(1_700_000_000_000L));
        AvailableDiscoveries available = new AvailableDiscoveries(file, logger);

        assertEquals("1", available.value());

        write(file, 1, 4, 7, null);
        Files.setLastModifiedTime(file, FileTime.fromMillis(1_700_000_000_000L));
        assertEquals("12", available.value());
    }

    @Test
    void structureTotalsDoNotRaiseTheAvailableTotal() throws Exception {
        Path file = tempDir.resolve("totals.yml");
        write(file, 2, 3, 0, null);
        AvailableDiscoveries available = new AvailableDiscoveries(file, logger());

        assertEquals("5", available.value());

        write(file, 2, 3, 0, 40);
        Files.setLastModifiedTime(file, FileTime.fromMillis(1_700_000_002_000L));
        assertEquals("5", available.value());
    }

    @Test
    void aMissingOrInvalidFileStaysBlankAndLogsOnce() throws Exception {
        Path file = tempDir.resolve("totals.yml");
        Logger logger = logger();
        AvailableDiscoveries available = new AvailableDiscoveries(file, logger);

        assertEquals("", available.value());
        assertEquals("", available.value());
        assertEquals(1, records(logger).size());
        assertEquals(Level.WARNING, records(logger).get(0).getLevel());
        assertTrue(records(logger).get(0).getMessage().contains("totals.yml is missing"));

        Files.writeString(file, "overworld: []\n");
        Files.setLastModifiedTime(file, FileTime.fromMillis(1_700_000_003_000L));
        assertEquals("", available.value());
        assertEquals("", available.value());
        assertEquals(2, records(logger).size());
        assertTrue(records(logger).get(1).getMessage().contains("overworld must be a map"));

        write(file, 2, 0, 5, null);
        Files.setLastModifiedTime(file, FileTime.fromMillis(1_700_000_004_000L));
        assertEquals("7", available.value());
    }

    private static void write(Path file, int regions, int water, int souls, Integer structures) throws Exception {
        String soulsLine = souls > 0 ? "  souls:\n    total: " + souls + "\n" : "";
        String structureBlock = structures == null
                ? "  structures: {}\n"
                : "  structures:\n    shipwreck:\n      name: Shipwrecks\n      total: " + structures + "\n";
        Files.writeString(file, """
                overworld:
                  regions:
                    total: %d
                  water_regions:
                    total: %d
                  villages:
                    total: 0
                  hearts:
                    total: 0
                  nerves:
                    total: 0
                %s%s
                nether:
                  regions:
                    total: 0
                  hearts:
                    total: 0
                """.formatted(regions, water, soulsLine, structureBlock));
    }

    private static Logger logger() {
        Logger logger = Logger.getLogger("available-discoveries-" + java.util.UUID.randomUUID());
        logger.setUseParentHandlers(false);
        logger.setLevel(Level.ALL);
        logger.addHandler(new RememberingHandler());
        return logger;
    }

    private static List<LogRecord> records(Logger logger) {
        List<LogRecord> records = new ArrayList<>();
        for (Handler handler : logger.getHandlers()) {
            if (handler instanceof RememberingHandler remembering) {
                records.addAll(remembering.published);
            }
        }
        return records;
    }

    private static final class RememberingHandler extends Handler {
        private final List<LogRecord> published = new ArrayList<>();

        @Override
        public void publish(LogRecord record) {
            published.add(record);
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    }
}
