package dev.explorationcore.db;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PlayerTalliesTest {
    private static final String PLAYER_UUID = "12345678-1234-1234-1234-123456789abc";

    @TempDir
    Path tempDir;

    @Test
    void talliesCountRowsForThePlayerAcrossServerNames() throws Exception {
        try (DiscoveryRepository repository = DiscoveryRepository.open(tempDir.resolve("exploration.db"))) {
            repository.insert(row("overworld", "region", "siluria", "Lowothra"));
            repository.insert(row("overworld", "region", "outmarsh", "OldName"));
            repository.insert(row("overworld", "structure", "cache", "Lowothra", "ocean_ruin"));
            repository.insert(row("overworld", "structure", "wreck", "Lowothra", "ocean_ruin"));
            repository.insert(row("overworld", "structure", "blank", "Lowothra", ""));
            repository.insert(row("overworld", "structure", "spaces", "Lowothra", "   "));
            repository.insert(row("nether", "heart", "heart_of_ashen_veins", "Lowothra"));
            repository.insert(new StoredDiscovery(
                    "99999999-9999-9999-9999-999999999999",
                    "someone",
                    "Lowothra",
                    "overworld",
                    "region",
                    "other",
                    "Other",
                    0,
                    "2026-09-26T17:31:42Z",
                    null
            ));

            List<PlayerTally> tallies = repository.talliesFor(PLAYER_UUID);

            assertEquals(2, count(tallies, "overworld", "region"));
            assertEquals(2, countStructure(tallies, "ocean_ruin"));
            assertEquals(1, count(tallies, "nether", "heart"));
            assertEquals(0, count(tallies, "nether", "region"));
            long blank = 0;
            for (PlayerTally tally : tallies) {
                if ("structure".equals(tally.entityType()) && tally.structureType() == null) {
                    blank += tally.count();
                    assertNull(tally.structureType());
                }
            }
            assertEquals(2, blank);
        }
    }

    private static long count(List<PlayerTally> tallies, String world, String entityType) {
        long total = 0;
        for (PlayerTally tally : tallies) {
            if (world.equals(tally.world()) && entityType.equals(tally.entityType())) {
                total += tally.count();
            }
        }
        return total;
    }

    private static long countStructure(List<PlayerTally> tallies, String structureType) {
        long total = 0;
        for (PlayerTally tally : tallies) {
            if ("overworld".equals(tally.world())
                    && "structure".equals(tally.entityType())
                    && structureType.equals(tally.structureType())) {
                total += tally.count();
            }
        }
        return total;
    }

    private static StoredDiscovery row(String world, String type, String id, String server) {
        return row(world, type, id, server, null);
    }

    private static StoredDiscovery row(String world, String type, String id, String server, String structureType) {
        return new StoredDiscovery(
                PLAYER_UUID,
                "verzion",
                server,
                world,
                type,
                id,
                id,
                0,
                "2026-09-26T17:31:42Z",
                structureType
        );
    }
}
