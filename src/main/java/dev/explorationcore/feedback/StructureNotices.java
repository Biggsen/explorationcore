package dev.explorationcore.feedback;

public final class StructureNotices {
    private StructureNotices() {
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
