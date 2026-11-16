package dev.explorationcore.cartography;

import dev.explorationcore.location.LocationIndex;

import java.util.List;

/**
 * Picks the amp under a lodestone from WorldGuard region ids, highest priority first.
 * The names and kinds come from the location index. A region, village, or structure is not an amp.
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
            if (entry == null || !isAmp(entry.kind()) || entry.name() == null || entry.name().isBlank()) {
                continue;
            }
            return new Amp(regionId, entry.kind(), entry.name());
        }
        return null;
    }

    public static boolean isAmp(String kind) {
        return "heart".equals(kind) || "nerve".equals(kind) || "soul".equals(kind);
    }

    public record Amp(String id, String kind, String name) {
    }
}
