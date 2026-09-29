package dev.explorationcore.db;

import dev.explorationcore.record.AttemptResult;
import dev.explorationcore.record.OnlinePlayer;
import dev.explorationcore.record.RecordAttempt;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

class DiscoveryRepositoryTest {
    private static final String PLAYER_UUID = "12345678-1234-1234-1234-123456789abc";

    @TempDir
    Path tempDir;

    @Test
    void firstWriteWinsForTheEntireRow() throws Exception {
        try (DiscoveryRepository repository = DiscoveryRepository.open(tempDir.resolve("exploration.db"))) {
            StoredDiscovery original = row("overworld", "region", "sakonotur", "Sakonotur", 2, "2026-09-26T17:31:42Z");
            assertEquals(InsertOutcome.INSERTED, repository.insert(original));
            assertEquals(InsertOutcome.DUPLICATE, repository.insert(
                    row("overworld", "region", "sakonotur", "Renamed", 5, "2026-09-27T00:00:00Z", "Charidh", "verzion_later")
            ));

            StoredDiscovery stored = repository.find(PLAYER_UUID, "overworld", "region", "sakonotur");
            assertEquals(original, stored);
            assertEquals(1L, repository.count());
        }
    }

    @Test
    void regionAndHeartWithTheSameDisplayNameAreDistinct() throws Exception {
        try (DiscoveryRepository repository = DiscoveryRepository.open(tempDir.resolve("exploration.db"))) {
            repository.insert(row("overworld", "region", "sakonotur", "Sakonotur", 2, "2026-09-26T17:31:42Z"));
            repository.insert(row("overworld", "heart", "heart_of_sakonotur", "Sakonotur", 0, "2026-09-26T17:32:00Z"));
            repository.insert(row("overworld", "village", "widecombe", "Widecombe", 0, "2026-09-26T17:33:00Z"));
            repository.insert(row("overworld", "nerve", "nerve_of_sakonotur", "Sakonotur", 0, "2026-09-26T17:34:00Z"));
            repository.insert(row("overworld", "structure", "ocean_ruin_37", "Ocean Ruin", 0, "2026-09-26T17:35:00Z"));
            repository.insert(row("overworld", "structure", "ocean_ruin_52", "Ocean Ruin", 0, "2026-09-26T17:36:00Z"));
            repository.insert(row("nether", "region", "sakonotur", "Sakonotur", 4, "2026-09-26T17:37:00Z"));
            assertEquals(7L, repository.count());
            assertEquals("heart_of_sakonotur", repository.find(PLAYER_UUID, "overworld", "heart", "heart_of_sakonotur").entityId());
            assertEquals("Ocean Ruin", repository.find(PLAYER_UUID, "overworld", "structure", "ocean_ruin_52").displayName());
            assertEquals("village", repository.find(PLAYER_UUID, "overworld", "village", "widecombe").entityType());
            assertEquals("nerve", repository.find(PLAYER_UUID, "overworld", "nerve", "nerve_of_sakonotur").entityType());
            assertNull(repository.find(PLAYER_UUID, "nether", "heart", "heart_of_sakonotur"));
        }
    }

    @Test
    void normalizedWorldAndTypeShareOneImmutableRow() throws Exception {
        try (DiscoveryRepository repository = DiscoveryRepository.open(tempDir.resolve("exploration.db"))) {
            OnlinePlayer player = new OnlinePlayer(UUID.fromString(PLAYER_UUID), "verzion");
            String[] first = command("Overworld", "Region", "Sakonotur", "2");
            String[] second = command("overworld", "region", "Renamed", "5");
            AttemptResult inserted = RecordAttempt.attempt(
                    "Lowothra",
                    first,
                    name -> Optional.of(player),
                    repository,
                    Instant.parse("2026-09-26T17:31:42Z")
            );
            AttemptResult duplicate = RecordAttempt.attempt(
                    "Lowothra",
                    second,
                    name -> Optional.of(player),
                    repository,
                    Instant.parse("2026-09-27T00:00:00Z")
            );
            assertInstanceOf(AttemptResult.Recorded.class, inserted);
            assertInstanceOf(AttemptResult.Duplicate.class, duplicate);
            StoredDiscovery stored = repository.find(PLAYER_UUID, "overworld", "region", "sakonotur");
            assertEquals("Sakonotur", stored.displayName());
            assertEquals(2, stored.difficulty());
            assertEquals("2026-09-26T17:31:42Z", stored.discoveredAt());
            assertEquals("Lowothra", stored.server());
            assertNull(stored.structureType());
            assertEquals(1L, repository.count());
        }
    }

