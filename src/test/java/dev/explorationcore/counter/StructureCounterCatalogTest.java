package dev.explorationcore.counter;

import dev.explorationcore.db.PlayerTally;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StructureCounterCatalogTest {
    @TempDir
    Path tempDir;

    @Test
    void shippedCatalogMatchesTheBuiltinFamilies() throws Exception {
        Logger logger = logger();
        try (InputStream in = StructureCounterCatalogTest.class.getClassLoader().getResourceAsStream("structure-counters.yml")) {
            assertTrue(in != null);
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                DiscoveryCounters loaded = DiscoveryCounters.load(reader, logger);
                DiscoveryCounters builtin = DiscoveryCounters.builtin();

                assertTrue(loaded.accepted());
                assertEquals(13, loaded.familyCount());
                assertEquals(builtin.names(), loaded.names());
                assertTrue(records(logger).isEmpty());
            }
        }
    }

    @Test
    void anAddedFamilyCountsAndAffectsThatCounter() {
        Logger logger = logger();
        DiscoveryCounters counters = DiscoveryCounters.load(new StringReader("""
                structures:
                  ancient_city: ancient_cities_found
                  ocean_monument: ocean_monuments_found
                """), logger);
        List<PlayerTally> tallies = List.of(
                new PlayerTally("overworld", "structure", "ocean_monument", 2),
                new PlayerTally("overworld", "structure", "ancient_city", 1)
        );

        assertTrue(counters.accepted());
        assertTrue(records(logger).isEmpty());
        assertEquals(2, counters.familyCount());
        assertTrue(counters.known("ocean_monuments_found"));
        assertEquals(2, counters.count(tallies, "ocean_monuments_found"));
        assertEquals(3, counters.count(tallies, "structures_found"));
        assertEquals(
                List.of("structures_found", "ocean_monuments_found"),
                counters.affected("overworld", "structure", "ocean_monument")
        );
    }

    @Test
    void emptyCatalogIsAccepted() {
        Logger logger = logger();
        DiscoveryCounters counters = DiscoveryCounters.load(new StringReader("""
                structures: {}
                """), logger);

        assertTrue(counters.accepted());
        assertEquals(0, counters.familyCount());
        assertEquals(9, counters.names().size());
        assertTrue(counters.known("structures_found"));
        assertFalse(counters.known("shipwrecks_found"));
        assertTrue(records(logger).isEmpty());
    }

    @Test
    void invalidCatalogsAreNamedAndLoadNoFamilies() throws Exception {
        assertRejected("structures: [", "structure-counters.yml is malformed.");
        assertRejected("catalog: {}", "structure-counters.yml has no structures section.");
        assertRejected("""
                structures:
                  Ocean_Monument: ocean_monuments_found
                """, "structures.Ocean_Monument is not a lowercase token.");
        assertRejected("""
                structures:
                  ocean_monument: Ocean_Monuments
                """, "structures.ocean_monument counter Ocean_Monuments is not a lowercase token.");
        assertRejected("""
                structures:
                  ocean_monument: regions_discovered
                """, "structures.ocean_monument counter regions_discovered is a main discovery counter.");
        assertRejected("""
                structures:
                  ocean_monument: monuments_found
                  ocean_ruin: monuments_found
                """, "structures.ocean_ruin counter monuments_found is duplicated.");
        assertRejected(tempDir.resolve("absent.yml"), "structure-counters.yml is missing.");
    }

    private void assertRejected(String yaml, String message) {
        Logger logger = logger();
        DiscoveryCounters counters = DiscoveryCounters.load(new StringReader(yaml), logger);
        assertRejected(counters, logger, message);
    }

    private void assertRejected(Path file, String message) {
        Logger logger = logger();
        DiscoveryCounters counters = DiscoveryCounters.load(file, logger);
        assertRejected(counters, logger, message);
    }

    private static void assertRejected(DiscoveryCounters counters, Logger logger, String message) {
        assertFalse(counters.accepted());
        assertEquals(0, counters.familyCount());
        assertEquals(9, counters.names().size());
        assertTrue(counters.known("total_discovered"));
        List<LogRecord> records = records(logger);
        assertEquals(1, records.size());
        assertEquals(Level.SEVERE, records.get(0).getLevel());
        assertEquals(message, records.get(0).getMessage());
    }

    private static Logger logger() {
        Logger logger = Logger.getLogger("structure-counters-" + java.util.UUID.randomUUID());
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
