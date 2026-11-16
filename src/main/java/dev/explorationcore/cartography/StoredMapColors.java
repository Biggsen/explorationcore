package dev.explorationcore.cartography;

import org.bukkit.map.MapView;

import java.lang.reflect.Field;
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
        if (view == null) {
            return null;
        }
        try {
            Object saved = field(view.getClass(), "worldMap").get(view);
            if (saved == null) {
                return null;
            }
            Object value = field(saved.getClass(), "colors").get(saved);
            if (value instanceof byte[] bytes && bytes.length == MapCoverage.PIXELS) {
                return bytes;
            }
            return null;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return null;
        }
    }

    private static Field field(Class<?> type, String name) throws NoSuchFieldException {
        Class<?> current = type;
        while (current != null) {
            try {
                Field found = current.getDeclaredField(name);
                found.setAccessible(true);
                return found;
            } catch (NoSuchFieldException exception) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }
}
