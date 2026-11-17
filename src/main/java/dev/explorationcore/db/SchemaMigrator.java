package dev.explorationcore.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

final class SchemaMigrator {
    private static final String CREATE_MIGRATIONS = """
            CREATE TABLE IF NOT EXISTS schema_migrations (
                version INTEGER PRIMARY KEY,
                description TEXT NOT NULL,
                applied_at TEXT NOT NULL
            )
            """;

    private static final String CREATE_DISCOVERIES = """
            CREATE TABLE IF NOT EXISTS discoveries (
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
                UNIQUE (
                    player_uuid,
                    world,
                    entity_type,
                    entity_id
                )
            )
            """;

    private static final String CREATE_REWARD_INTENTS = """
            CREATE TABLE reward_intents (
                id INTEGER PRIMARY KEY,
                player_uuid TEXT NOT NULL,
                source_type TEXT NOT NULL,
                source_key TEXT NOT NULL,
                reward_id TEXT NOT NULL,
                reward_payload TEXT NOT NULL,
                rule_version TEXT NOT NULL,
                status TEXT NOT NULL,
                created_at TEXT NOT NULL,
                updated_at TEXT NOT NULL,
                delivered_at TEXT,
                last_error TEXT,
                UNIQUE (
                    player_uuid,
                    source_type,
                    source_key,
                    reward_id
                )
            )
            """;

    private static final String CREATE_REWARD_STATUS_INDEX =
            "CREATE INDEX idx_reward_intents_status ON reward_intents(status)";

    private static final String CREATE_MILESTONE_GRANTS = """
            CREATE TABLE milestone_grants (
                player_uuid TEXT NOT NULL,
                milestone_id TEXT NOT NULL,
                status TEXT NOT NULL,
                achieved_at TEXT,
                rule_version TEXT NOT NULL,
                PRIMARY KEY (player_uuid, milestone_id)
            )
            """;

    private static final String CREATE_AMP_MAP_MARKERS = """
            CREATE TABLE map_amp_markers (
                map_id INTEGER NOT NULL,
                amp_id TEXT NOT NULL,
                kind TEXT NOT NULL,
                display_name TEXT NOT NULL,
                x INTEGER NOT NULL,
                y INTEGER NOT NULL,
                z INTEGER NOT NULL,
                PRIMARY KEY (map_id, amp_id)
            )
            """;

    private static final String INSERT_MIGRATION = """
            INSERT INTO schema_migrations (version, description, applied_at)
            VALUES (?, ?, ?)
            """;

    private SchemaMigrator() {
    }

    static void migrate(Connection connection) throws SQLException {
        migrate(connection, production());
    }

    static void migrate(Connection connection, List<Migration> migrations) throws SQLException {
        List<Migration> ordered = validate(migrations);
        int maxVersion = ordered.get(ordered.size() - 1).version();
        execute(connection, CREATE_MIGRATIONS);
        TreeSet<Integer> applied = appliedVersions(connection);
        int highest = applied.isEmpty() ? 0 : applied.last();
        if (highest > maxVersion) {
            throw new SQLException(
                    "Unsupported schema version " + highest
                            + "; this build supports versions 1 through " + maxVersion
            );
        }
        List<Integer> missing = new ArrayList<>();
        for (int version = 1; version < highest; version++) {
            if (!applied.contains(version)) {
                missing.add(version);
            }
        }
        if (!missing.isEmpty()) {
            throw new SQLException(
                    "Missing schema " + (missing.size() == 1 ? "version " : "versions ")
                            + join(missing)
                            + "; found schema version " + highest
                            + "; this build supports versions 1 through " + maxVersion
            );
        }
        for (Migration migration : ordered) {
            if (!applied.contains(migration.version())) {
                apply(connection, migration);
            }
        }
    }

    static List<Migration> production() {
        return List.of(
                new DefinedMigration(1, "discoveries ledger", SchemaMigrator::ensureDiscoveries),
                new DefinedMigration(2, "reward intents", SchemaMigrator::createRewardIntents),
                new DefinedMigration(3, "milestone grants", SchemaMigrator::createMilestoneGrants),
                new DefinedMigration(4, "amp map markers", SchemaMigrator::createAmpMapMarkers)
        );
    }

