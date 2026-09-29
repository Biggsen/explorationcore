package dev.explorationcore.record;

public sealed interface AttemptResult {
    record Rejected(String reason) implements AttemptResult {
    }

    record Recorded(
            String playerName,
            String world,
            String entityType,
            String entityId,
            String structureType
    ) implements AttemptResult {
        public String logLine() {
            return "Recorded " + playerName + " -> " + world + "/" + entityType + "/" + entityId;
        }
    }

    record Duplicate(String playerName, String world, String entityType, String entityId) implements AttemptResult {
        public String logLine() {
            return "Duplicate ignored " + playerName + " -> " + world + "/" + entityType + "/" + entityId;
        }
    }

    record Failed(String player, String world, String entityType, String entityId, Exception cause) implements AttemptResult {
        public String logLine() {
            return "Persistence failed: player=" + player
                    + " world=" + world
                    + " entity_type=" + entityType
                    + " entity_id=" + entityId;
        }
    }
}
