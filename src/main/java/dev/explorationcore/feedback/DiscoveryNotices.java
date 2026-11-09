package dev.explorationcore.feedback;

import dev.explorationcore.record.AttemptResult;

import java.util.Optional;

public final class DiscoveryNotices {
    private DiscoveryNotices() {
    }

    public static Optional<DiscoveryNotice> notice(AttemptResult result) {
        if (!(result instanceof AttemptResult.Recorded recorded)) {
            return Optional.empty();
        }
        if ("structure".equals(recorded.entityType())) {
            if (recorded.structureType() == null) {
                return Optional.empty();
            }
            return Optional.of(found(recorded.structureType(), recorded.displayName()));
        }
        String title = switch (recorded.world() + "/" + recorded.entityType()) {
            case "overworld/region", "overworld/water" -> "Region Discovered";
            case "overworld/village" -> "Village Discovered";
            case "overworld/heart" -> "Heart Discovered";
            case "overworld/nerve" -> "Nerve Discovered";
            case "overworld/soul" -> "Soul Discovered";
            case "nether/region" -> "Nether Region Discovered";
            case "nether/heart" -> "Nether Heart Discovered";
            default -> null;
        };
        if (title == null) {
            return Optional.empty();
        }
        String sentence = sentence(recorded.entityType(), recorded.displayName());
        return Optional.of(new DiscoveryNotice(title, sentence, sentence + "!"));
    }

    private static String sentence(String entityType, String displayName) {
        return switch (entityType) {
            case "heart" -> "You discovered the Heart of " + displayName;
            case "nerve" -> "You discovered the Nerve of " + displayName;
            case "soul" -> "You discovered the Soul of " + displayName;
            default -> "You discovered " + displayName;
        };
    }

    public static DiscoveryNotice found(String structureType, String displayName) {
        return new DiscoveryNotice(
                family(structureType) + " Found",
                "You found " + displayName,
                "You found " + displayName + "!"
        );
    }

    public static String experience(int amount) {
        return "You received " + amount + " experience!";
    }

    public static String claimblocks(int amount) {
        return "You received your reward: " + amount + " claimblocks";
    }

    public static String item(int amount, String name) {
        if (amount == 1) {
            return "You received your reward: " + name;
        }
        return "You received your reward: " + name + " ×" + amount;
    }

    public static DiscoveryNotice milestone(String displayName, String message) {
        return new DiscoveryNotice(displayName, message, message);
    }

    private static String family(String structureType) {
        StringBuilder title = new StringBuilder();
        for (String word : structureType.split("_")) {
            if (word.isEmpty()) {
                continue;
            }
            if (title.length() > 0) {
                title.append(' ');
            }
            title.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                title.append(word.substring(1));
            }
        }
        return title.toString();
    }
}
