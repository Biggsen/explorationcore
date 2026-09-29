package dev.explorationcore.db;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public final class DiscoveryRepository implements DiscoveryStore, AutoCloseable {
    private static final String CREATE_TABLE = """
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

    private static final String INSERT = """
            INSERT INTO discoveries (
                player_uuid,
                player_name,
                server,
                world,
                entity_type,
                entity_id,
                display_name,
                difficulty,
                discovered_at,
                structure_type
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String COUNT = "SELECT COUNT(*) FROM discoveries";

    private static final String FIND = """
            SELECT player_uuid, player_name, server, world, entity_type, entity_id,
                   display_name, difficulty, discovered_at, structure_type
            FROM discoveries
            WHERE player_uuid = ? AND world = ? AND entity_type = ? AND entity_id = ?
            """;

    private final Connection connection;
    private final Object lock = new Object();

    private DiscoveryRepository(Connection connection) {
        this.connection = connection;
    }

    public static DiscoveryRepository open(Path databaseFile) throws SQLException, IOException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException exception) {
            throw new SQLException("SQLite JDBC driver is missing", exception);
        }
        Path absolute = databaseFile.toAbsolutePath();
        Path parent = absolute.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        String url = "jdbc:sqlite:" + absolute.toString().replace('\\', '/');
        Connection connection = DriverManager.getConnection(url);
        try {
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate(CREATE_TABLE);
            }
            ensureStructureTypeColumn(connection);
            return new DiscoveryRepository(connection);
        } catch (SQLException exception) {
            connection.close();
            throw exception;
        }
    }

    @Override
    public InsertOutcome insert(StoredDiscovery discovery) throws SQLException {
        synchronized (lock) {
            ensureOpen();
            try (PreparedStatement statement = connection.prepareStatement(INSERT)) {
                statement.setString(1, discovery.playerUuid());
                statement.setString(2, discovery.playerName());
                statement.setString(3, discovery.server());
                statement.setString(4, discovery.world());
                statement.setString(5, discovery.entityType());
                statement.setString(6, discovery.entityId());
                statement.setString(7, discovery.displayName());
                statement.setInt(8, discovery.difficulty());
                statement.setString(9, discovery.discoveredAt());
                statement.setString(10, discovery.structureType());
                statement.executeUpdate();
                return InsertOutcome.INSERTED;
            } catch (SQLException exception) {
                if (isUniqueViolation(exception)) {
                    return InsertOutcome.DUPLICATE;
                }
                throw exception;
            }
        }
    }

    public long count() throws SQLException {
        synchronized (lock) {
            ensureOpen();
            try (Statement statement = connection.createStatement();
                 ResultSet results = statement.executeQuery(COUNT)) {
                if (!results.next()) {
                    return 0L;
                }
                return results.getLong(1);
            }
        }
    }

    StoredDiscovery find(String playerUuid, String world, String entityType, String entityId) throws SQLException {
        synchronized (lock) {
            ensureOpen();
            try (PreparedStatement statement = connection.prepareStatement(FIND)) {
                statement.setString(1, playerUuid);
                statement.setString(2, world);
                statement.setString(3, entityType);
                statement.setString(4, entityId);
                try (ResultSet results = statement.executeQuery()) {
                    if (!results.next()) {
                        return null;
                    }
                    return new StoredDiscovery(
                            results.getString("player_uuid"),
                            results.getString("player_name"),
                            results.getString("server"),
                            results.getString("world"),
                            results.getString("entity_type"),
                            results.getString("entity_id"),
                            results.getString("display_name"),
                            results.getInt("difficulty"),
                            results.getString("discovered_at"),
                            results.getString("structure_type")
                    );
                }
            }
        }
    }

    @Override
    public void close() throws SQLException {
        synchronized (lock) {
            if (!connection.isClosed()) {
                connection.close();
            }
        }
    }

    private void ensureOpen() throws SQLException {
        if (connection.isClosed()) {
            throw new SQLException("SQLite database is closed");
        }
    }

    private static void ensureStructureTypeColumn(Connection connection) throws SQLException {
        if (hasColumn(connection, "structure_type")) {
            return;
        }
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("ALTER TABLE discoveries ADD COLUMN structure_type TEXT");
        }
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

    private static boolean isUniqueViolation(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && message.contains("UNIQUE constraint failed")) {
                return true;
            }
            if (current instanceof SQLException sqlException) {
                int code = sqlException.getErrorCode();
                if (code == 2067 || code == 1555) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }
}
