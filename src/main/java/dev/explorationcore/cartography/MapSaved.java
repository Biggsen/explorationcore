package dev.explorationcore.cartography;

import java.lang.reflect.Field;

final class MapSaved {
    private MapSaved() {
    }

    static Object data(Object view) {
        if (view == null) {
            return null;
        }
        try {
            return field(view.getClass(), "worldMap").get(view);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return null;
        }
    }

    static Field field(Class<?> type, String name) throws NoSuchFieldException {
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
