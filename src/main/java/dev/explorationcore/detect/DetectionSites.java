package dev.explorationcore.detect;

import org.yaml.snakeyaml.Yaml;

import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class DetectionSites {
    private DetectionSites() {
    }

    public static DetectionCatalogue parse(String yaml) {
        return parse(new StringReader(yaml));
    }

    public static DetectionCatalogue parse(Reader reader) {
        Object loaded = new Yaml().load(reader);
        if (!(loaded instanceof Map<?, ?> root)) {
            throw new IllegalArgumentException("detection sites root must be a map");
        }
        Object rawSites = root.get("sites");
        if (!(rawSites instanceof List<?> rows)) {
            throw new IllegalArgumentException("detection sites must be a list");
        }
        List<DetectionSite> sites = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        for (int index = 0; index < rows.size(); index++) {
            sites.add(site(rows.get(index), index, ids));
        }
        return new DetectionCatalogue(sites);
    }

    private static DetectionSite site(Object raw, int index, Set<String> ids) {
        if (!(raw instanceof Map<?, ?> fields)) {
            throw new IllegalArgumentException("detection site at index " + index + " must be a map");
        }
        String id = token(fields.get("id"), index);
        if (!ids.add(id)) {
            throw new IllegalArgumentException("duplicate detection site " + id);
        }
        String type = kind(fields.get("type"), id);
        world(fields.get("world"), id);
        int x = coordinate(fields.get("x"), id, "x");
        int y = coordinate(fields.get("y"), id, "y");
        int z = coordinate(fields.get("z"), id, "z");
        return new DetectionSite(id, type, x, y, z);
    }

    private static String token(Object raw, int index) {
        if (!(raw instanceof String value) || !singleToken(value.trim())) {
            throw new IllegalArgumentException(
                    "detection site at index " + index + " id must be a non-empty token without whitespace"
            );
        }
        return value.trim();
    }

    private static String kind(Object raw, String id) {
        if (!(raw instanceof String value)) {
            throw new IllegalArgumentException("unknown detection site type for " + id);
        }
        String type = value.trim().toLowerCase(Locale.ROOT);
        if (!"heart".equals(type) && !"nerve".equals(type) && !"soul".equals(type)) {
            throw new IllegalArgumentException("unknown detection site type: " + value.trim());
        }
        return type;
    }

    private static void world(Object raw, String id) {
        if (!(raw instanceof String value)) {
            throw new IllegalArgumentException("unknown detection site world for " + id);
        }
        String world = value.trim().toLowerCase(Locale.ROOT);
        if (!"overworld".equals(world)) {
            throw new IllegalArgumentException("unknown detection site world: " + value.trim());
        }
    }

    private static int coordinate(Object raw, String id, String axis) {
        if (!(raw instanceof Number number)) {
            throw new IllegalArgumentException("detection site " + id + " coordinate " + axis + " is missing");
        }
        double value = number.doubleValue();
        if (!Double.isFinite(value) || value != Math.rint(value) || value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("detection site " + id + " coordinate " + axis + " must be a block coordinate");
        }
        return (int) value;
    }

    private static boolean singleToken(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            if (Character.isWhitespace(value.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
