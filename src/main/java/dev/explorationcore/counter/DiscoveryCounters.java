package dev.explorationcore.counter;

import dev.explorationcore.db.PlayerTally;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class DiscoveryCounters {
    private static final Set<String> MAIN = Set.of(
            "total_discovered",
            "regions_discovered",
            "villages_discovered",
            "hearts_discovered",
            "nerves_discovered",
            "nether_regions_discovered",
            "nether_hearts_discovered",
            "structures_found"
    );

    private static final Map<String, String> FAMILIES = Map.ofEntries(
            Map.entry("ancient_cities_found", "ancient_city"),
            Map.entry("buried_treasures_found", "buried_treasure"),
            Map.entry("desert_pyramids_found", "desert_pyramid"),
            Map.entry("desert_wells_found", "desert_well"),
            Map.entry("igloos_found", "igloo"),
            Map.entry("jungle_temples_found", "jungle_temple"),
            Map.entry("ocean_ruins_found", "ocean_ruin"),
            Map.entry("pillager_outposts_found", "pillager_outpost"),
            Map.entry("ruined_portals_found", "ruined_portal"),
            Map.entry("shipwrecks_found", "shipwreck"),
            Map.entry("trail_ruins_found", "trail_ruins"),
            Map.entry("swamp_huts_found", "swamp_hut"),
            Map.entry("woodland_mansions_found", "woodland_mansion")
    );

    private static final Set<String> NAMES = namesOf(MAIN, FAMILIES);

    private DiscoveryCounters() {
    }

    public static boolean known(String name) {
        return name != null && NAMES.contains(name);
    }

    public static Set<String> names() {
        return NAMES;
    }

    public static List<String> affected(String world, String entityType, String structureType) {
        List<String> names = new ArrayList<>();
        switch (entityType == null ? "" : entityType) {
            case "region" -> {
                if ("overworld".equals(world)) {
                    names.add("regions_discovered");
                } else if ("nether".equals(world)) {
                    names.add("nether_regions_discovered");
                }
            }
            case "village" -> names.add("villages_discovered");
            case "heart" -> {
                if ("overworld".equals(world)) {
                    names.add("hearts_discovered");
                } else if ("nether".equals(world)) {
                    names.add("nether_hearts_discovered");
                }
            }
            case "nerve" -> {
                if ("overworld".equals(world)) {
                    names.add("nerves_discovered");
                }
            }
            case "structure" -> {
                names.add("structures_found");
                String family = familyCounter(structureType);
                if (family != null) {
                    names.add(family);
                }
            }
            default -> {
            }
        }
        if (countsTowardTotal(world, entityType)) {
            names.add("total_discovered");
        }
        return List.copyOf(names);
    }

    public static long count(List<PlayerTally> tallies, String name) {
        if (!known(name)) {
            throw new IllegalArgumentException("unknown counter: " + name);
        }
        return switch (name) {
            case "total_discovered" -> sum(tallies, "overworld", "region")
                    + sumType(tallies, "village")
                    + sum(tallies, "overworld", "heart")
                    + sum(tallies, "overworld", "nerve")
                    + sum(tallies, "nether", "region")
                    + sum(tallies, "nether", "heart");
            case "regions_discovered" -> sum(tallies, "overworld", "region");
            case "villages_discovered" -> sumType(tallies, "village");
            case "hearts_discovered" -> sum(tallies, "overworld", "heart");
            case "nerves_discovered" -> sum(tallies, "overworld", "nerve");
            case "nether_regions_discovered" -> sum(tallies, "nether", "region");
            case "nether_hearts_discovered" -> sum(tallies, "nether", "heart");
            case "structures_found" -> sumType(tallies, "structure");
            default -> sumStructure(tallies, FAMILIES.get(name));
        };
    }

    private static String familyCounter(String structureType) {
        if (structureType == null || structureType.isBlank()) {
            return null;
        }
        for (Map.Entry<String, String> entry : FAMILIES.entrySet()) {
            if (entry.getValue().equals(structureType)) {
                return entry.getKey();
            }
        }
        return null;
    }

    private static boolean countsTowardTotal(String world, String entityType) {
        return switch (entityType == null ? "" : entityType) {
            case "village" -> true;
            case "region", "heart" -> "overworld".equals(world) || "nether".equals(world);
            case "nerve" -> "overworld".equals(world);
            default -> false;
        };
    }

    private static Set<String> namesOf(Set<String> main, Map<String, String> families) {
        Set<String> names = new HashSet<>(main);
        names.addAll(families.keySet());
        return Set.copyOf(names);
    }

    private static long sum(List<PlayerTally> tallies, String world, String entityType) {
        long total = 0;
        for (PlayerTally tally : tallies) {
            if (world.equals(tally.world()) && entityType.equals(tally.entityType())) {
                total += tally.count();
            }
        }
        return total;
    }

    private static long sumType(List<PlayerTally> tallies, String entityType) {
        long total = 0;
        for (PlayerTally tally : tallies) {
            if (entityType.equals(tally.entityType())) {
                total += tally.count();
            }
        }
        return total;
    }

    private static long sumStructure(List<PlayerTally> tallies, String structureType) {
        long total = 0;
        for (PlayerTally tally : tallies) {
            if ("structure".equals(tally.entityType()) && structureType.equals(tally.structureType())) {
                total += tally.count();
            }
        }
        return total;
    }
}
