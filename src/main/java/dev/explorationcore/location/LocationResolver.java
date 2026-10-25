package dev.explorationcore.location;

import java.util.List;

/**
 * Turns a world name and region ids, already sorted highest WorldGuard priority first, into one
 * placeholder string. The caller has dropped {@code __global__}.
 */
public final class LocationResolver {
    public static final String UNRESOLVED = "-";
    public static final String CURRENT_REGION = "current_region";
    public static final String CURRENT_VILLAGE = "current_village";
    public static final String CURRENT_STRUCTURE = "current_structure";

    private LocationResolver() {}

    public static boolean isKnown(String params) {
        return CURRENT_REGION.equals(params) || CURRENT_VILLAGE.equals(params) || CURRENT_STRUCTURE.equals(params);
    }

    public static String resolve(
            LocationIndex index, String world, List<String> regionIdsHighestPriorityFirst, String params) {
        if (!isKnown(params)) {
            return null;
        }
        if (index == null || regionIdsHighestPriorityFirst == null || regionIdsHighestPriorityFirst.isEmpty()) {
            return UNRESOLVED;
        }
        String innermostId = regionIdsHighestPriorityFirst.get(0);
        String parentId =
                regionIdsHighestPriorityFirst.size() >= 2 ? regionIdsHighestPriorityFirst.get(1) : innermostId;
        return switch (params) {
            case CURRENT_REGION -> formatRegion(index.get(world, parentId));
            case CURRENT_VILLAGE -> formatKind(index.get(world, innermostId), "village");
            case CURRENT_STRUCTURE -> formatKind(index.get(world, innermostId), "structure");
            default -> null;
        };
    }

    private static String formatRegion(LocationIndex.Entry entry) {
        if (entry == null || entry.name() == null) {
            return UNRESOLVED;
        }
        if (!"region".equals(entry.kind()) && !"water".equals(entry.kind()) && !"system".equals(entry.kind())) {
            return UNRESOLVED;
        }
        if (entry.color() == null || entry.color().isEmpty()) {
            return entry.name();
        }
        return entry.color() + entry.name();
    }

    private static String formatKind(LocationIndex.Entry entry, String kind) {
        if (entry == null || entry.name() == null || !kind.equals(entry.kind())) {
            return UNRESOLVED;
        }
        return entry.name();
    }
}
