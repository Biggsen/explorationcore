package dev.explorationcore.counter;

import dev.explorationcore.db.DiscoveryRepository;
import dev.explorationcore.db.StoredDiscovery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CounterRequestTest {
    private static final String PLAYER_UUID = "12345678-1234-1234-1234-123456789abc";

    @TempDir
    Path tempDir;

    @Test
    void missingPlayerAndEmptyLedgerAnswerZero() throws Exception {
        Logger logger = logger();
        try (DiscoveryRepository repository = DiscoveryRepository.open(tempDir.resolve("exploration.db"))) {
            assertEquals("0", CounterRequest.resolve(
                    "total_discovered",
                    null,
                    repository,
                    new TallyCache(),
                    logger
            ));
            assertEquals("0", CounterRequest.resolve(
                    "desert_pyramids_found",
                    PLAYER_UUID,
                    repository,
                    new TallyCache(),
                    logger
            ));
            assertNull(CounterRequest.resolve("votes", PLAYER_UUID, repository, new TallyCache(), logger));
            assertTrue(records(logger).isEmpty());
        }
    }

    @Test
    void countedRowsAreReturnedAsText() throws Exception {
        Logger logger = logger();
        try (DiscoveryRepository repository = DiscoveryRepository.open(tempDir.resolve("counted.db"))) {
            repository.insert(new StoredDiscovery(
                    PLAYER_UUID,
                    "verzion",
                    "Lowothra",
                    "nether",
                    "region",
                    "ash",
                    "Ash",
                    4,
                    "2026-09-26T17:31:42Z",
                    null
            ));
            assertEquals("1", CounterRequest.resolve(
                    "nether_regions_discovered",
                    PLAYER_UUID,
                    repository,
                    new TallyCache(),
                    logger
            ));
            assertEquals("1", CounterRequest.resolve(
                    "total_discovered",
                    PLAYER_UUID,
                    repository,
                    new TallyCache(),
                    logger
            ));
            assertTrue(records(logger).isEmpty());
        }
    }

    @Test
    void nullRepositoryReturnsEmptyAndLogs() {
        Logger logger = logger();
        assertEquals("", CounterRequest.resolve(
                "total_discovered",
                PLAYER_UUID,
                null,
                new TallyCache(),
                logger
        ));
        LogRecord record = records(logger).get(0);
        assertEquals(Level.SEVERE, record.getLevel());
        assertTrue(record.getMessage().contains("not open"));
    }

    @Test
    void closedRepositoryReturnsEmptyAndLogs() throws Exception {
        Logger logger = logger();
        DiscoveryRepository repository = DiscoveryRepository.open(tempDir.resolve("closed.db"));
        repository.close();
        TallyCache cache = new TallyCache();

        assertEquals("", CounterRequest.resolve("regions_discovered", PLAYER_UUID, repository, cache, logger));
        assertEquals("", CounterRequest.resolve("regions_discovered", PLAYER_UUID, repository, cache, logger));

        List<LogRecord> records = records(logger);
        assertEquals(2, records.size());
        assertEquals(Level.SEVERE, records.get(0).getLevel());
        assertTrue(records.get(0).getThrown() instanceof java.sql.SQLException);
    }

    private static Logger logger() {
        Logger logger = Logger.getLogger("counter-request-" + java.util.UUID.randomUUID());
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
