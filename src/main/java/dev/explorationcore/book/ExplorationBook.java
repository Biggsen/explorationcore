package dev.explorationcore.book;

import dev.explorationcore.db.PlayerTally;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public final class ExplorationBook {
    public static final String TITLE = "Exploration";
    public static final String SUBTITLE = "Exploration Record";
    public static final int SERVER_NAME_COLOR = 0x986A27;
    public static final int BODY_COLOR = 0x704C2E;

    private ExplorationBook() {
    }

    public static PreparedBook prepare(
            String serverName,
            ExplorationTotals totals,
            List<PlayerTally> tallies
    ) {
        long overworldRegions = count(tallies, "overworld", "region");
        long villages = count(tallies, "overworld", "village");
        long hearts = count(tallies, "overworld", "heart");
        long nerves = count(tallies, "overworld", "nerve");
        long netherRegions = count(tallies, "nether", "region");
        long netherHearts = count(tallies, "nether", "heart");

        long structuresFound = 0;
        int structuresAvailable = 0;
        List<String> structureLines = new ArrayList<>();
        for (StructureTotal structure : totals.structures()) {
            long discovered = countStructure(tallies, structure.key());
            structuresFound += discovered;
            structuresAvailable += structure.total();
            structureLines.add(line(structureLabel(structure), discovered, structure.total()));
        }

        List<String> pages = new ArrayList<>();
        pages.add(String.join("\n",
                serverName == null ? "" : serverName,
                SUBTITLE,
                BookText.countedLine("World explored", percent(
                        overworldRegions + villages + hearts + nerves + netherRegions + netherHearts,
                        totals.overworldRegions() + totals.villages() + totals.hearts() + totals.nerves()
                                + totals.netherRegions() + totals.netherHearts()
                )),
                "",
                heading("Overworld",
                        overworldRegions + villages + hearts + nerves,
                        totals.overworldRegions() + totals.villages() + totals.hearts() + totals.nerves()),
                "",
                line("Regions", overworldRegions, totals.overworldRegions()),
                line("Villages", villages, totals.villages()),
                line("Hearts", hearts, totals.hearts()),
                line("Nerves", nerves, totals.nerves())
        ));
        if (!structureLines.isEmpty()) {
            structureLines.add(0, "");
            structureLines.add(0, heading("Structures", structuresFound, structuresAvailable));
        }
        pages.addAll(BookText.paginate(structureLines));
        pages.add(String.join("\n",
                heading("Nether",
                        netherRegions + netherHearts,
                        totals.netherRegions() + totals.netherHearts()),
                "",
                line("Regions", netherRegions, totals.netherRegions()),
                line("Hearts", netherHearts, totals.netherHearts())
        ));
        return new PreparedBook(pages, notices(totals, tallies));
    }

    private static List<String> notices(ExplorationTotals totals, List<PlayerTally> tallies) {
        Set<String> keys = new HashSet<>();
        for (StructureTotal structure : totals.structures()) {
            keys.add(structure.key());
        }
        long empty = 0;
        TreeMap<String, Long> unknown = new TreeMap<>();
        for (PlayerTally tally : tallies) {
            if (!"structure".equals(tally.entityType())) {
                continue;
            }
            String structureType = tally.structureType();
            if (structureType == null || structureType.isEmpty()) {
                empty += tally.count();
            } else if (!keys.contains(structureType)) {
                unknown.merge(structureType, tally.count(), Long::sum);
            }
        }
        List<String> notices = new ArrayList<>();
        if (empty > 0) {
            notices.add("Left out " + empty + " structure " + noun(empty) + " with an empty structure_type");
        }
        for (Map.Entry<String, Long> entry : unknown.entrySet()) {
            long count = entry.getValue();
            notices.add("Left out structure_type " + entry.getKey() + " (" + count + " " + noun(count) + "): no totals.yml entry");
        }
        return notices;
    }

    private static String noun(long count) {
        return count == 1 ? "discovery" : "discoveries";
    }

    private static long count(List<PlayerTally> tallies, String world, String entityType) {
        long total = 0;
        for (PlayerTally tally : tallies) {
            if (world.equals(tally.world()) && entityType.equals(tally.entityType())) {
                total += tally.count();
            }
        }
        return total;
    }

    private static long countStructure(List<PlayerTally> tallies, String key) {
        long total = 0;
        for (PlayerTally tally : tallies) {
            if ("overworld".equals(tally.world())
                    && "structure".equals(tally.entityType())
                    && key.equals(tally.structureType())) {
                total += tally.count();
            }
        }
        return total;
    }

    private static String structureLabel(StructureTotal structure) {
        return switch (structure.key()) {
            case "buried_treasure" -> "Treasures";
            case "woodland_mansion" -> "WL Mansions";
            default -> structure.name();
        };
    }

    private static String heading(String name, long discovered, int total) {
        return name + " (" + percent(discovered, total) + ")";
    }

    private static String line(String label, long discovered, int total) {
        return BookText.countedLine(label, discovered + "/" + total);
    }

    private static String percent(long discovered, int total) {
        if (total <= 0) {
            return "0%";
        }
        return Math.round(discovered * 100.0 / total) + "%";
    }
}
