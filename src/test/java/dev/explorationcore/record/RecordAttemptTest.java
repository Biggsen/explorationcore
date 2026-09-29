package dev.explorationcore.record;

import dev.explorationcore.db.DiscoveryStore;
import dev.explorationcore.db.InsertOutcome;
import dev.explorationcore.db.StoredDiscovery;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

class RecordAttemptTest {
    private static final Instant WHEN = Instant.parse("2026-09-26T17:31:42Z");
    private static final OnlinePlayer VERZION = new OnlinePlayer(
            UUID.fromString("12345678-1234-1234-1234-123456789abc"),
            "verzion"
    );
    private static final OnlinePlayers ONLINE = name ->
            "verzion".equalsIgnoreCase(name) ? Optional.of(VERZION) : Optional.empty();

    @Test
    void recordsANormalizedDiscoveryWithTheDecodedDisplayName() {
        CapturingStore store = new CapturingStore();
        AttemptResult result = RecordAttempt.attempt(
                " Lowothra ",
                args("Overworld", "Region", "sakonotur", "Sakonotur", "2"),
                ONLINE,
                store,
                WHEN
        );

        AttemptResult.Recorded recorded = assertInstanceOf(AttemptResult.Recorded.class, result);
        assertEquals("Recorded verzion -> overworld/region/sakonotur", recorded.logLine());
        StoredDiscovery row = store.row;
        assertEquals("12345678-1234-1234-1234-123456789abc", row.playerUuid());
        assertEquals("verzion", row.playerName());
        assertEquals("Lowothra", row.server());
        assertEquals("overworld", row.world());
        assertEquals("region", row.entityType());
        assertEquals("sakonotur", row.entityId());
        assertEquals("Sakonotur", row.displayName());
        assertEquals(2, row.difficulty());
        assertEquals("2026-09-26T17:31:42Z", row.discoveredAt());
        assertNull(row.structureType());
    }

    @Test
    void storesALowercasedStructureTypeAndOmitsItWhenAbsent() {
        CapturingStore typed = new CapturingStore();
        AttemptResult result = RecordAttempt.attempt(
                "Lowothra",
                withStructureType(args("overworld", "structure", "ocean_ruin_37", "Ocean Ruin", "0"), "Ocean_Ruin"),
                ONLINE,
                typed,
                WHEN
        );
        AttemptResult.Recorded recorded = assertInstanceOf(AttemptResult.Recorded.class, result);
        assertEquals("ocean_ruin", recorded.structureType());
        assertEquals("ocean_ruin", typed.row.structureType());

        CapturingStore untyped = new CapturingStore();
        RecordAttempt.attempt(
                "Lowothra",
                args("overworld", "structure", "inner_core", "Inner Core", "0"),
                ONLINE,
                untyped,
                WHEN
        );
        assertNull(untyped.row.structureType());
    }

    @Test
    void acceptsVillageHeartNerveStructureAndNether() {
        assertInstanceOf(AttemptResult.Recorded.class, attempt("overworld", "village", "widecombe", "Widecombe", "0"));
        assertInstanceOf(AttemptResult.Recorded.class, attempt("overworld", "heart", "heart_of_sakonotur", "Sakonotur", "0"));
        assertInstanceOf(AttemptResult.Recorded.class, attempt("overworld", "nerve", "nerve_of_sakonotur", "Sakonotur", "0"));
        assertInstanceOf(AttemptResult.Recorded.class, attempt("overworld", "structure", "ocean_ruin_37", "Ocean Ruin", "0"));
        assertInstanceOf(AttemptResult.Recorded.class, attempt("nether", "region", "ash_barrow", "Ash Barrow", "4"));
    }

    @Test
    void storesInnerCoreExactly() {
        CapturingStore store = new CapturingStore();
        RecordAttempt.attempt(
                "Lowothra",
                args("overworld", "structure", "inner_core", "Inner Core", "0"),
                ONLINE,
                store,
                WHEN
        );
        assertEquals("Inner Core", store.row.displayName());
        assertEquals("SW5uZXIgQ29yZQ", args("overworld", "structure", "inner_core", "Inner Core", "0")[4]);
    }

