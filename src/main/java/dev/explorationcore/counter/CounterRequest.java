package dev.explorationcore.counter;

import dev.explorationcore.db.DiscoveryRepository;
import dev.explorationcore.db.PlayerTally;

import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class CounterRequest {
    private CounterRequest() {
    }

    public static String resolve(
            String params,
            String playerUuid,
            DiscoveryRepository repository,
            TallyCache cache,
            DiscoveryCounters counters,
            Logger logger
    ) {
        if (!counters.known(params)) {
            return null;
        }
        if (playerUuid == null || playerUuid.isBlank()) {
            return "0";
        }
        if (repository == null) {
            logger.severe("Discovery counters are unavailable because the SQLite database is not open.");
            return "";
        }
        try {
            List<PlayerTally> tallies = cache.get(playerUuid, repository::talliesFor);
            return Long.toString(counters.count(tallies, params));
        } catch (SQLException exception) {
            logger.log(Level.SEVERE, "Failed to count discoveries for " + playerUuid, exception);
            return "";
        }
    }
}
