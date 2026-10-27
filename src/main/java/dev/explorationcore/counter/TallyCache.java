package dev.explorationcore.counter;

import dev.explorationcore.db.PlayerTally;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.LongSupplier;

public final class TallyCache {
    public static final long TTL_NANOS = 5_000_000_000L;

    private final LongSupplier now;
    private final Object lock = new Object();
    private final Map<String, Entry> entries = new HashMap<>();

    public TallyCache() {
        this(System::nanoTime);
    }

    public TallyCache(LongSupplier now) {
        this.now = now;
    }

    public List<PlayerTally> get(String playerUuid, TallyLoader loader) throws SQLException {
        long time = now.getAsLong();
        synchronized (lock) {
            Entry entry = entries.get(playerUuid);
            if (entry != null && time - entry.storedAtNanos < TTL_NANOS) {
                return entry.tallies;
            }
        }
        List<PlayerTally> loaded = List.copyOf(loader.load(playerUuid));
        synchronized (lock) {
            entries.put(playerUuid, new Entry(now.getAsLong(), loaded));
        }
        return loaded;
    }

    @FunctionalInterface
    public interface TallyLoader {
        List<PlayerTally> load(String playerUuid) throws SQLException;
    }

    private record Entry(long storedAtNanos, List<PlayerTally> tallies) {
    }
}
