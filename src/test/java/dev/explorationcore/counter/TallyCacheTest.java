package dev.explorationcore.counter;

import dev.explorationcore.db.DiscoveryRepository;
import dev.explorationcore.db.InsertOutcome;
import dev.explorationcore.db.PlayerTally;
import dev.explorationcore.db.StoredDiscovery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TallyCacheTest {
    private static final String PLAYER_UUID = "12345678-1234-1234-1234-123456789abc";

    @TempDir
    Path tempDir;

    @Test
    void secondReadInsideFiveSecondsReusesTheList() throws Exception {
        long[] now = {1_000};
        TallyCache cache = new TallyCache(() -> now[0]);
        List<PlayerTally> first = List.of(new PlayerTally("overworld", "region", null, 1));
        List<PlayerTally> later = List.of(new PlayerTally("overworld", "region", null, 4));
        AtomicInteger loads = new AtomicInteger();
        TallyCache.TallyLoader loader = uuid -> {
            loads.incrementAndGet();
            return loads.get() == 1 ? first : later;
        };

        List<PlayerTally> cached = cache.get(PLAYER_UUID, loader);
        assertSame(cached, cache.get(PLAYER_UUID, loader));
        assertEquals(1, loads.get());

        now[0] += TallyCache.TTL_NANOS;
        assertEquals(4, cache.get(PLAYER_UUID, loader).get(0).count());
        assertEquals(2, loads.get());
        assertEquals(1, cached.get(0).count());
    }

    @Test
    void failedLoadIsNotStored() {
        long[] now = {0};
        TallyCache cache = new TallyCache(() -> now[0]);
        AtomicInteger loads = new AtomicInteger();
        TallyCache.TallyLoader loader = uuid -> {
            loads.incrementAndGet();
            throw new SQLException("closed");
        };

        assertThrows(SQLException.class, () -> cache.get(PLAYER_UUID, loader));
        assertThrows(SQLException.class, () -> cache.get(PLAYER_UUID, loader));
        assertEquals(2, loads.get());
    }

    @Test
    void duplicateStaysOneRowAndANewDiscoveryWaitsForTheTtl() throws Exception {
        long[] now = {1_000};
        TallyCache cache = new TallyCache(() -> now[0]);
        try (DiscoveryRepository repository = DiscoveryRepository.open(tempDir.resolve("exploration.db"))) {
            assertEquals(InsertOutcome.INSERTED, repository.insert(region("sakonotur")));
            AtomicInteger loads = new AtomicInteger();
            TallyCache.TallyLoader loader = uuid -> {
                loads.incrementAndGet();
                return repository.talliesFor(uuid);
            };

            assertEquals(1, regions(cache.get(PLAYER_UUID, loader)));
            assertEquals(1, regions(cache.get(PLAYER_UUID, loader)));
            assertEquals(1, loads.get());

            assertEquals(InsertOutcome.DUPLICATE, repository.insert(region("sakonotur")));
            now[0] += TallyCache.TTL_NANOS;
            assertEquals(1, regions(cache.get(PLAYER_UUID, loader)));
            assertEquals(2, loads.get());

            assertEquals(InsertOutcome.INSERTED, repository.insert(region("auronia")));
            assertEquals(1, regions(cache.get(PLAYER_UUID, loader)));
            assertEquals(2, loads.get());

            now[0] += TallyCache.TTL_NANOS;
            assertEquals(2, regions(cache.get(PLAYER_UUID, loader)));
            assertEquals(3, loads.get());
        }
    }

    private static long regions(List<PlayerTally> tallies) {
        return DiscoveryCounters.builtin().count(tallies, "regions_discovered");
    }

    private static StoredDiscovery region(String id) {
        return new StoredDiscovery(
                PLAYER_UUID,
                "verzion",
                "Lowothra",
                "overworld",
                "region",
                id,
                id,
                1,
                "2026-09-26T17:31:42Z",
                null
        );
    }
}
