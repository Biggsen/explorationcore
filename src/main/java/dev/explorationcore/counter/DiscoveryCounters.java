package dev.explorationcore.counter;

import dev.explorationcore.db.PlayerTally;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class DiscoveryCounters {
    private static final Set<String> MAIN = Set.of(
            "total_discovered",
            "regions_discovered",
            "water_regions_discovered",
            "villages_discovered",
            "hearts_discovered",
            "nerves_discovered",
            "nether_regions_discovered",
            "nether_hearts_discovered",
            "structures_found"
    );

    private static final Map<String, String> BUILTIN = Map.ofEntries(
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

    private final Map<String, String> families;
    private final Map<String, String> countersByType;
    private final Set<String> names;
    private final boolean accepted;

    private DiscoveryCounters(Map<String, String> families, boolean accepted) {
        this.families = Map.copyOf(families);
        Map<String, String> byType = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : this.families.entrySet()) {
            byType.put(entry.getValue(), entry.getKey());
        }
        this.countersByType = Map.copyOf(byType);
        Set<String> known = new HashSet<>(MAIN);
        known.addAll(this.families.keySet());
        this.names = Set.copyOf(known);
        this.accepted = accepted;
    }

    public static DiscoveryCounters builtin() {
        return new DiscoveryCounters(BUILTIN, true);
    }

    public static DiscoveryCounters none() {
        return new DiscoveryCounters(Map.of(), false);
    }

    public static DiscoveryCounters load(Path file, Logger logger) {
        if (!Files.isRegularFile(file)) {
            return reject(logger, "structure-counters.yml is missing.");
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return load(reader, logger);
        } catch (IOException exception) {
            return reject(logger, "structure-counters.yml could not be read.", exception);
        }
    }

    public static DiscoveryCounters load(Reader reader, Logger logger) {
        Object loaded;
        try {
            loaded = new Yaml().load(reader);
        } catch (RuntimeException exception) {
            return reject(logger, "structure-counters.yml is malformed.", exception);
        }
        if (!(loaded instanceof Map<?, ?> rawRoot)) {
            return reject(logger, "structure-counters.yml has no structures section.");
        }
        if (!hasStringKey(rawRoot, "structures")) {
            return reject(logger, "structure-counters.yml has no structures section.");
        }
        Object section = value(rawRoot, "structures");
        if (!(section instanceof Map<?, ?> rawStructures)) {
            return reject(logger, "structures is malformed.");
        }
        Map<String, String> families = new LinkedHashMap<>();
        Set<String> seenCounters = new HashSet<>();
        for (Map.Entry<?, ?> entry : rawStructures.entrySet()) {
            if (!(entry.getKey() instanceof String type) || !token(type)) {
                String shown = entry.getKey() == null ? "" : entry.getKey().toString();
                return reject(logger, "structures." + shown + " is not a lowercase token.");
            }
            if (!(entry.getValue() instanceof String counter)) {
                return reject(logger, "structures." + type + " counter must be text.");
            }
            if (!token(counter)) {
                return reject(logger, "structures." + type + " counter " + counter + " is not a lowercase token.");
            }
            if (MAIN.contains(counter)) {
                return reject(logger, "structures." + type + " counter " + counter + " is a main discovery counter.");
            }
            if (!seenCounters.add(counter)) {
                return reject(logger, "structures." + type + " counter " + counter + " is duplicated.");
            }
            families.put(counter, type);
        }
        return new DiscoveryCounters(families, true);
    }

    public boolean accepted() {
        return accepted;
    }

    public int familyCount() {
        return families.size();
    }

    public boolean known(String name) {
        return name != null && names.contains(name);
    }

    public Set<String> names() {
        return names;
    }

    public List<String> affected(String world, String entityType, String structureType) {
        List<String> affected = new ArrayList<>();
        switch (entityType == null ? "" : entityType) {
            case "region" -> {
                if ("overworld".equals(world)) {
                    affected.add("regions_discovered");
                } else if ("nether".equals(world)) {
                    affected.add("nether_regions_discovered");
                }
            }
            case "water" -> {
                if ("overworld".equals(world)) {
                    affected.add("water_regions_discovered");
                }
            }
            case "village" -> affected.add("villages_discovered");
            case "heart" -> {
                if ("overworld".equals(world)) {
                    affected.add("hearts_discovered");
                } else if ("nether".equals(world)) {
                    affected.add("nether_hearts_discovered");
                }
            }
            case "nerve" -> {
                if ("overworld".equals(world)) {
                    affected.add("nerves_discovered");
                }
            }
            case "structure" -> {
                affected.add("structures_found");
                if (structureType != null && !structureType.isBlank()) {
                    String family = countersByType.get(structureType);
                    if (family != null) {
                        affected.add(family);
                    }
                }
            }
            default -> {
            }
        }
        if (countsTowardTotal(world, entityType)) {
            affected.add("total_discovered");
        }
        return List.copyOf(affected);
    }

    public long count(List<PlayerTally> tallies, String name) {
        if (!known(name)) {
            throw new IllegalArgumentException("unknown counter: " + name);
        }
        return switch (name) {
            case "total_discovered" -> sum(tallies, "overworld", "region")
                    + sum(tallies, "overworld", "water")
                    + sumType(tallies, "village")
                    + sum(tallies, "overworld", "heart")
                    + sum(tallies, "overworld", "nerve")
                    + sum(tallies, "nether", "region")
                    + sum(tallies, "nether", "heart");
            case "regions_discovered" -> sum(tallies, "overworld", "region");
            case "water_regions_discovered" -> sum(tallies, "overworld", "water");
            case "villages_discovered" -> sumType(tallies, "village");
            case "hearts_discovered" -> sum(tallies, "overworld", "heart");
            case "nerves_discovered" -> sum(tallies, "overworld", "nerve");
            case "nether_regions_discovered" -> sum(tallies, "nether", "region");
            case "nether_hearts_discovered" -> sum(tallies, "nether", "heart");
            case "structures_found" -> sumType(tallies, "structure");
            default -> sumStructure(tallies, families.get(name));
        };
    }

    private static boolean countsTowardTotal(String world, String entityType) {
        return switch (entityType == null ? "" : entityType) {
            case "village" -> true;
            case "region", "heart" -> "overworld".equals(world) || "nether".equals(world);
            case "water", "nerve" -> "overworld".equals(world);
            default -> false;
        };
    }

    private static boolean token(String value) {
        if (value.isEmpty()) {
            return false;
        }
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (!((character >= 'a' && character <= 'z') || (character >= '0' && character <= '9') || character == '_')) {
                return false;
            }
        }
        return true;
    }

    private static boolean hasStringKey(Map<?, ?> map, String key) {
        for (Object candidate : map.keySet()) {
            if (key.equals(candidate)) {
                return true;
            }
        }
        return false;
    }

    private static Object value(Map<?, ?> map, String key) {
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (key.equals(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
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

    private static DiscoveryCounters reject(Logger logger, String message) {
        logger.severe(message);
        return none();
    }

    private static DiscoveryCounters reject(Logger logger, String message, Exception exception) {
        logger.log(Level.SEVERE, message, exception);
        return none();
    }
}
