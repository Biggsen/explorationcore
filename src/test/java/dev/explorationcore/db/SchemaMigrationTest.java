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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SchemaMigrationTest {
    private static final String PLAYER_UUID = "12345678-1234-1234-1234-123456789abc";
    private static final String DISCOVERED_AT = "2026-09-26T17:31:42Z";
    private static final String STAMP = "2000-01-01T00:00:00Z";

    private static final String INSERT_REWARD = """
            INSERT INTO reward_intents (
                player_uuid, source_type, source_key, reward_id, reward_payload, rule_version,
                status, created_at, updated_at, delivered_at, last_error
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String INSERT_MILESTONE = """
            INSERT INTO milestone_grants (
                player_uuid, milestone_id, status, achieved_at, rule_version
            ) VALUES (?, ?, ?, ?, ?)
            """;

    @TempDir
    Path tempDir;

    @Test
    void freshDatabaseReceivesTheFullSchema() throws Exception {
        Path database = tempDir.resolve("exploration.db");
        try (DiscoveryRepository repository = DiscoveryRepository.open(database)) {
            assertEquals(0L, repository.count());
        }

        try (Connection connection = connect(database)) {
            assertEquals(
                    List.of(
                            "id", "player_uuid", "player_name", "server", "world", "entity_type", "entity_id",
                            "display_name", "difficulty", "discovered_at", "structure_type"
                    ),
                    columnNames(connection, "discoveries")
            );
            assertEquals(
                    List.of(
                            "id", "player_uuid", "source_type", "source_key", "reward_id", "reward_payload",
                            "rule_version", "status", "created_at", "updated_at", "delivered_at", "last_error"
                    ),
                    columnNames(connection, "reward_intents")
            );
            assertEquals(
                    List.of("player_uuid", "milestone_id", "status", "achieved_at", "rule_version"),
                    columnNames(connection, "milestone_grants")
            );
            assertTrue(indexExists(connection, "idx_reward_intents_status"));
            assertEquals(
                    List.of(
                            new MigrationRow(1, "discoveries ledger"),
                            new MigrationRow(2, "reward intents"),
                            new MigrationRow(3, "milestone grants")
                    ),
                    migrationRows(connection)
            );
            for (String appliedAt : appliedAtValues(connection)) {
                assertTrue(appliedAt.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z"), appliedAt);
            }
        }
    }

    @Test
    void currentSchemaKeepsExistingDiscoveries() throws Exception {
        Path database = tempDir.resolve("exploration.db");
        try (Connection connection = connect(database)) {
            execute(connection, """
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
                        structure_type TEXT,
                        UNIQUE (player_uuid, world, entity_type, entity_id)
                    )
                    """);
            execute(connection, """
                    INSERT INTO discoveries (
                        player_uuid, player_name, server, world, entity_type, entity_id,
                        display_name, difficulty, discovered_at, structure_type
                    ) VALUES (
                        '12345678-1234-1234-1234-123456789abc', 'verzion', 'Lowothra',
                        'overworld', 'structure', 'inner_core', 'Inner Core', 0,
                        '2026-09-26T17:31:42Z', NULL
                    )
                    """);
        }

        try (DiscoveryRepository repository = DiscoveryRepository.open(database)) {
            assertEquals(legacyDiscovery(null), repository.find(PLAYER_UUID, "overworld", "structure", "inner_core"));
            assertEquals(1L, repository.count());
        }
        assertEquals(List.of(1, 2, 3), versions(database));
    }

    @Test
    void databaseWithoutStructureTypeGainsTheColumnAndKeepsTheRow() throws Exception {
        Path database = tempDir.resolve("exploration.db");
        try (Connection connection = connect(database)) {
            execute(connection, """
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
            execute(connection, """
                    INSERT INTO discoveries (
                        player_uuid, player_name, server, world, entity_type, entity_id,
                        display_name, difficulty, discovered_at
                    ) VALUES (
                        '12345678-1234-1234-1234-123456789abc', 'verzion', 'Lowothra',
                        'overworld', 'structure', 'inner_core', 'Inner Core', 0, '2026-09-26T17:31:42Z'
                    )
                    """);
        }

        try (DiscoveryRepository repository = DiscoveryRepository.open(database)) {
            StoredDiscovery stored = repository.find(PLAYER_UUID, "overworld", "structure", "inner_core");
            assertEquals(legacyDiscovery(null), stored);
            assertNull(stored.structureType());
            assertEquals(DISCOVERED_AT, stored.discoveredAt());
            assertEquals(1L, repository.count());
        }
        assertTrue(columnNames(database, "discoveries").contains("structure_type"));
    }

    @Test
    void secondOpenDoesNotReapplyMigrations() throws Exception {
        Path database = tempDir.resolve("exploration.db");
        try (DiscoveryRepository repository = DiscoveryRepository.open(database)) {
            assertEquals(0L, repository.count());
        }
        try (Connection connection = connect(database);
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE schema_migrations SET applied_at = ?"
             )) {
            statement.setString(1, STAMP);
            assertEquals(3, statement.executeUpdate());
        }

        try (DiscoveryRepository repository = DiscoveryRepository.open(database)) {
            assertEquals(0L, repository.count());
        }

        try (Connection connection = connect(database)) {
            assertEquals(List.of(1, 2, 3), versions(connection));
            assertEquals(List.of(STAMP, STAMP, STAMP), appliedAtValues(connection));
        }
    }

    @Test
    void unsupportedFutureSchemaVersionAbortsBeforeMigrating() throws Exception {
        Path database = tempDir.resolve("exploration.db");
        try (Connection connection = connect(database)) {
            createCurrentDiscoveries(connection);
            insertLegacyDiscovery(connection);
            createMigrationTable(connection);
            execute(connection, """
                    INSERT INTO schema_migrations (version, description, applied_at)
                    VALUES (99, 'future', '2026-01-01T00:00:00Z')
                    """);
        }

        SQLException failure = assertThrows(SQLException.class, () -> DiscoveryRepository.open(database));
        assertEquals(
                "Unsupported schema version 99; this build supports versions 1 through 3",
                failure.getMessage()
        );

        try (Connection connection = connect(database)) {
            assertFalse(tableExists(connection, "reward_intents"));
            assertFalse(tableExists(connection, "milestone_grants"));
            assertEquals(List.of(99), versions(connection));
            assertEquals(DISCOVERED_AT, discoveryTimestamp(connection));
        }
    }

    @Test
    void missingMigrationVersionAbortsBeforePendingMigrations() throws Exception {
        Path database = tempDir.resolve("exploration.db");
        try (Connection connection = connect(database)) {
            createCurrentDiscoveries(connection);
            insertLegacyDiscovery(connection);
            createMigrationTable(connection);
            execute(connection, """
                    INSERT INTO schema_migrations (version, description, applied_at) VALUES
                    (1, 'discoveries ledger', '2026-01-01T00:00:00Z'),
                    (3, 'milestone grants', '2026-01-01T00:00:01Z')
                    """);
        }

        SQLException failure = assertThrows(SQLException.class, () -> DiscoveryRepository.open(database));
        assertEquals(
                "Missing schema version 2; found schema version 3; this build supports versions 1 through 3",
                failure.getMessage()
        );

        try (Connection connection = connect(database)) {
            assertFalse(tableExists(connection, "reward_intents"));
            assertFalse(tableExists(connection, "milestone_grants"));
            assertEquals(List.of(1, 3), versions(connection));
            assertEquals(DISCOVERED_AT, discoveryTimestamp(connection));
        }
    }

    @Test
    void failedMigrationRollsBackAndKeepsEarlierVersions() throws Exception {
        Path database = tempDir.resolve("exploration.db");
        List<SchemaMigrator.Migration> steps = new ArrayList<>(SchemaMigrator.production());
        steps.add(new SchemaMigrator.Migration() {
            @Override
            public int version() {
                return 4;
            }

            @Override
            public String description() {
                return "probe";
            }

            @Override
            public void apply(Connection connection) throws SQLException {
                execute(connection, "CREATE TABLE rollback_probe (id INTEGER)");
                throw new SQLException("probe failed");
            }
        });

        try (Connection connection = connect(database)) {
            SQLException failure = assertThrows(SQLException.class, () -> SchemaMigrator.migrate(connection, steps));
            assertEquals("probe failed", failure.getMessage());
            assertFalse(tableExists(connection, "rollback_probe"));
            assertEquals(List.of(1, 2, 3), versions(connection));
            assertTrue(tableExists(connection, "discoveries"));
            assertTrue(tableExists(connection, "reward_intents"));
            assertTrue(tableExists(connection, "milestone_grants"));
        }
    }

    @Test
    void duplicateRewardIntentLeavesTheFirstRow() throws Exception {
        Path database = tempDir.resolve("exploration.db");
        try (DiscoveryRepository repository = DiscoveryRepository.open(database)) {
            assertEquals(0L, repository.count());
        }

        try (Connection connection = connect(database)) {
            insertReward(
                    connection,
                    "crate:1",
                    "rules-1",
                    "pending",
                    "2026-09-26T17:31:42Z",
                    "2026-09-26T17:31:42Z",
                    null,
                    null
            );
            assertThrows(SQLException.class, () -> insertReward(
                    connection,
                    "crate:9",
                    "rules-2",
                    "delivered",
                    "2026-10-01T00:00:00Z",
                    "2026-10-01T00:00:00Z",
                    "2026-10-01T00:00:00Z",
                    "nope"
            ));
            assertThrows(SQLException.class, () -> insertReward(
                    connection,
                    "discovery",
                    "other",
                    "other-reward",
                    "crate:1",
                    "rules-1",
                    null,
                    "2026-09-26T17:31:42Z",
                    "2026-09-26T17:31:42Z",
                    null,
                    null
            ));

            try (Statement statement = connection.createStatement();
                 ResultSet results = statement.executeQuery("""
                         SELECT reward_payload, rule_version, status, created_at, updated_at, delivered_at, last_error
                         FROM reward_intents
                         """)) {
                assertTrue(results.next());
                assertEquals("crate:1", results.getString("reward_payload"));
                assertEquals("rules-1", results.getString("rule_version"));
                assertEquals("pending", results.getString("status"));
                assertEquals("2026-09-26T17:31:42Z", results.getString("created_at"));
                assertEquals("2026-09-26T17:31:42Z", results.getString("updated_at"));
                assertNull(results.getString("delivered_at"));
                assertNull(results.getString("last_error"));
                assertFalse(results.next());
            }
        }
    }

    @Test
    void duplicateMilestoneGrantLeavesTheFirstRow() throws Exception {
        Path database = tempDir.resolve("exploration.db");
        try (DiscoveryRepository repository = DiscoveryRepository.open(database)) {
            assertEquals(0L, repository.count());
        }

        try (Connection connection = connect(database)) {
            insertMilestone(connection, "regions:15", "historical", "2026-09-26T17:31:42Z", "rules-1");
            assertThrows(SQLException.class, () -> insertMilestone(
                    connection,
                    "regions:15",
                    "earned",
                    null,
                    "rules-2"
            ));
            assertThrows(SQLException.class, () -> insertMilestone(
                    connection,
                    "regions:30",
                    null,
                    null,
                    "rules-1"
            ));

            try (Statement statement = connection.createStatement();
                 ResultSet results = statement.executeQuery("""
                         SELECT status, achieved_at, rule_version
                         FROM milestone_grants
                         """)) {
                assertTrue(results.next());
                assertEquals("historical", results.getString("status"));
                assertEquals("2026-09-26T17:31:42Z", results.getString("achieved_at"));
                assertEquals("rules-1", results.getString("rule_version"));
                assertFalse(results.next());
            }
        }
    }

    @Test
    void recordingWritesADiscoveryAndNothingElse() throws Exception {
        Path database = tempDir.resolve("exploration.db");
        OnlinePlayer player = new OnlinePlayer(UUID.fromString(PLAYER_UUID), "verzion");
        String[] args = {
                "verzion",
                "overworld",
                "region",
                "sakonotur",
                Base64.getUrlEncoder().withoutPadding().encodeToString("Sakonotur".getBytes(StandardCharsets.UTF_8)),
                "2"
        };
        try (DiscoveryRepository repository = DiscoveryRepository.open(database)) {
            AttemptResult result = RecordAttempt.attempt(
                    "Lowothra",
                    args,
                    name -> Optional.of(player),
                    repository,
                    Instant.parse(DISCOVERED_AT)
            );
            assertInstanceOf(AttemptResult.Recorded.class, result);
            StoredDiscovery stored = repository.find(PLAYER_UUID, "overworld", "region", "sakonotur");
            assertEquals("Sakonotur", stored.displayName());
            assertEquals(DISCOVERED_AT, stored.discoveredAt());
            assertEquals(1L, repository.count());
        }

        try (Connection connection = connect(database)) {
            assertEquals(0, countRows(connection, "reward_intents"));
            assertEquals(0, countRows(connection, "milestone_grants"));
        }
    }

    private static StoredDiscovery legacyDiscovery(String structureType) {
        return new StoredDiscovery(
                PLAYER_UUID,
                "verzion",
                "Lowothra",
                "overworld",
                "structure",
                "inner_core",
                "Inner Core",
                0,
                DISCOVERED_AT,
                structureType
        );
    }

    private static void createCurrentDiscoveries(Connection connection) throws SQLException {
        execute(connection, """
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
                    structure_type TEXT,
                    UNIQUE (player_uuid, world, entity_type, entity_id)
                )
                """);
    }

    private static void createMigrationTable(Connection connection) throws SQLException {
        execute(connection, """
                CREATE TABLE schema_migrations (
                    version INTEGER PRIMARY KEY,
                    description TEXT NOT NULL,
                    applied_at TEXT NOT NULL
                )
                """);
    }

    private static void insertLegacyDiscovery(Connection connection) throws SQLException {
        execute(connection, """
                INSERT INTO discoveries (
                    player_uuid, player_name, server, world, entity_type, entity_id,
                    display_name, difficulty, discovered_at
                ) VALUES (
                    '12345678-1234-1234-1234-123456789abc', 'verzion', 'Lowothra',
                    'overworld', 'structure', 'inner_core', 'Inner Core', 0, '2026-09-26T17:31:42Z'
                )
                """);
    }

    private static void insertReward(
            Connection connection,
            String payload,
            String ruleVersion,
            String status,
            String createdAt,
            String updatedAt,
            String deliveredAt,
            String lastError
    ) throws SQLException {
        insertReward(
                connection,
                "discovery",
                "overworld|region|sakonotur",
                "region-key",
                payload,
                ruleVersion,
                status,
                createdAt,
                updatedAt,
                deliveredAt,
                lastError
        );
    }

    private static void insertReward(
            Connection connection,
            String sourceType,
            String sourceKey,
            String rewardId,
            String payload,
            String ruleVersion,
            String status,
            String createdAt,
            String updatedAt,
            String deliveredAt,
            String lastError
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(INSERT_REWARD)) {
            statement.setString(1, PLAYER_UUID);
            statement.setString(2, sourceType);
            statement.setString(3, sourceKey);
            statement.setString(4, rewardId);
            statement.setString(5, payload);
            statement.setString(6, ruleVersion);
            statement.setString(7, status);
            statement.setString(8, createdAt);
            statement.setString(9, updatedAt);
            statement.setString(10, deliveredAt);
            statement.setString(11, lastError);
            statement.executeUpdate();
        }
    }

    private static void insertMilestone(
            Connection connection,
            String milestoneId,
            String status,
            String achievedAt,
            String ruleVersion
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(INSERT_MILESTONE)) {
            statement.setString(1, PLAYER_UUID);
            statement.setString(2, milestoneId);
            statement.setString(3, status);
            statement.setString(4, achievedAt);
            statement.setString(5, ruleVersion);
            statement.executeUpdate();
        }
    }

    private static List<String> columnNames(Path database, String table) throws Exception {
        try (Connection connection = connect(database)) {
            return columnNames(connection, table);
        }
    }

    private static List<String> columnNames(Connection connection, String table) throws SQLException {
        List<String> names = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet results = statement.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (results.next()) {
                names.add(results.getString("name"));
            }
        }
        return names;
    }

    private static boolean indexExists(Connection connection, String name) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM sqlite_master WHERE type = 'index' AND name = ?"
        )) {
            statement.setString(1, name);
            try (ResultSet results = statement.executeQuery()) {
                return results.next();
            }
        }
    }

    private static boolean tableExists(Connection connection, String name) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ?"
        )) {
            statement.setString(1, name);
            try (ResultSet results = statement.executeQuery()) {
                return results.next();
            }
        }
    }

    private static List<Integer> versions(Path database) throws Exception {
        try (Connection connection = connect(database)) {
            return versions(connection);
        }
    }

    private static List<Integer> versions(Connection connection) throws SQLException {
        List<Integer> versions = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet results = statement.executeQuery(
                     "SELECT version FROM schema_migrations ORDER BY version"
             )) {
            while (results.next()) {
                versions.add(results.getInt(1));
            }
        }
        return versions;
    }

    private static List<MigrationRow> migrationRows(Connection connection) throws SQLException {
        List<MigrationRow> rows = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet results = statement.executeQuery(
                     "SELECT version, description FROM schema_migrations ORDER BY version"
             )) {
            while (results.next()) {
                rows.add(new MigrationRow(results.getInt("version"), results.getString("description")));
            }
        }
        return rows;
    }

    private static List<String> appliedAtValues(Connection connection) throws SQLException {
        List<String> values = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet results = statement.executeQuery(
                     "SELECT applied_at FROM schema_migrations ORDER BY version"
             )) {
            while (results.next()) {
                values.add(results.getString(1));
            }
        }
        return values;
    }

    private static String discoveryTimestamp(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet results = statement.executeQuery(
                     "SELECT discovered_at FROM discoveries WHERE entity_id = 'inner_core'"
             )) {
            assertTrue(results.next());
            return results.getString(1);
        }
    }

    private static int countRows(Connection connection, String table) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet results = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
            assertTrue(results.next());
            return results.getInt(1);
        }
    }

    private static void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    private static Connection connect(Path database) throws Exception {
        Class.forName("org.sqlite.JDBC");
        String url = "jdbc:sqlite:" + database.toAbsolutePath().toString().replace('\\', '/');
        return DriverManager.getConnection(url);
    }

    private record MigrationRow(int version, String description) {
    }
}
