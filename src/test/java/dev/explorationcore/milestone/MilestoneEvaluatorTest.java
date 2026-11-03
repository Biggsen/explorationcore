package dev.explorationcore.milestone;

import dev.explorationcore.counter.DiscoveryCounters;
import dev.explorationcore.db.PlayerTally;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.logging.Logger;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MilestoneEvaluatorTest {
    private static final Logger LOGGER = quietLogger();
    private static final DiscoveryCounters COUNTERS = DiscoveryCounters.builtin();

    @Test
    void firstVillageCrossesOnlyTheVillageMilestone() throws IOException {
        List<Milestone> crossed = crossed(
                shipped(),
                "overworld",
                "village",
                null,
                List.of(new PlayerTally("overworld", "village", null, 1))
        );

        assertEquals(List.of("villages_discovered_1"), ids(crossed));
    }

    @Test
    void fourthCountingDiscoveryCrossesTheTotalMilestone() throws IOException {
        List<Milestone> crossed = crossed(
                shipped(),
                "overworld",
                "heart",
                null,
                List.of(
                        new PlayerTally("overworld", "village", null, 3),
                        new PlayerTally("overworld", "heart", null, 1)
                )
        );

        assertEquals(List.of("hearts_discovered_1", "total_discovered_10"), ids(crossed));
    }

    @Test
    void aHeartAlreadyPastItsThresholdDoesNotCrossAgain() throws IOException {
        List<Milestone> crossed = crossed(
                shipped(),
                "overworld",
                "heart",
                null,
                List.of(new PlayerTally("overworld", "heart", null, 6))
        );

        assertTrue(crossed.isEmpty());
    }

    @Test
    void fifthStructureCrossesStructuresFoundAndLeavesTotalAlone() throws IOException {
        List<Milestone> fifth = crossed(
                shipped(),
                "overworld",
                "structure",
                "shipwreck",
                List.of(new PlayerTally("overworld", "structure", "shipwreck", 5))
        );
        List<Milestone> sixth = crossed(
                shipped(),
                "overworld",
                "structure",
                "shipwreck",
                List.of(new PlayerTally("overworld", "structure", "shipwreck", 6))
        );

        assertEquals(List.of("structures_found_5"), ids(fifth));
        assertTrue(sixth.isEmpty());
    }

    @Test
    void netherNerveCrossesNothing() throws IOException {
        List<Milestone> crossed = crossed(
                shipped(),
                "nether",
                "nerve",
                null,
                List.of(new PlayerTally("nether", "nerve", null, 1))
        );

        assertTrue(crossed.isEmpty());
    }

    @Test
    void netherRegionCanCrossOnlyTheTotalMilestone() throws IOException {
        List<Milestone> crossed = crossed(
                shipped(),
                "nether",
                "region",
                null,
                List.of(
                        new PlayerTally("overworld", "village", null, 3),
                        new PlayerTally("nether", "region", null, 1)
                )
        );

        assertEquals(List.of("total_discovered_10"), ids(crossed));
    }

    @Test
    void oneDiscoveryCrossesOnlyTheThresholdItLandsOn() {
        Milestones milestones = load("""
                milestones:
                  structures_found:
                    - id: structures_found_1
                      threshold: 1
                      display-name: One
                      message: One structure.
                    - id: structures_found_5
                      threshold: 5
                      display-name: Five
                      message: Five structures.
                  shipwrecks_found:
                    - id: shipwrecks_found_5
                      threshold: 5
                      display-name: Shipwrecks
                      message: Five shipwrecks.
                """);

        List<Milestone> first = crossed(
                milestones,
                "overworld",
                "structure",
                "shipwreck",
                List.of(new PlayerTally("overworld", "structure", "shipwreck", 1))
        );
        List<Milestone> fifth = crossed(
                milestones,
                "overworld",
                "structure",
                "shipwreck",
                List.of(new PlayerTally("overworld", "structure", "shipwreck", 5))
        );

        assertEquals(List.of("structures_found_1"), ids(first));
        assertEquals(List.of("structures_found_5", "shipwrecks_found_5"), ids(fifth));
    }

    @Test
    void equalThresholdsOnOneCounterCrossTogetherInListOrder() {
        Milestones milestones = load("""
                milestones:
                  regions_discovered:
                    - id: regions_a
                      threshold: 1
                      display-name: First
                      message: First.
                    - id: regions_b
                      threshold: 1
                      display-name: Second
                      message: Second.
                """);

        List<Milestone> crossed = crossed(
                milestones,
                "overworld",
                "region",
                null,
                List.of(new PlayerTally("overworld", "region", null, 1))
        );

        assertEquals(List.of("regions_a", "regions_b"), ids(crossed));
    }

    @Test
    void unknownStructureCountsTowardStructuresFoundOnly() {
        List<PlayerTally> before = List.of(new PlayerTally("overworld", "village", null, 1));
        List<PlayerTally> after = List.of(
                new PlayerTally("overworld", "village", null, 1),
                new PlayerTally("overworld", "structure", "custom_hut", 1)
        );

        assertEquals(0, COUNTERS.count(before, "structures_found"));
        assertEquals(1, COUNTERS.count(after, "structures_found"));
        for (String name : COUNTERS.names()) {
            if (!"structures_found".equals(name)) {
                assertEquals(COUNTERS.count(before, name), COUNTERS.count(after, name), name);
            }
        }

        Milestones milestones = load("""
                milestones:
                  villages_discovered:
                    - id: villages_discovered_1
                      threshold: 1
                      display-name: Village
                      message: A village.
                  structures_found:
                    - id: structures_found_1
                      threshold: 1
                      display-name: Structure
                      message: A structure.
                  shipwrecks_found:
                    - id: shipwrecks_found_1
                      threshold: 1
                      display-name: Shipwreck
                      message: A shipwreck.
                """);

        assertEquals(
                List.of("structures_found_1"),
                ids(crossed(milestones, "overworld", "structure", "custom_hut", after))
        );
    }

    private static List<String> ids(List<Milestone> milestones) {
        return milestones.stream().map(Milestone::id).toList();
    }

    private static Milestones shipped() throws IOException {
        try (InputStream in = MilestoneEvaluatorTest.class.getClassLoader().getResourceAsStream("milestones.yml")) {
            if (in == null) {
                throw new IOException("missing resource milestones.yml");
            }
            return load(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    private static Milestones load(String yaml) {
        YamlConfiguration configuration = new YamlConfiguration();
        try {
            configuration.loadFromString(yaml);
        } catch (InvalidConfigurationException exception) {
            throw new AssertionError(exception);
        }
        return Milestones.load(configuration, LOGGER, "acb {player} {amount}", COUNTERS);
    }

    private static List<Milestone> crossed(
            Milestones milestones,
            String world,
            String entityType,
            String structureType,
            List<PlayerTally> tallies
    ) {
        return MilestoneEvaluator.crossed(milestones, COUNTERS, world, entityType, structureType, tallies);
    }

    private static Logger quietLogger() {
        Logger logger = Logger.getLogger("milestone-evaluator-test");
        logger.setUseParentHandlers(false);
        return logger;
    }
}