    @Test
    void structureTypeIsStoredAndADuplicateLeavesItAlone() throws Exception {
        try (DiscoveryRepository repository = DiscoveryRepository.open(tempDir.resolve("exploration.db"))) {
            StoredDiscovery original = row(
                    "overworld", "structure", "ocean_ruin_37", "Ocean Ruin", 0, "2026-09-26T17:35:00Z", "ocean_ruin"
            );
            assertEquals(InsertOutcome.INSERTED, repository.insert(original));
            assertEquals(InsertOutcome.DUPLICATE, repository.insert(
                    row("overworld", "structure", "ocean_ruin_37", "Ocean Ruin", 0, "2026-09-27T00:00:00Z", "shipwreck")
            ));
            StoredDiscovery stored = repository.find(PLAYER_UUID, "overworld", "structure", "ocean_ruin_37");
            assertEquals("ocean_ruin", stored.structureType());
            assertEquals("2026-09-26T17:35:00Z", stored.discoveredAt());
        }
    }

    @Test
    void existingDatabaseGainsANullableStructureTypeColumn() throws Exception {
        Path database = tempDir.resolve("exploration.db");
        Class.forName("org.sqlite.JDBC");
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + database.toAbsolutePath().toString().replace('\\', '/'))) {
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("""
                        CREATE TABLE discoveries (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            player_uuid TEXT NOT NULL,
                            player_name TEXT NOT NULL,
                            server TEXT NOT NULL,
                            world TEXT NOT NULL,
                            entity_type TEXT NOT NULL,
                            entity_id TEXT NOT NULL,
                            display_name TEXT NOT NULL,
                            difficulty INTEGER NOT NULL,
                            discovered_at TEXT NOT NULL,
                            UNIQUE (player_uuid, world, entity_type, entity_id)
                        )
                        """);
                statement.executeUpdate("""
                        INSERT INTO discoveries (
                            player_uuid, player_name, server, world, entity_type, entity_id,
                            display_name, difficulty, discovered_at
                        ) VALUES (
                            '12345678-1234-1234-1234-123456789abc', 'verzion', 'Lowothra',
                            'overworld', 'structure', 'inner_core', 'Inner Core', 0, '2026-09-26T17:31:42Z'
                        )
                        """);
            }
        }

        try (DiscoveryRepository repository = DiscoveryRepository.open(database)) {
            StoredDiscovery existing = repository.find(PLAYER_UUID, "overworld", "structure", "inner_core");
            assertNull(existing.structureType());
            assertEquals(InsertOutcome.INSERTED, repository.insert(
                    row("overworld", "structure", "ocean_ruin_37", "Ocean Ruin", 0, "2026-09-26T17:35:00Z", "ocean_ruin")
            ));
            assertEquals("ocean_ruin", repository.find(PLAYER_UUID, "overworld", "structure", "ocean_ruin_37").structureType());
            assertNull(repository.find(PLAYER_UUID, "overworld", "structure", "inner_core").structureType());
        }
    }

    @Test
    void rowsSurviveReopen() throws Exception {
        Path database = tempDir.resolve("exploration.db");
        try (DiscoveryRepository repository = DiscoveryRepository.open(database)) {
            repository.insert(row("overworld", "region", "sakonotur", "Sakonotur", 2, "2026-09-26T17:31:42Z"));
        }
        try (DiscoveryRepository repository = DiscoveryRepository.open(database)) {
            assertEquals(
                    "Sakonotur",
                    repository.find(PLAYER_UUID, "overworld", "region", "sakonotur").displayName()
            );
            assertEquals(1L, repository.count());
        }
    }

    private static String[] command(String world, String type, String displayName, String difficulty) {
        return new String[]{
                "verzion",
                world,
                type,
                "sakonotur",
                Base64.getUrlEncoder().withoutPadding().encodeToString(displayName.getBytes(StandardCharsets.UTF_8)),
                difficulty
        };
    }

    private static StoredDiscovery row(
            String world,
            String type,
            String id,
            String displayName,
            int difficulty,
            String discoveredAt
    ) {
        return row(world, type, id, displayName, difficulty, discoveredAt, "Lowothra", "verzion", null);
    }

    private static StoredDiscovery row(
            String world,
            String type,
            String id,
            String displayName,
            int difficulty,
            String discoveredAt,
            String structureType
    ) {
        return row(world, type, id, displayName, difficulty, discoveredAt, "Lowothra", "verzion", structureType);
    }

    private static StoredDiscovery row(
            String world,
            String type,
            String id,
            String displayName,
            int difficulty,
            String discoveredAt,
            String server,
            String playerName
    ) {
        return row(world, type, id, displayName, difficulty, discoveredAt, server, playerName, null);
    }

    private static StoredDiscovery row(
            String world,
            String type,
            String id,
            String displayName,
            int difficulty,
            String discoveredAt,
            String server,
            String playerName,
            String structureType
    ) {
        return new StoredDiscovery(
                PLAYER_UUID,
                playerName,
                server,
                world,
                type,
                id,
                displayName,
                difficulty,
                discoveredAt,
                structureType
        );
    }
}
