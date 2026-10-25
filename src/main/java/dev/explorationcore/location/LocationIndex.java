package dev.explorationcore.location;

import java.util.Collections;
import java.util.Map;

/** World folder, then WorldGuard region id, then the location entry. */
public final class LocationIndex {
    private final Map<String, Map<String, Entry>> worlds;

    public LocationIndex(Map<String, Map<String, Entry>> worlds) {
        this.worlds = Collections.unmodifiableMap(worlds);
    }

    public static LocationIndex empty() {
        return new LocationIndex(Map.of());
    }

    public Entry get(String world, String regionId) {
        if (world == null || regionId == null) {
            return null;
        }
        Map<String, Entry> regions = worlds.get(world);
        if (regions == null) {
            return null;
        }
        return regions.get(regionId);
    }

    public record Entry(String kind, String name, String color) {}
}
