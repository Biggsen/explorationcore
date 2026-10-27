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
    @Test
    void countsTypesWorldsAndStructureFamilies() {
        List<PlayerTally> tallies = List.of(
                new PlayerTally("overworld", "region", null, 2),
                new PlayerTally("overworld", "village", null, 3),
                new PlayerTally("nether", "village", null, 1),
                new PlayerTally("overworld", "heart", null, 4),
                new PlayerTally("overworld", "nerve", null, 5),
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

        assertEquals(28, DiscoveryCounters.count(tallies, "total_discovered"));
        assertEquals(2, DiscoveryCounters.count(tallies, "regions_discovered"));
        assertEquals(4, DiscoveryCounters.count(tallies, "villages_discovered"));
        assertEquals(4, DiscoveryCounters.count(tallies, "hearts_discovered"));
        assertEquals(5, DiscoveryCounters.count(tallies, "nerves_discovered"));
        assertEquals(6, DiscoveryCounters.count(tallies, "nether_regions_discovered"));
        assertEquals(7, DiscoveryCounters.count(tallies, "nether_hearts_discovered"));
        assertEquals(19, DiscoveryCounters.count(tallies, "structures_found"));
        assertEquals(3, DiscoveryCounters.count(tallies, "ancient_cities_found"));
        assertEquals(1, DiscoveryCounters.count(tallies, "buried_treasures_found"));
        assertEquals(1, DiscoveryCounters.count(tallies, "desert_pyramids_found"));
        assertEquals(1, DiscoveryCounters.count(tallies, "desert_wells_found"));
        assertEquals(1, DiscoveryCounters.count(tallies, "igloos_found"));
        assertEquals(1, DiscoveryCounters.count(tallies, "jungle_temples_found"));
        assertEquals(1, DiscoveryCounters.count(tallies, "ocean_ruins_found"));
        assertEquals(1, DiscoveryCounters.count(tallies, "pillager_outposts_found"));
        assertEquals(1, DiscoveryCounters.count(tallies, "ruined_portals_found"));
        assertEquals(1, DiscoveryCounters.count(tallies, "shipwrecks_found"));
        assertEquals(1, DiscoveryCounters.count(tallies, "trail_ruins_found"));
        assertEquals(1, DiscoveryCounters.count(tallies, "swamp_huts_found"));
        assertEquals(1, DiscoveryCounters.count(tallies, "woodland_mansions_found"));
    }

    @Test
    void playerWithNoRowsIsZeroForEveryCounter() {
        assertEquals(21, DiscoveryCounters.names().size());
        for (String name : DiscoveryCounters.names()) {
            assertTrue(DiscoveryCounters.known(name));
            assertEquals(0, DiscoveryCounters.count(List.of(), name));
        }
    }

    @Test
    void unknownNameIsRejected() {
        assertFalse(DiscoveryCounters.known("votes"));
        assertFalse(DiscoveryCounters.known(null));
        assertThrows(IllegalArgumentException.class, () -> DiscoveryCounters.count(List.of(), "votes"));
    }

    @Test
    void blankStructureTypeCountsOnlyTowardStructuresFound() {
        List<PlayerTally> tallies = new ArrayList<>();
        tallies.add(new PlayerTally("overworld", "structure", null, 2));
        tallies.add(new PlayerTally("overworld", "structure", "", 3));

        assertEquals(5, DiscoveryCounters.count(tallies, "structures_found"));
        assertEquals(0, DiscoveryCounters.count(tallies, "ocean_ruins_found"));
        assertEquals(0, DiscoveryCounters.count(tallies, "total_discovered"));
    }
}
