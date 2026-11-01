package dev.explorationcore.book;

import dev.explorationcore.db.DiscoveryEntry;

import java.util.ArrayList;
import java.util.Comparator;
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
    public static final int LINK_COLOR = 0x365B83;
    public static final int MAX_PAGES = 100;
    static final String SUMMARY_LINK = "< Summary";
    static final int SUMMARY_PAGE = 1;

    private static final Comparator<DiscoveryEntry> ENTRY_ORDER = Comparator
            .comparing(DiscoveryEntry::displayName, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(DiscoveryEntry::entityId, Comparator.nullsFirst(String::compareTo));

    private ExplorationBook() {
    }

    public static PreparedBook prepare(
            String serverName,
            ExplorationTotals totals,
            List<DiscoveryEntry> discoveries
    ) {
        List<DiscoveryEntry> rows = discoveries == null ? List.of() : discoveries;
        List<Section> sections = new ArrayList<>();

        List<DiscoveryEntry> overworldRegions = matching(rows, "overworld", "region");
        List<DiscoveryEntry> villages = matching(rows, "overworld", "village");
        List<DiscoveryEntry> hearts = matching(rows, "overworld", "heart");
        List<DiscoveryEntry> nerves = matching(rows, "overworld", "nerve");
        sections.add(new Section("Regions", overworldRegions));
        sections.add(new Section("Villages", villages));
        sections.add(new Section("Hearts", hearts));
        sections.add(new Section("Nerves", nerves));

        long structuresFound = 0;
        int structuresAvailable = 0;
        List<Draft> structureLines = new ArrayList<>();
        for (StructureTotal structure : totals.structures()) {
            List<DiscoveryEntry> found = matchingStructure(rows, structure.key());
            structuresFound += found.size();
            structuresAvailable += structure.total();
            String label = structureLabel(structure);
            int sectionIndex = sections.size();
            sections.add(new Section(label, found));
            structureLines.add(counted(label, found.size(), structure.total(), sectionIndex));
        }

        List<DiscoveryEntry> netherRegions = matching(rows, "nether", "region");
        List<DiscoveryEntry> netherHearts = matching(rows, "nether", "heart");
        int netherRegionSection = sections.size();
        sections.add(new Section("Nether Regions", netherRegions));
        int netherHeartSection = sections.size();
        sections.add(new Section("Nether Hearts", netherHearts));

        List<List<Draft>> summary = summaryDrafts(
                serverName,
                totals,
                overworldRegions.size(),
                villages.size(),
                hearts.size(),
                nerves.size(),
                structuresFound,
                structuresAvailable,
                structureLines,
                counted("Regions", netherRegions.size(), totals.netherRegions(), netherRegionSection),
                counted("Hearts", netherHearts.size(), totals.netherHearts(), netherHeartSection)
        );

        List<List<List<BookLine>>> lists = layout(sections, true);
        if (summary.size() + countPages(lists) > MAX_PAGES) {
            lists = layout(sections, false);
        }
        int[] starts = startPages(summary.size(), lists);
        int omitted = Math.max(0, summary.size() + countPages(lists) - MAX_PAGES);

        List<List<BookLine>> pages = new ArrayList<>();
        for (List<Draft> draftPage : summary) {
            List<BookLine> lines = new ArrayList<>();
            for (Draft draft : draftPage) {
                lines.add(draft.toLine(starts));
            }
            pages.add(lines);
        }
        for (List<List<BookLine>> sectionPages : lists) {
            pages.addAll(sectionPages);
        }
        if (pages.size() > MAX_PAGES) {
            pages = cap(pages);
        }

        List<String> notices = notices(totals, rows);
        if (omitted > 0) {
            notices.add("Left out " + omitted + " journal " + (omitted == 1 ? "page" : "pages")
                    + " past the 100 page book limit");
        }
        return new PreparedBook(pages, notices);
    }

    static List<DiscoveryEntry> inJournalOrder(List<DiscoveryEntry> entries) {
        List<DiscoveryEntry> copy = new ArrayList<>(entries);
        copy.sort(ENTRY_ORDER);
        return copy;
    }

    private static List<List<Draft>> summaryDrafts(
            String serverName,
            ExplorationTotals totals,
            long overworldRegions,
            long villages,
            long hearts,
            long nerves,
            long structuresFound,
            int structuresAvailable,
            List<Draft> structureLines,
            Draft netherRegions,
            Draft netherHearts
    ) {
        List<List<Draft>> pages = new ArrayList<>();
        List<Draft> first = new ArrayList<>();
        first.add(Draft.plain(serverName == null ? "" : serverName));
        first.add(Draft.plain(SUBTITLE));
        first.add(Draft.plain(BookText.countedLine("World explored", percent(
                overworldRegions + villages + hearts + nerves + netherRegions.discovered() + netherHearts.discovered(),
                totals.overworldRegions() + totals.villages() + totals.hearts() + totals.nerves()
                        + totals.netherRegions() + totals.netherHearts()
        ))));
        first.add(Draft.plain(""));
        first.add(Draft.plain(heading("Overworld",
                overworldRegions + villages + hearts + nerves,
                totals.overworldRegions() + totals.villages() + totals.hearts() + totals.nerves())));
        first.add(Draft.plain(""));
        first.add(counted("Regions", overworldRegions, totals.overworldRegions(), 0));
        first.add(counted("Villages", villages, totals.villages(), 1));
        first.add(counted("Hearts", hearts, totals.hearts(), 2));
        first.add(counted("Nerves", nerves, totals.nerves(), 3));
        pages.add(first);

        if (!structureLines.isEmpty()) {
            List<Draft> structures = new ArrayList<>();
            structures.add(Draft.plain(heading("Structures", structuresFound, structuresAvailable)));
            structures.add(Draft.plain(""));
            structures.addAll(structureLines);
            pages.addAll(paginateDrafts(structures));
        }

        List<Draft> nether = new ArrayList<>();
        nether.add(Draft.plain(heading("Nether",
                netherRegions.discovered() + netherHearts.discovered(),
                totals.netherRegions() + totals.netherHearts())));
        nether.add(Draft.plain(""));
        nether.add(netherRegions);
        nether.add(netherHearts);
        pages.add(nether);
        return pages;
    }

    private static List<List<Draft>> paginateDrafts(List<Draft> lines) {
        List<String> texts = new ArrayList<>();
        for (Draft line : lines) {
            texts.add(line.text());
        }
        List<List<Draft>> pages = new ArrayList<>();
        int cursor = 0;
        for (String page : BookText.paginate(texts)) {
            List<Draft> drafted = new ArrayList<>();
            for (String part : page.split("\n", -1)) {
                Draft draft = lines.get(cursor++);
                if (!draft.text().equals(part)) {
                    throw new IllegalStateException("Page layout drifted");
                }
                drafted.add(draft);
            }
            pages.add(drafted);
        }
        if (cursor != lines.size()) {
            throw new IllegalStateException("Page layout drifted");
        }
        return pages;
    }

    private static List<List<List<BookLine>>> layout(List<Section> sections, boolean repeatHeader) {
        List<List<List<BookLine>>> lists = new ArrayList<>();
        for (Section section : sections) {
            if (section.entries().isEmpty()) {
                lists.add(List.of());
                continue;
            }
            lists.add(BookText.paginateNamed(
                    section.label() + " found",
                    names(section.entries()),
                    SUMMARY_LINK,
                    SUMMARY_PAGE,
                    repeatHeader
            ));
        }
        return lists;
    }

    private static int countPages(List<List<List<BookLine>>> lists) {
        int total = 0;
        for (List<List<BookLine>> pages : lists) {
            total += pages.size();
        }
        return total;
    }

    private static int[] startPages(int summaryPages, List<List<List<BookLine>>> lists) {
        int[] starts = new int[lists.size()];
        int next = summaryPages + 1;
        for (int index = 0; index < lists.size(); index++) {
            if (lists.get(index).isEmpty()) {
                starts[index] = 0;
            } else {
                starts[index] = next;
                next += lists.get(index).size();
            }
        }
        return starts;
    }

    private static List<List<BookLine>> cap(List<List<BookLine>> pages) {
        List<List<BookLine>> kept = new ArrayList<>();
        int limit = Math.min(pages.size(), MAX_PAGES);
        for (int index = 0; index < limit; index++) {
            List<BookLine> stripped = new ArrayList<>();
            for (BookLine line : pages.get(index)) {
                if (line.page() != null && line.page() > MAX_PAGES) {
                    stripped.add(BookLine.of(line.text()));
                } else {
                    stripped.add(line);
                }
            }
            kept.add(stripped);
        }
        return kept;
    }

    private static List<String> names(List<DiscoveryEntry> entries) {
        List<String> names = new ArrayList<>();
        for (DiscoveryEntry entry : entries) {
            names.add(entry.displayName());
        }
        return names;
    }

    private static List<DiscoveryEntry> matching(List<DiscoveryEntry> rows, String world, String entityType) {
        List<DiscoveryEntry> found = new ArrayList<>();
        for (DiscoveryEntry entry : rows) {
            if (world.equals(entry.world()) && entityType.equals(entry.entityType())) {
                found.add(entry);
            }
        }
        found.sort(ENTRY_ORDER);
        return found;
    }

    private static List<DiscoveryEntry> matchingStructure(List<DiscoveryEntry> rows, String key) {
        List<DiscoveryEntry> found = new ArrayList<>();
        for (DiscoveryEntry entry : rows) {
            if ("overworld".equals(entry.world())
                    && "structure".equals(entry.entityType())
                    && key.equals(entry.structureType())) {
                found.add(entry);
            }
        }
        found.sort(ENTRY_ORDER);
        return found;
    }

    private static List<String> notices(ExplorationTotals totals, List<DiscoveryEntry> rows) {
        Set<String> keys = new HashSet<>();
        for (StructureTotal structure : totals.structures()) {
            keys.add(structure.key());
        }
        long empty = 0;
        TreeMap<String, Long> unknown = new TreeMap<>();
        for (DiscoveryEntry entry : rows) {
            if (!"structure".equals(entry.entityType())) {
                continue;
            }
            String structureType = entry.structureType();
            if (structureType == null || structureType.isBlank()) {
                empty++;
            } else if (!keys.contains(structureType)) {
                unknown.merge(structureType, 1L, Long::sum);
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

    private static Draft counted(String label, long discovered, int total, int section) {
        return new Draft(
                BookText.countedLine(label, discovered + "/" + total),
                discovered > 0 ? section : -1,
                discovered
        );
    }

    private static String percent(long discovered, int total) {
        if (total <= 0) {
            return "0%";
        }
        return Math.round(discovered * 100.0 / total) + "%";
    }

    private record Section(String label, List<DiscoveryEntry> entries) {
    }

    private record Draft(String text, int section, long discovered) {
        static Draft plain(String text) {
            return new Draft(text, -1, 0);
        }

        BookLine toLine(int[] starts) {
            if (section < 0) {
                return BookLine.of(text);
            }
            int start = starts[section];
            if (start < 1 || start > MAX_PAGES) {
                return BookLine.of(text);
            }
            return BookLine.link(text, start);
        }
    }
}
