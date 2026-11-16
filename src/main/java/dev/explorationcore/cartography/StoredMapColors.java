package dev.explorationcore.cartography;

import org.bukkit.map.MapView;

import java.util.OptionalInt;

/**
 * Paper 1.21.11 keeps explored pixels on {@code CraftMapView.worldMap}
 * ({@code MapItemSavedData.colors}). The public map API does not expose that buffer.
 */
public final class StoredMapColors {
    private StoredMapColors() {
    }

    public static OptionalInt explored(MapView view) {
        return MapCoverage.explored(colors(view));
    }

    static byte[] colors(Object view) {
        Object saved = MapSaved.data(view);
        if (saved == null) {
            return null;
        }
        try {
            Object value = MapSaved.field(saved.getClass(), "colors").get(saved);
            if (value instanceof byte[] bytes && bytes.length == MapCoverage.PIXELS) {
                return bytes;
            }
            return null;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return null;
        }
    }
}
