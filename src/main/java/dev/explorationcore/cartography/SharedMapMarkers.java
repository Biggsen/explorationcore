package dev.explorationcore.cartography;

import net.kyori.adventure.text.Component;
import org.bukkit.World;
import org.bukkit.map.MapView;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Writes one decoration onto the shared map data. Copies that share the map id
 * read the same decoration. Banner records are left untouched, because vanilla
 * deletes a banner whose block is not a banner.
 */
public final class SharedMapMarkers {
    static final String PREFIX = "ec:";

    private SharedMapMarkers() {
    }

    public enum Outcome {
        ADDED,
        ALREADY,
        REMOVED,
        UNAVAILABLE
    }

    public static Outcome mark(MapView view, AmpSelection.Amp amp, int x, int y, int z, World world) {
        if (view == null || amp == null || amp.id() == null || amp.id().isBlank()) {
            return Outcome.UNAVAILABLE;
        }
        String dye = dye(amp.kind());
        if (dye == null) {
            return Outcome.UNAVAILABLE;
        }
        Object saved = MapSaved.data(view);
        if (saved == null) {
            return Outcome.UNAVAILABLE;
        }
        return markSaved(saved, amp.id(), dye, amp.name(), x, y, z, level(world));
    }

    public static Outcome unmark(MapView view, String id) {
        if (view == null || id == null || id.isBlank()) {
            return Outcome.UNAVAILABLE;
        }
        Object saved = MapSaved.data(view);
        if (saved == null) {
            return Outcome.UNAVAILABLE;
        }
        return unmarkSaved(saved, id);
    }

    public static Optional<List<CartographyReport.AmpMarker>> read(MapView view) {
        Object saved = MapSaved.data(view);
        if (saved == null) {
            return Optional.empty();
        }
        return readSaved(saved);
    }

    static Outcome markSaved(
            Object saved,
            String id,
            String dye,
            String name,
            int x,
            int y,
            int z,
            Object level
    ) {
        Map<String, Object> decorations = decorations(saved);
        if (decorations == null) {
            return Outcome.UNAVAILABLE;
        }
        String key = PREFIX + id;
        if (decorations.containsKey(key)) {
            return Outcome.ALREADY;
        }
        Object banner;
        try {
            banner = createBanner(x, y, z, dye, name);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return Outcome.UNAVAILABLE;
        }
        try {
            decorate(saved, banner, key, x, z, name, level);
            return Outcome.ADDED;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            removeDecoration(saved, key);
            return Outcome.UNAVAILABLE;
        }
    }

    static Outcome unmarkSaved(Object saved, String id) {
        Map<String, Object> decorations = decorations(saved);
        if (decorations == null) {
            return Outcome.UNAVAILABLE;
        }
        String key = PREFIX + id;
        try {
            Method method = removeDecoration(saved.getClass());
            if (method == null) {
                decorations.remove(key);
            } else {
                method.setAccessible(true);
                method.invoke(saved, key);
            }
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return Outcome.UNAVAILABLE;
        }
        return Outcome.REMOVED;
    }

    static Optional<List<CartographyReport.AmpMarker>> readSaved(Object saved) {
        Map<String, Object> decorations = decorations(saved);
        if (decorations == null) {
            return Optional.empty();
        }
        List<CartographyReport.AmpMarker> markers = new ArrayList<>();
        for (Map.Entry<String, Object> entry : decorations.entrySet()) {
            String key = entry.getKey();
            if (key == null || !key.startsWith(PREFIX)) {
                continue;
            }
            try {
                markers.add(marker(key, entry.getValue()));
            } catch (ReflectiveOperationException | RuntimeException exception) {
                return Optional.empty();
            }
        }
        markers.sort(Comparator.comparing(CartographyReport.AmpMarker::id));
        return Optional.of(markers);
    }

