package dev.explorationcore.db;

import java.sql.SQLException;

public interface DiscoveryStore {
    InsertOutcome insert(StoredDiscovery discovery) throws SQLException;
}
