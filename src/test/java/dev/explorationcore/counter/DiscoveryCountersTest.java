package dev.explorationcore.counter;

import dev.explorationcore.db.PlayerTally;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiscoveryCountersTest {
    private static final DiscoveryCounters COUNTERS = DiscoveryCounters.builtin();

    @Test
    void countsTypesWorldsAndStructureFamilies() {
        List<PlayerTally> tallies = List.of(
                new PlayerTally("overworld", "region", null, 2),
                new PlayerTally("overworld", "water", null, 8),
                new PlayerTally("nether", "water", null, 3),
                new PlayerTally("overworld", "village", null, 3),
                new PlayerTally("nether", "village", null, 1),
                new PlayerTally("overworld", "heart", null, 4),
                new PlayerTally("overworld", "nerve", null, 5),
                new PlayerTally("overworld", "soul", null, 2),
                new PlayerTally("nether", "soul", null, 6),
                new PlayerTally("nether", "nerve", null, 9),
                new PlayerTally("nether", "region", null, 6),
                new PlayerTally("nether", "heart", null, 7),
                new PlayerTally("overworld", "structure", "ancient_city", 2),
                new PlayerTally("nether", "structure", "ancient_city", 1),
                new PlayerTally("overworld", "structure", null, 4),
                new PlayerTally("overworld", "structure", "buried_treasure", 1),
                new PlayerTally("overworld", "structure", "desert_pyramid", 1),
                new PlayerTally("overworld", "structure", "desert_well", 1),
                new PlayerTally("overworld", "structure", "igloo", 1),
                new PlayerTally("overworld", "structure", "jungle_temple", 1),
                new PlayerTally("overworld", "structure", "ocean_ruin", 1),
                new PlayerTally("overworld", "structure", "pillager_outpost", 1),
                new PlayerTally("overworld", "structure", "ruined_portal", 1),
                new PlayerTally("overworld", "structure", "shipwreck", 1),
                new PlayerTally("overworld", "structure", "trail_ruins", 1),
                new PlayerTally("overworld", "structure", "swamp_hut", 1),
                new PlayerTally("overworld", "structure", "woodland_mansion", 1)
        );

        assertEquals(38, COUNTERS.count(tallies, "total_discovered"));
        assertEquals(2, COUNTERS.count(tallies, "regions_discovered"));
        assertEquals(8, COUNTERS.count(tallies, "water_regions_discovered"));
        assertEquals(4, COUNTERS.count(tallies, "villages_discovered"));
        assertEquals(4, COUNTERS.count(tallies, "hearts_discovered"));
        assertEquals(5, COUNTERS.count(tallies, "nerves_discovered"));
        assertEquals(2, COUNTERS.count(tallies, "souls_discovered"));
        assertEquals(6, COUNTERS.count(tallies, "nether_regions_discovered"));
        assertEquals(7, COUNTERS.count(tallies, "nether_hearts_discovered"));
        assertEquals(19, COUNTERS.count(tallies, "structures_found"));
        assertEquals(3, COUNTERS.count(tallies, "ancient_cities_found"));
        assertEquals(1, COUNTERS.count(tallies, "buried_treasures_found"));
        assertEquals(1, COUNTERS.count(tallies, "desert_pyramids_found"));
        assertEquals(1, COUNTERS.count(tallies, "desert_wells_found"));
        assertEquals(1, COUNTERS.count(tallies, "igloos_found"));
        assertEquals(1, COUNTERS.count(tallies, "jungle_temples_found"));
        assertEquals(1, COUNTERS.count(tallies, "ocean_ruins_found"));
        assertEquals(1, COUNTERS.count(tallies, "pillager_outposts_found"));
        assertEquals(1, COUNTERS.count(tallies, "ruined_portals_found"));
        assertEquals(1, COUNTERS.count(tallies, "shipwrecks_found"));
        assertEquals(1, COUNTERS.count(tallies, "trail_ruins_found"));
        assertEquals(1, COUNTERS.count(tallies, "swamp_huts_found"));
        assertEquals(1, COUNTERS.count(tallies, "woodland_mansions_found"));
    }

    @Test
    void playerWithNoRowsIsZeroForEveryCounter() {
        assertEquals(23, COUNTERS.names().size());
        for (String name : COUNTERS.names()) {
            assertTrue(COUNTERS.known(name));
            assertEquals(0, COUNTERS.count(List.of(), name));
        }
    }

    @Test
    void aDiscoveryMovesItsCategoryAndTotalWhenTheLedgerCountsIt() {
        assertEquals(List.of("regions_discovered", "total_discovered"), COUNTERS.affected("overworld", "region", null));
        assertEquals(
                List.of("water_regions_discovered", "total_discovered"),
                COUNTERS.affected("overworld", "water", null)
        );
        assertEquals(List.of(), COUNTERS.affected("nether", "water", null));
        assertEquals(List.of("villages_discovered", "total_discovered"), COUNTERS.affected("nether", "village", null));
        assertEquals(List.of("hearts_discovered", "total_discovered"), COUNTERS.affected("overworld", "heart", null));
        assertEquals(List.of("nerves_discovered", "total_discovered"), COUNTERS.affected("overworld", "nerve", null));
        assertEquals(List.of("souls_discovered", "total_discovered"), COUNTERS.affected("overworld", "soul", null));
        assertEquals(List.of(), COUNTERS.affected("nether", "soul", null));
        assertEquals(
                List.of("nether_regions_discovered", "total_discovered"),
                COUNTERS.affected("nether", "region", null)
        );
        assertEquals(
                List.of("nether_hearts_discovered", "total_discovered"),
                COUNTERS.affected("nether", "heart", null)
        );
        assertEquals(List.of(), COUNTERS.affected("nether", "nerve", null));
        assertEquals(
                List.of("structures_found", "shipwrecks_found"),
                COUNTERS.affected("overworld", "structure", "shipwreck")
        );
        assertEquals(List.of("structures_found"), COUNTERS.affected("nether", "structure", null));
        assertEquals(List.of("structures_found"), COUNTERS.affected("overworld", "structure", "custom_hut"));
    }

    @Test
    void unknownNameIsRejected() {
        assertFalse(COUNTERS.known("votes"));
        assertFalse(COUNTERS.known(null));
        assertThrows(IllegalArgumentException.class, () -> COUNTERS.count(List.of(), "votes"));
    }

    @Test
    void blankStructureTypeCountsOnlyTowardStructuresFound() {
        List<PlayerTally> tallies = new ArrayList<>();
        tallies.add(new PlayerTally("overworld", "structure", null, 2));
        tallies.add(new PlayerTally("overworld", "structure", "", 3));

        assertEquals(5, COUNTERS.count(tallies, "structures_found"));
        assertEquals(0, COUNTERS.count(tallies, "ocean_ruins_found"));
        assertEquals(0, COUNTERS.count(tallies, "total_discovered"));
    }
}