    @Test
    void rejectsInvalidIdentityAndWritesNothing() {
        AtomicInteger calls = new AtomicInteger();
        DiscoveryStore store = discovery -> {
            calls.incrementAndGet();
            return InsertOutcome.INSERTED;
        };
        assertRejected("unknown world: world", args("world", "region", "sakonotur", "Sakonotur", "2"), store);
        assertRejected("unknown world: world_nether", args("world_nether", "region", "sakonotur", "Sakonotur", "2"), store);
        assertRejected("unknown entity type: biome", args("overworld", "biome", "sakonotur", "Sakonotur", "2"), store);
        assertRejected("entity id must be a non-empty token without whitespace", args("overworld", "region", "ocean ruin", "Sakonotur", "2"), store);
        assertRejected("entity id must be a non-empty token without whitespace", args("overworld", "region", "", "Sakonotur", "2"), store);
        assertRejected("difficulty must be an integer from 0 through 5", args("overworld", "region", "sakonotur", "Sakonotur", "6"), store);
        assertRejected("difficulty must be an integer from 0 through 5", args("overworld", "region", "sakonotur", "Sakonotur", "-1"), store);
        assertRejected("difficulty must be an integer from 0 through 5", args("overworld", "region", "sakonotur", "Sakonotur", "hard"), store);
        assertRejected("display name token is not valid base64url", new String[]{"verzion", "overworld", "region", "sakonotur", "%%%", "2"}, store);
        assertRejected("player is not online: verzion", args("overworld", "region", "sakonotur", "Sakonotur", "2"), name -> Optional.empty(), store);
        assertRejected("server-name is missing or blank", args("overworld", "region", "sakonotur", "Sakonotur", "2"), ONLINE, store, "  ");
        assertRejected(
                "expected 6 or 7 arguments: <player> <world> <entity-type> <entity-id> <display-name> <difficulty> [structure-type]",
                new String[]{"verzion", "overworld", "region", "sakonotur", DisplayNameCodecTest.encode("Sakonotur")},
                store
        );
        assertRejected(
                "structure_type is only recorded for structures",
                withStructureType(args("overworld", "region", "sakonotur", "Sakonotur", "2"), "ocean_ruin"),
                store
        );
        assertRejected(
                "structure_type must be a non-empty token without whitespace",
                withStructureType(args("overworld", "structure", "ocean_ruin_37", "Ocean Ruin", "0"), "ocean ruin"),
                store
        );
        assertRejected(
                "structure_type must be a non-empty token without whitespace",
                withStructureType(args("overworld", "structure", "ocean_ruin_37", "Ocean Ruin", "0"), " "),
                store
        );
        assertEquals(0, calls.get());
    }

    @Test
    void containsADatabaseFailure() {
        DiscoveryStore broken = discovery -> {
            throw new SQLException("disk full");
        };
        AttemptResult result = RecordAttempt.attempt(
                "Lowothra",
                args("overworld", "region", "sakonotur", "Sakonotur", "2"),
                ONLINE,
                broken,
                WHEN
        );
        AttemptResult.Failed failed = assertInstanceOf(AttemptResult.Failed.class, result);
        assertEquals(
                "Persistence failed: player=verzion world=overworld entity_type=region entity_id=sakonotur",
                failed.logLine()
        );
        assertInstanceOf(SQLException.class, failed.cause());
    }

    @Test
    void containsAMissingDatabaseWithoutThrowing() {
        AttemptResult result = RecordAttempt.attempt(
                "Lowothra",
                args("overworld", "region", "sakonotur", "Sakonotur", "2"),
                ONLINE,
                null,
                WHEN
        );
        AttemptResult.Failed failed = assertInstanceOf(AttemptResult.Failed.class, result);
        assertNull(failed.cause());
        assertEquals(
                "Persistence failed: player=verzion world=overworld entity_type=region entity_id=sakonotur",
                failed.logLine()
        );
    }

    private static AttemptResult attempt(String world, String type, String id, String displayName, String difficulty) {
        return RecordAttempt.attempt(
                "Lowothra",
                new String[]{"verzion", world, type, id, DisplayNameCodecTest.encode(displayName), difficulty},
                ONLINE,
                discovery -> InsertOutcome.INSERTED,
                WHEN
        );
    }

    private static void assertRejected(String reason, String[] args, DiscoveryStore store) {
        assertRejected(reason, args, ONLINE, store, "Lowothra");
    }

    private static void assertRejected(String reason, String[] args, OnlinePlayers players, DiscoveryStore store) {
        assertRejected(reason, args, players, store, "Lowothra");
    }

    private static void assertRejected(
            String reason,
            String[] args,
            OnlinePlayers players,
            DiscoveryStore store,
            String serverName
    ) {
        AttemptResult result = RecordAttempt.attempt(serverName, args, players, store, WHEN);
        AttemptResult.Rejected rejected = assertInstanceOf(AttemptResult.Rejected.class, result);
        assertEquals(reason, rejected.reason());
    }

    private static String[] args(String world, String type, String id, String displayName, String difficulty) {
        return new String[]{"verzion", world, type, id, DisplayNameCodecTest.encode(displayName), difficulty};
    }

    private static String[] withStructureType(String[] args, String structureType) {
        String[] withType = new String[args.length + 1];
        System.arraycopy(args, 0, withType, 0, args.length);
        withType[args.length] = structureType;
        return withType;
    }

    private static final class CapturingStore implements DiscoveryStore {
        private StoredDiscovery row;

        @Override
        public InsertOutcome insert(StoredDiscovery discovery) {
            row = discovery;
            return InsertOutcome.INSERTED;
        }
    }
}
