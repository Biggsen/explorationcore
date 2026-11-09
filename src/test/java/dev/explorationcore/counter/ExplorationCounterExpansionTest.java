package dev.explorationcore.counter;

import dev.explorationcore.book.AvailableDiscoveries;
import dev.explorationcore.db.DiscoveryRepository;
import dev.explorationcore.db.StoredDiscovery;
import org.bukkit.OfflinePlayer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ExplorationCounterExpansionTest {
    private static final String PLAYER_UUID = "12345678-1234-1234-1234-123456789abc";

    @TempDir
    Path tempDir;

    @Test
    void totalAvailableIgnoresThePlayerLedgerAndLeavesOtherPlaceholdersAlone() throws Exception {
        Path totals = tempDir.resolve("totals.yml");
        Files.writeString(totals, totalsYaml(1, 2, 0));
        Logger logger = Logger.getLogger("counter-expansion-" + UUID.randomUUID());
        logger.setUseParentHandlers(false);
        AvailableDiscoveries available = new AvailableDiscoveries(totals, logger);
        try (DiscoveryRepository repository = DiscoveryRepository.open(tempDir.resolve("exploration.db"))) {
            ExplorationCounterExpansion expansion = expansion(logger, repository, available);
            OfflinePlayer player = player();

            assertEquals("0", expansion.onRequest(null, "total_discovered"));
            assertEquals("3", expansion.onRequest(null, "total_available"));
            assertNull(expansion.onRequest(player, "votes"));

            repository.insert(new StoredDiscovery(
                    PLAYER_UUID,
                    "verzion",
                    "Lowothra",
                    "overworld",
                    "region",
                    "monkvos",
                    "Monkvos",
                    1,
                    "2026-09-26T17:31:42Z",
                    null
            ));

            assertEquals("1", expansion.onRequest(player, "total_discovered"));
            assertEquals("3", expansion.onRequest(player, "total_available"));
            assertEquals("0", expansion.onRequest(null, "regions_discovered"));

            Files.writeString(totals, totalsYaml(4, 2, 5));
            Files.setLastModifiedTime(totals, FileTime.fromMillis(1_700_000_005_000L));
            assertEquals("11", expansion.onRequest(player, "total_available"));
            assertEquals("1", expansion.onRequest(player, "total_discovered"));
        }
    }

    @Test
    void aMissingTotalsFileDoesNotDisturbDiscoveryCounters() throws Exception {
        Logger logger = Logger.getLogger("counter-expansion-missing-" + UUID.randomUUID());
        logger.setUseParentHandlers(false);
        AvailableDiscoveries available = new AvailableDiscoveries(tempDir.resolve("totals.yml"), logger);
        try (DiscoveryRepository repository = DiscoveryRepository.open(tempDir.resolve("exploration.db"))) {
            ExplorationCounterExpansion expansion = expansion(logger, repository, available);

            assertEquals("", expansion.onRequest(player(), "total_available"));
            assertEquals("0", expansion.onRequest(player(), "total_discovered"));
            assertEquals("", expansion.onRequest(player(), "total_available"));
        }
    }

    private static ExplorationCounterExpansion expansion(
            Logger logger,
            DiscoveryRepository repository,
            AvailableDiscoveries available
    ) {
        return new ExplorationCounterExpansion(
                "test",
                logger,
                repository,
                new TallyCache(),
                DiscoveryCounters.builtin(),
                available
        );
    }

    private static OfflinePlayer player() {
        return (OfflinePlayer) Proxy.newProxyInstance(
                OfflinePlayer.class.getClassLoader(),
                new Class<?>[]{OfflinePlayer.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getUniqueId" -> UUID.fromString(PLAYER_UUID);
                    case "getName" -> "verzion";
                    default -> {
                        Class<?> type = method.getReturnType();
                        if (type.equals(boolean.class)) {
                            yield false;
                        }
                        if (type.equals(int.class)) {
                            yield 0;
                        }
                        if (type.equals(long.class)) {
                            yield 0L;
                        }
                        if (type.equals(double.class)) {
                            yield 0.0d;
                        }
                        if (type.equals(float.class)) {
                            yield 0.0f;
                        }
                        yield null;
                    }
                }
        );
    }

    private static String totalsYaml(int regions, int water, int souls) {
        String soulsLine = souls > 0 ? "  souls:\n    total: " + souls + "\n" : "";
        return """
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
                %s  structures: {}
                nether:
                  regions:
                    total: 0
                  hearts:
                    total: 0
                """.formatted(regions, water, soulsLine);
    }
}
