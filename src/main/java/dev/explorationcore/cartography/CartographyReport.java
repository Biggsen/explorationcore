package dev.explorationcore.cartography;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.UUID;

public final class CartographyReport {
    private CartographyReport() {
    }

    public static List<String> lines(Inspection inspection) {
        MapGrid.Tile tile = MapGrid.tile(inspection.scale(), inspection.centerX(), inspection.centerZ());
        List<String> lines = new ArrayList<>();
        lines.add("Cartography Test");
        lines.add("Map ID: " + inspection.mapId());
        lines.add("World: " + shown(inspection.worldName()));
        lines.add("Dimension: " + shown(inspection.dimension()));
        lines.add("Scale: " + inspection.scale());
        lines.add("Centre: " + inspection.centerX() + ", " + inspection.centerZ());
        lines.add("Grid: " + tile.x() + ", " + tile.z());
        lines.add("Aligned: " + yesNo(tile.aligned()));
        lines.add("Locked: " + yesNo(inspection.locked()));
        lines.add(coverage(inspection.explored()));
        lines.add("Required: 98%");
        lines.add("Same map id: " + sameId(inspection));
        lines.add("Same grid: " + sameGrid(inspection, tile));
        lines.add("Valid: " + yesNo(valid(inspection, tile)));
        return List.copyOf(lines);
    }

    static String dimensionName(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        if (key.equals("minecraft:overworld")) {
            return "overworld";
        }
        if (key.equals("minecraft:the_nether")) {
            return "nether";
        }
        if (key.equals("minecraft:the_end")) {
            return "the_end";
        }
        return key;
    }

    private static boolean valid(Inspection inspection, MapGrid.Tile tile) {
        if (inspection.explored().isEmpty() || inspection.virtual() || inspection.worldId() == null) {
            return false;
        }
        if (!known(inspection.worldName()) || !known(inspection.dimension())) {
            return false;
        }
        if (inspection.scale() != MapGrid.REQUIRED_SCALE || !tile.aligned()) {
            return false;
        }
        return MapCoverage.meetsRequired(inspection.explored().getAsInt());
    }

    private static String sameId(Inspection inspection) {
        List<String> places = new ArrayList<>();
        if (inspection.heldCount() > 1) {
            places.add("held stack x" + inspection.heldCount());
        }
        for (Sighting other : inspection.others()) {
            if (other.mapId() == inspection.mapId()) {
                places.add(other.place());
            }
        }
        return places.isEmpty() ? "none" : String.join(", ", places);
    }

    private static String sameGrid(Inspection inspection, MapGrid.Tile tile) {
        if (inspection.worldId() == null || !known(inspection.dimension())) {
            return "none";
        }
        Map<Integer, List<String>> places = new LinkedHashMap<>();
        for (Sighting other : inspection.others()) {
            if (other.mapId() == inspection.mapId() || other.worldId() == null || !known(other.dimension())) {
                continue;
            }
            if (!inspection.worldId().equals(other.worldId()) || !inspection.dimension().equals(other.dimension())) {
                continue;
            }
            MapGrid.Tile otherTile = MapGrid.tile(other.scale(), other.centerX(), other.centerZ());
            if (other.scale() != inspection.scale() || otherTile.x() != tile.x() || otherTile.z() != tile.z()) {
                continue;
            }
            places.computeIfAbsent(other.mapId(), id -> new ArrayList<>()).add(other.place());
        }
        if (places.isEmpty()) {
            return "none";
        }
        List<String> parts = new ArrayList<>();
        for (Map.Entry<Integer, List<String>> entry : places.entrySet()) {
            parts.add("map " + entry.getKey() + " (" + String.join(", ", entry.getValue()) + ")");
        }
        return String.join(", ", parts);
    }

    private static String coverage(OptionalInt explored) {
        if (explored.isEmpty()) {
            return "Coverage: unavailable";
        }
        int count = explored.getAsInt();
        return "Coverage: " + MapCoverage.percent(count) + " (" + count + "/" + MapCoverage.PIXELS + ")";
    }

    private static boolean known(String value) {
        return value != null && !value.isBlank();
    }

    private static String shown(String value) {
        return known(value) ? value : "unknown";
    }

    private static String yesNo(boolean value) {
        return value ? "Yes" : "No";
    }

    public record Inspection(
            int mapId,
            String worldName,
            UUID worldId,
            String dimension,
            int scale,
            int centerX,
            int centerZ,
            boolean locked,
            boolean virtual,
            OptionalInt explored,
            int heldCount,
            List<Sighting> others
    ) {
    }

    public record Sighting(
            int mapId,
            String place,
            UUID worldId,
            String dimension,
            int scale,
            int centerX,
            int centerZ
    ) {
    }
}
