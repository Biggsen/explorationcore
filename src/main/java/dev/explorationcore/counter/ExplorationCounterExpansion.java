package dev.explorationcore.counter;

import dev.explorationcore.db.DiscoveryRepository;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

import java.util.logging.Logger;

/**
 * PlaceholderAPI identifier {@code explorationcore}. Answers discovery counters
 * such as {@code %explorationcore_total_discovered%} from the ledger.
 */
public final class ExplorationCounterExpansion extends PlaceholderExpansion {
    private final String version;
    private final Logger logger;
    private final DiscoveryRepository repository;
    private final TallyCache cache;

    public ExplorationCounterExpansion(
            String version,
            Logger logger,
            DiscoveryRepository repository,
            TallyCache cache
    ) {
        this.version = version;
        this.logger = logger;
        this.repository = repository;
        this.cache = cache;
    }

    @Override
    public String getIdentifier() {
        return "explorationcore";
    }

    @Override
    public String getAuthor() {
        return "exploration";
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        String playerUuid = null;
        if (player != null && player.getUniqueId() != null) {
            playerUuid = player.getUniqueId().toString();
        }
        return CounterRequest.resolve(params, playerUuid, repository, cache, logger);
    }
}