    private static String dye(String kind) {
        return switch (kind) {
            case "heart" -> "GREEN";
            case "nerve" -> "RED";
            case "soul" -> "WHITE";
            case "village" -> "YELLOW";
            case "structure" -> "GRAY";
            default -> null;
        };
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> decorations(Object saved) {
        try {
            Object value = MapSaved.field(saved.getClass(), "decorations").get(saved);
            if (value instanceof Map<?, ?> map) {
                return (Map<String, Object>) map;
            }
            return null;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return null;
        }
    }

    private static Object createBanner(int x, int y, int z, String dyeName, String caption)
            throws ReflectiveOperationException {
        Class<?> blockPosType = Class.forName("net.minecraft.core.BlockPos");
        Object pos = blockPosType.getConstructor(int.class, int.class, int.class).newInstance(x, y, z);
        Object dye = dyeValue(dyeName);
        Object text = vanillaText(caption);
        Class<?> bannerType = Class.forName("net.minecraft.world.level.saveddata.maps.MapBanner");
        for (Constructor<?> constructor : bannerType.getDeclaredConstructors()) {
            Class<?>[] parameters = constructor.getParameterTypes();
            if (parameters.length == 3 && parameters[2] == Optional.class) {
                constructor.setAccessible(true);
                return constructor.newInstance(pos, dye, Optional.of(text));
            }
        }
        for (Constructor<?> constructor : bannerType.getDeclaredConstructors()) {
            if (constructor.getParameterCount() == 3) {
                constructor.setAccessible(true);
                return constructor.newInstance(pos, dye, text);
            }
        }
        throw new NoSuchMethodException("MapBanner");
    }

    private static Object dyeValue(String dyeName) throws ReflectiveOperationException {
        Class<?> dyeType = Class.forName("net.minecraft.world.item.DyeColor");
        return dyeType.getMethod("valueOf", String.class).invoke(null, dyeName);
    }

    private static void decorate(
            Object saved,
            Object banner,
            String key,
            int x,
            int z,
            String caption,
            Object level
    ) throws ReflectiveOperationException {
        Object type = call(banner, "getDecoration");
        if (type == null) {
            type = call(banner, "decoration");
        }
        if (type == null) {
            throw new NoSuchMethodException("getDecoration");
        }
        Method method = addDecoration(saved.getClass());
        method.setAccessible(true);
        method.invoke(saved, type, level, key, (double) x, (double) z, 180.0, vanillaText(caption));
    }

    private static Method removeDecoration(Class<?> type) {
        Class<?> current = type;
        while (current != null) {
            for (Method method : current.getDeclaredMethods()) {
                Class<?>[] parameters = method.getParameterTypes();
                if (parameters.length == 1
                        && method.getName().equals("removeDecoration")
                        && parameters[0] == String.class) {
                    return method;
                }
            }
            current = current.getSuperclass();
        }
        return null;
    }

    private static Method addDecoration(Class<?> type) throws NoSuchMethodException {
        Class<?> current = type;
        while (current != null) {
            for (Method method : current.getDeclaredMethods()) {
                Class<?>[] parameters = method.getParameterTypes();
                if (parameters.length == 7
                        && method.getName().equals("addDecoration")
                        && parameters[2] == String.class
                        && parameters[3] == double.class
                        && parameters[4] == double.class
                        && parameters[5] == double.class) {
                    return method;
                }
            }
            current = current.getSuperclass();
        }
        throw new NoSuchMethodException("addDecoration");
    }

    @SuppressWarnings("unchecked")
    private static void removeDecoration(Object saved, String key) {
        try {
            Object decorations = MapSaved.field(saved.getClass(), "decorations").get(saved);
            if (decorations instanceof Map<?, ?> map) {
                ((Map<Object, Object>) map).remove(key);
            }
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return;
        }
    }

    private static Object vanillaText(String caption) throws ReflectiveOperationException {
        Class<?> paper = Class.forName("io.papermc.paper.adventure.PaperAdventure");
        Method asVanilla = paper.getMethod("asVanilla", Component.class);
        return asVanilla.invoke(null, Component.text(caption));
    }

    private static Object level(World world) {
        if (world == null) {
            return null;
        }
        try {
            return world.getClass().getMethod("getHandle").invoke(world);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return null;
        }
    }

    private static CartographyReport.AmpMarker marker(String key, Object decoration) throws ReflectiveOperationException {
        Object type = call(decoration, "type");
        if (type == null) {
            type = call(decoration, "getType");
        }
        if (type == null) {
            throw new NoSuchMethodException("type");
        }
        return new CartographyReport.AmpMarker(key.substring(PREFIX.length()), kindOf(describe(type)));
    }

    private static String describe(Object type) throws ReflectiveOperationException {
        Object key = call(type, "unwrapKey");
        if (key instanceof Optional<?> optional) {
            key = optional.orElse(null);
        }
        if (key != null) {
            Object location = call(key, "location");
            if (location == null) {
                location = call(key, "getLocation");
            }
            if (location != null) {
                Object path = call(location, "getPath");
                if (path == null) {
                    path = call(location, "path");
                }
                if (path != null) {
                    return String.valueOf(path);
                }
                return String.valueOf(location);
            }
        }
        return String.valueOf(type);
    }

    private static String kindOf(String described) {
        if (described.contains("banner_green")) {
            return "heart";
        }
        if (described.contains("banner_red")) {
            return "nerve";
        }
        if (described.contains("banner_white")) {
            return "soul";
        }
        if (described.contains("banner_yellow")) {
            return "village";
        }
        if (described.contains("banner_gray")) {
            return "structure";
        }
        return described.toLowerCase(Locale.ROOT);
    }

    private static Object call(Object target, String name) throws ReflectiveOperationException {
        Method method = declared(target.getClass(), name);
        if (method == null) {
            return null;
        }
        method.setAccessible(true);
        return method.invoke(target);
    }

    private static Method declared(Class<?> type, String name) {
        Class<?> current = type;
        while (current != null) {
            for (Method method : current.getDeclaredMethods()) {
                if (method.getName().equals(name) && method.getParameterCount() == 0) {
                    return method;
                }
            }
            current = current.getSuperclass();
        }
        return null;
    }
}
