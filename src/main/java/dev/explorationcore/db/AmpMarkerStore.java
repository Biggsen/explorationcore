package dev.explorationcore.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class AmpMarkerStore {
    private static final String INSERT = """
            INSERT INTO map_amp_markers (map_id, amp_id, kind, display_name, x, y, z)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String FOR_MAP = """
            SELECT amp_id, kind, display_name, x, y, z
            FROM map_amp_markers
            WHERE map_id = ?
            ORDER BY amp_id
            """;

    private final Connection connection;
    private final Object lock;

    AmpMarkerStore(Connection connection, Object lock) {
        this.connection = connection;
        this.lock = lock;
    }

    public boolean insert(Marker marker) throws SQLException {
        synchronized (lock) {
            ensureOpen();
            try (PreparedStatement statement = connection.prepareStatement(INSERT)) {
                statement.setInt(1, marker.mapId());
                statement.setString(2, marker.ampId());
                statement.setString(3, marker.kind());
                statement.setString(4, marker.name());
                statement.setInt(5, marker.x());
                statement.setInt(6, marker.y());
                statement.setInt(7, marker.z());
                statement.executeUpdate();
                return true;
            } catch (SQLException exception) {
                if (isUniqueViolation(exception)) {
                    return false;
                }
                throw exception;
            }
        }
    }

    public List<Marker> forMap(int mapId) throws SQLException {
        synchronized (lock) {
            ensureOpen();
            try (PreparedStatement statement = connection.prepareStatement(FOR_MAP)) {
                statement.setInt(1, mapId);
                try (ResultSet results = statement.executeQuery()) {
                    List<Marker> markers = new ArrayList<>();
                    while (results.next()) {
                        markers.add(new Marker(
                                mapId,
                                results.getString(1),
                                results.getString(2),
                                results.getString(3),
                                results.getInt(4),
                                results.getInt(5),
                                results.getInt(6)
                        ));
                    }
                    return markers;
                }
            }
        }
    }

    private void ensureOpen() throws SQLException {
        if (connection.isClosed()) {
            throw new SQLException("SQLite database is closed");
        }
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

    public record Marker(int mapId, String ampId, String kind, String name, int x, int y, int z) {
    }
}
