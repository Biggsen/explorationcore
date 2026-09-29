package dev.explorationcore.db;

public record StoredDiscovery(
        String playerUuid,
        String playerName,
        String server,
        String world,
        String entityType,
        String entityId,
        String displayName,
        int difficulty,
        String discoveredAt,
        String structureType
) {
}
