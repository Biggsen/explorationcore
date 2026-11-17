package dev.explorationcore.cartography;

import dev.explorationcore.location.LocationIndex;

import java.util.List;

/**
 * Picks the location under a lodestone from WorldGuard region ids, highest priority first.
 * Hearts, nerves, souls, villages, and structures can be marked. Land, water, and system
 * regions are skipped. A village is shown with "Village" after its name.
 */
public final class AmpSelection {
    private AmpSelection() {
    }

    public static Amp select(LocationIndex index, String world, List<String> regionIdsHighestPriorityFirst) {
        if (index == null || regionIdsHighestPriorityFirst == null) {
            return null;
        }
        for (String regionId : regionIdsHighestPriorityFirst) {
            LocationIndex.Entry entry = index.get(world, regionId);
            if (entry == null || !markable(entry.kind()) || entry.name() == null || entry.name().isBlank()) {
                continue;
            }
            return new Amp(regionId, entry.kind(), displayName(entry.kind(), entry.name()));
        }
        return null;
    }

    public static boolean isAmp(String kind) {
        return "heart".equals(kind) || "nerve".equals(kind) || "soul".equals(kind);
    }

    private static boolean markable(String kind) {
        return isAmp(kind) || "village".equals(kind) || "structure".equals(kind);
    }

    private static String displayName(String kind, String name) {
        if ("village".equals(kind) && !name.endsWith(" Village")) {
            return name + " Village";
        }
        return name;
    }

    public record Amp(String id, String kind, String name) {
    }
}