    private static List<Migration> validate(List<Migration> migrations) throws SQLException {
        if (migrations == null || migrations.isEmpty()) {
            throw new SQLException("Schema migrations are missing");
        }
        List<Migration> ordered = new ArrayList<>(migrations);
        ordered.sort(Comparator.comparingInt(Migration::version));
        for (int index = 0; index < ordered.size(); index++) {
            int expected = index + 1;
            if (ordered.get(index).version() != expected) {
                throw new SQLException(
                        "Schema migrations must be versions 1 through " + ordered.size() + " without gaps"
                );
            }
        }
        return List.copyOf(ordered);
    }

    private static void apply(Connection connection, Migration migration) throws SQLException {
        boolean autoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        SQLException failure = null;
        try {
            migration.apply(connection);
            insertVersion(connection, migration);
            connection.commit();
        } catch (Exception exception) {
            failure = asSqlException(migration, exception);
            try {
                connection.rollback();
            } catch (SQLException rollback) {
                failure.addSuppressed(rollback);
            }
        } finally {
            try {
                if (connection.getAutoCommit() != autoCommit) {
                    connection.setAutoCommit(autoCommit);
                }
            } catch (SQLException exception) {
                if (failure == null) {
                    failure = exception;
                } else {
                    failure.addSuppressed(exception);
                }
            }
        }
        if (failure != null) {
            throw failure;
        }
    }

    private static SQLException asSqlException(Migration migration, Exception exception) {
        if (exception instanceof SQLException sqlException) {
            return sqlException;
        }
        return new SQLException("Schema migration " + migration.version() + " failed", exception);
    }

    private static void insertVersion(Connection connection, Migration migration) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(INSERT_MIGRATION)) {
            statement.setInt(1, migration.version());
            statement.setString(2, migration.description());
            statement.setString(3, appliedAt());
            statement.executeUpdate();
        }
    }

    private static TreeSet<Integer> appliedVersions(Connection connection) throws SQLException {
        TreeSet<Integer> versions = new TreeSet<>();
        try (Statement statement = connection.createStatement();
             ResultSet results = statement.executeQuery("SELECT version FROM schema_migrations")) {
            while (results.next()) {
                versions.add(results.getInt(1));
            }
        }
        return versions;
    }

    private static void ensureDiscoveries(Connection connection) throws SQLException {
        execute(connection, CREATE_DISCOVERIES);
        if (hasColumn(connection, "structure_type")) {
            return;
        }
        execute(connection, "ALTER TABLE discoveries ADD COLUMN structure_type TEXT");
    }

    private static void createRewardIntents(Connection connection) throws SQLException {
        execute(connection, CREATE_REWARD_INTENTS);
        execute(connection, CREATE_REWARD_STATUS_INDEX);
    }

    private static void createMilestoneGrants(Connection connection) throws SQLException {
        execute(connection, CREATE_MILESTONE_GRANTS);
    }

    private static void createAmpMapMarkers(Connection connection) throws SQLException {
        execute(connection, CREATE_AMP_MAP_MARKERS);
    }

    private static boolean hasColumn(Connection connection, String name) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet results = statement.executeQuery("PRAGMA table_info(discoveries)")) {
            while (results.next()) {
                if (name.equals(results.getString("name"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    private static String join(List<Integer> versions) {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < versions.size(); index++) {
            if (index > 0) {
                builder.append(", ");
            }
            builder.append(versions.get(index));
        }
        return builder.toString();
    }

    // Same UTC instant with second precision as Timestamps.format. Kept here so db does not depend on record.
    private static String appliedAt() {
        return DateTimeFormatter.ISO_INSTANT.format(Instant.now().truncatedTo(ChronoUnit.SECONDS));
    }

    interface Migration {
        int version();

        String description();

        void apply(Connection connection) throws SQLException;
    }

    private record DefinedMigration(int version, String description, Step step) implements Migration {
        @Override
        public void apply(Connection connection) throws SQLException {
            step.apply(connection);
        }
    }

    @FunctionalInterface
    private interface Step {
        void apply(Connection connection) throws SQLException;
    }
}
