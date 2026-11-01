package dev.explorationcore.db;

public record DiscoveryEntry(
        String world,
        String entityType,
        String structureType,
        String entityId,
        String displayName
) {
}
