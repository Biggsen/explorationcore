package dev.explorationcore.book;

import dev.explorationcore.db.DiscoveryEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class JournalFixtures {
    public static final List<String> SCENARIOS = List.of("empty", "partial", "long");

    private JournalFixtures() {
    }

    public static String scenario(String name) {
        if (name == null) {
            return null;
        }
        for (String known : SCENARIOS) {
            if (known.equalsIgnoreCase(name)) {
                return known;
            }
        }
        return null;
    }

    public static PreparedBook book(String serverName, String scenario) {
        String known = scenario(scenario);
        if (known == null) {
            throw new IllegalArgumentException("Unknown book scenario " + scenario);
        }
        return switch (known) {
            case "empty" -> ExplorationBook.prepare(serverName, totals(), List.of());
            case "partial" -> ExplorationBook.prepare(serverName, totals(), partial());
            case "long" -> ExplorationBook.prepare(serverName, totals(), longList());
            default -> throw new IllegalArgumentException("Unknown book scenario " + scenario);
        };
    }

    private static ExplorationTotals totals() {
        return new ExplorationTotals(
                544,
                0,
                56,
                30,
                30,
                2,
                List.of(
                        new StructureTotal("ancient_city", "Ancient Cities", 12),
                        new StructureTotal("buried_treasure", "Buried Treasures", 53),
                        new StructureTotal("desert_well", "Desert Wells", 1),
                        new StructureTotal("jungle_temple", "Jungle Temples", 11),
                        new StructureTotal("ocean_ruin", "Ocean Ruins", 169),
                        new StructureTotal("pillager_outpost", "Pillager Outposts", 8),
                        new StructureTotal("shipwreck", "Shipwrecks", 126),
                        new StructureTotal("trail_ruins", "Trail Ruins", 19)
                ),
                18,
                8
        );
    }

    private static List<DiscoveryEntry> partial() {
        List<DiscoveryEntry> rows = new ArrayList<>();
        rows.add(entry("overworld", "region", "alpha", "Alpha"));
        rows.add(entry("overworld", "region", "alpine", "alpine"));
        rows.add(entry("overworld", "region", "beta", "beta"));
        rows.add(entry("overworld", "region", "sakonotur", "Sakonotur"));
        rows.add(entry("overworld", "region", "echo-z", "Echo"));
        rows.add(entry("overworld", "region", "echo-a", "Echo"));
        rows.add(entry("overworld", "village", "widecombe", "Widecombe"));
        rows.add(entry("overworld", "heart", "heart_of_sakonotur", "Sakonotur"));
        rows.add(entry("overworld", "soul", "soul_of_ash", "Ash"));
        rows.add(entry("overworld", "soul", "soul_of_bram", "Bram"));
        rows.add(entry("nether", "soul", "soul_of_cinder", "Cinder Soul"));
        few(rows, "ancient_city", "city", "Deep Hall", "Deep Gallery", "Deep Vault");
        few(rows, "buried_treasure", "treasure", "Buried Cache", "Sandy Cache", "Tide Cache");
        few(rows, "desert_well", "well", "Dry Well", "Dust Well", "Lone Well");
        few(rows, "jungle_temple", "temple", "Moss Temple", "Vine Temple", "Canopy Temple");
        few(rows, "ocean_ruin", "ruin", "Ocean Ruin", "Ocean Ruin", "Sunken Court");
        few(rows, "pillager_outpost", "outpost", "Banner Post", "Watch Post", "Raid Post");
        rows.add(entry("overworld", "structure", "wreck_minnow", "SS Minnow", "shipwreck"));
        for (int index = 1; index <= 40; index++) {
            String name = String.format(Locale.ROOT, "Shipwreck %02d", index);
            rows.add(entry("overworld", "structure", "fake-wreck-" + index, name, "shipwreck"));
        }
        few(rows, "trail_ruins", "trail", "Gravel Trail", "Old Trail", "Broken Trail");
        rows.add(entry("overworld", "structure", "blank_one", "Nameless", null));
        rows.add(entry("overworld", "structure", "mineshaft_1", "Hidden Shaft", "mineshaft"));
        rows.add(entry("nether", "region", "sakonotur", "Sakonotur"));
        rows.add(entry("nether", "region", "ashen", "Ashen Veins"));
        rows.add(entry("nether", "heart", "ash-heart", "Ash Heart"));
        rows.add(entry("nether", "heart", "cinder-heart", "Cinder Heart"));
        rows.add(entry("nether", "heart", "vein-heart", "Vein Heart"));
        rows.add(entry("nether", "village", "ignored", "Ignored Village"));
        return rows;
    }

    private static List<DiscoveryEntry> longList() {
        List<DiscoveryEntry> rows = new ArrayList<>();
        for (int index = 1; index <= 20; index++) {
            String name = String.format(Locale.ROOT, "Long Region %02d", index);
            rows.add(entry("overworld", "region", "region-" + index, name));
        }
        rows.add(entry("overworld", "village", "widecombe", "Widecombe"));
        rows.add(entry("overworld", "village", "millhaven", "Millhaven"));
        rows.add(entry("overworld", "heart", "heart_of_sakonotur", "Sakonotur"));
        for (int index = 1; index <= 15; index++) {
            String name = String.format(Locale.ROOT, "Long Wreck %02d", index);
            rows.add(entry("overworld", "structure", "wreck-" + index, name, "shipwreck"));
        }
        rows.add(entry("overworld", "structure", "blank_one", "Nameless", null));
        rows.add(entry("overworld", "structure", "mineshaft_1", "Hidden Shaft", "mineshaft"));
        rows.add(entry("nether", "region", "ashen", "Ashen Veins"));
        return rows;
    }

    private static void few(List<DiscoveryEntry> rows, String structureType, String idPrefix, String... names) {
        for (int index = 0; index < names.length; index++) {
            rows.add(entry("overworld", "structure", idPrefix + "-" + (index + 1), names[index], structureType));
        }
    }

    private static DiscoveryEntry entry(String world, String type, String id, String name) {
        return entry(world, type, id, name, null);
    }

    private static DiscoveryEntry entry(String world, String type, String id, String name, String structureType) {
        return new DiscoveryEntry(world, type, structureType, id, name);
    }
}
