package dev.explorationcore.record;

import dev.explorationcore.db.DiscoveryStore;
import dev.explorationcore.db.InsertOutcome;
import dev.explorationcore.db.StoredDiscovery;

import java.time.Instant;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;

public final class RecordAttempt {
    private static final String NO_CRATE = "nocrate";
    private static final Set<String> ENTITY_TYPES = Set.of(
            "region",
            "village",
            "heart",
            "nerve",
            "structure"
    );

    private RecordAttempt() {
    }

    public static AttemptResult attempt(
            String serverName,
            String[] args,
            OnlinePlayers players,
            DiscoveryStore store,
            Instant now
    ) {
        String player = argument(args, 0);
        String world = argument(args, 1);
        String entityType = argument(args, 2);
        String entityId = argument(args, 3);
        try {
            boolean skipCrate = false;
            if (args != null && args.length > 0 && isNoCrate(args[args.length - 1])) {
                skipCrate = true;
                args = Arrays.copyOf(args, args.length - 1);
            }
            if (args.length != 6 && args.length != 7) {
                return new AttemptResult.Rejected(
                        "expected 6 or 7 arguments: <player> <world> <entity-type> <entity-id> <display-name> <difficulty> [structure-type] [nocrate]"
                );
            }
            if (serverName == null || serverName.isBlank()) {
                return new AttemptResult.Rejected("server-name is missing or blank");
            }
            if (player.isBlank() || containsWhitespace(player)) {
                return new AttemptResult.Rejected("player name must be a single token");
            }
            OnlinePlayer online = players.find(player).orElse(null);
            if (online == null) {
                return new AttemptResult.Rejected("player is not online: " + player);
            }
            String normalizedWorld = world.trim().toLowerCase(Locale.ROOT);
            world = normalizedWorld;
            if (!normalizedWorld.equals("overworld") && !normalizedWorld.equals("nether")) {
                return new AttemptResult.Rejected("unknown world: " + argument(args, 1));
            }
            String normalizedType = entityType.trim().toLowerCase(Locale.ROOT);
            entityType = normalizedType;
            if (!ENTITY_TYPES.contains(normalizedType)) {
                return new AttemptResult.Rejected("unknown entity type: " + argument(args, 2));
            }
            if (entityId.isBlank() || containsWhitespace(entityId)) {
                return new AttemptResult.Rejected("entity id must be a non-empty token without whitespace");
            }
            String displayName;
            try {
                displayName = DisplayNameCodec.decode(args[4]);
            } catch (IllegalArgumentException exception) {
                return new AttemptResult.Rejected(exception.getMessage());
            }
            int difficulty;
            try {
                difficulty = Integer.parseInt(args[5]);
            } catch (NumberFormatException exception) {
                return new AttemptResult.Rejected("difficulty must be an integer from 0 through 5");
            }
            if (difficulty < 0 || difficulty > 5) {
                return new AttemptResult.Rejected("difficulty must be an integer from 0 through 5");
            }
            String structureType = null;
            if (args.length == 7) {
                String rawStructureType = args[6];
                if (rawStructureType == null || rawStructureType.isBlank() || containsWhitespace(rawStructureType)) {
                    return new AttemptResult.Rejected("structure_type must be a non-empty token without whitespace");
                }
                if (!normalizedType.equals("structure")) {
                    return new AttemptResult.Rejected("structure_type is only recorded for structures");
                }
                structureType = rawStructureType.trim().toLowerCase(Locale.ROOT);
                if (NO_CRATE.equals(structureType)) {
                    return new AttemptResult.Rejected("nocrate is not a structure type");
                }
            }
            player = online.name();
            if (store == null) {
                return new AttemptResult.Failed(player, world, entityType, entityId, null);
            }
            StoredDiscovery discovery = new StoredDiscovery(
                    online.uuid().toString(),
                    online.name(),
                    serverName.trim(),
                    normalizedWorld,
                    normalizedType,
                    entityId,
                    displayName,
                    difficulty,
                    Timestamps.format(now),
                    structureType
            );
            InsertOutcome outcome = store.insert(discovery);
            return switch (outcome) {
                case INSERTED -> new AttemptResult.Recorded(
                        online.uuid(),
                        player,
                        world,
                        entityType,
                        entityId,
                        displayName,
                        structureType,
                        skipCrate
                );
                case DUPLICATE -> new AttemptResult.Duplicate(player, world, entityType, entityId);
            };
        } catch (Exception exception) {
            return new AttemptResult.Failed(player, world, entityType, entityId, exception);
        }
    }

    private static String argument(String[] args, int index) {
        if (args == null || index < 0 || index >= args.length || args[index] == null) {
            return "";
        }
        return args[index];
    }

    private static boolean isNoCrate(String token) {
        return token != null && NO_CRATE.equals(token.trim().toLowerCase(Locale.ROOT));
    }

    private static boolean containsWhitespace(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (Character.isWhitespace(value.charAt(i))) {
                return true;
            }
        }
        return false;
    }
}
