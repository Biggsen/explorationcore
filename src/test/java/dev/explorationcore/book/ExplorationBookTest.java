package dev.explorationcore.book;

import dev.explorationcore.db.DiscoveryEntry;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExplorationBookTest {
    @Test
    void pageShowsConfiguredLinesAndDoesNotCapDiscoveries() {
        ExplorationTotals totals = sampleTotals();
        PreparedBook book = ExplorationBook.prepare("Lowothra", totals, sampleDiscoveries());

        assertTrue(book.pages().size() > 3);
        assertEquals(String.join("\n",
                "Lowothra",
                "Exploration Record",
                BookText.countedLine("World explored", "28%"),
                "",
                "Overworld (28%)",
                "",
                BookText.countedLine("Land Regions", "40/28"),
                BookText.countedLine("Water Regions", "0/19"),
                BookText.countedLine("Villages", "0/50"),
                BookText.countedLine("Souls", "0/0"),
                BookText.countedLine("Hearts", "2/28"),
                BookText.countedLine("Nerves", "1/28")
        ), text(book, 0));
        assertEquals(String.join("\n",
                "Structures (2%)",
                "",
                BookText.countedLine("Shipwrecks", "3/126"),
                BookText.countedLine("Ancient Cities", "0/12")
        ), text(book, 1));
        assertEquals(String.join("\n",
                "Nether (28%)",
                "",
                BookText.countedLine("Regions", "4/9"),
                BookText.countedLine("Hearts", "1/9")
        ), text(book, 2));
        assertEquals(List.of("Left out structure_type Ocean_Ruin (9 discoveries): no totals.yml entry"), book.notices());
        assertFalse(text(book, 1).contains("9/"));
        assertFalse(text(book, 2).contains("Villages"));
        assertEquals(null, line(book, 0, BookText.countedLine("Villages", "0/50")).page());
        assertEquals(null, line(book, 0, BookText.countedLine("Souls", "0/0")).page());
        assertTrue(line(book, 1, BookText.countedLine("Shipwrecks", "3/126")).page() > 0);
        assertEquals(null, line(book, 1, BookText.countedLine("Ancient Cities", "0/12")).page());
    }

    @Test
    void soulsCountInTheOverworldTotalAndListAlphabetically() {
        ExplorationTotals totals = new ExplorationTotals(1, 0, 0, 0, 0, 2, List.of(), 0, 0);
        assertEquals(3, totals.available());
        List<DiscoveryEntry> rows = List.of(
                new DiscoveryEntry("overworld", "soul", null, "soul_of_zeta", "Zeta"),
                new DiscoveryEntry("overworld", "soul", null, "soul_of_alpha", "Alpha"),
                new DiscoveryEntry("nether", "soul", null, "soul_of_cinder", "Cinder Soul")
        );
        PreparedBook book = ExplorationBook.prepare("Lowothra", totals, rows);

        assertTrue(text(book, 0).contains(BookText.countedLine("World explored", "67%")));
        assertTrue(text(book, 0).contains("Overworld (67%)"));
        assertTrue(text(book, 0).contains(BookText.countedLine("Souls", "2/2")));
        assertTrue(line(book, 0, BookText.countedLine("Souls", "2/2")).page() > 0);
        assertTrue(text(book, 2).startsWith(String.join("\n",
                "Souls found",
                BookText.rule(),
                "Alpha",
                "Zeta",
                ""
        )));
        assertTrue(text(book, 2).endsWith(ExplorationBook.SUMMARY_LINK));
        assertFalse((text(book, 0) + text(book, 1) + text(book, 2)).contains("Cinder Soul"));
    }

    @Test
    void emptyBookStillListsEveryConfiguredLine() {
        PreparedBook book = ExplorationBook.prepare("Lowothra", sampleTotals(), List.of());

        assertEquals(3, book.pages().size());
        assertTrue(text(book, 0).contains(BookText.countedLine("Land Regions", "0/28")));
        assertTrue(text(book, 0).contains(BookText.countedLine("Water Regions", "0/19")));
        assertTrue(text(book, 0).contains(BookText.countedLine("Villages", "0/50")));
        assertTrue(text(book, 0).contains("Overworld (0%)"));
        assertTrue(text(book, 0).contains(BookText.countedLine("Nerves", "0/28")));
        assertTrue(text(book, 0).endsWith(BookText.countedLine("Nerves", "0/28")));
        assertTrue(text(book, 1).contains(BookText.countedLine("Shipwrecks", "0/126")));
        assertTrue(text(book, 1).contains(BookText.countedLine("Ancient Cities", "0/12")));
        assertTrue(book.notices().isEmpty());
        for (List<BookLine> page : book.pages()) {
            for (BookLine row : page) {
                assertEquals(null, row.page());
            }
        }
    }

    @Test
    void emptyAndUnknownStructureTypesAreLeftOut() {
        List<DiscoveryEntry> rows = new ArrayList<>();
        rows.addAll(copies("overworld", "structure", null, "Blank", 2));
        rows.addAll(copies("nether", "structure", "", "Blank", 1));
        rows.addAll(copies("overworld", "structure", "mineshaft", "Mine", 4));
        PreparedBook book = ExplorationBook.prepare("Lowothra", sampleTotals(), rows);

        assertEquals(String.join("\n",
                "Structures (0%)",
                "",
                BookText.countedLine("Shipwrecks", "0/126"),
                BookText.countedLine("Ancient Cities", "0/12")
        ), text(book, 1));
        assertEquals(List.of(
                "Left out 3 structure discoveries with an empty structure_type",
                "Left out structure_type mineshaft (4 discoveries): no totals.yml entry"
        ), book.notices());
        assertFalse(text(book, 1).contains("Mine"));
        assertFalse(text(book, 1).contains("Blank"));
    }

    @Test
    void lowothraStructureNamesFitOnOnePage() {
        List<StructureTotal> structures = List.of(
                new StructureTotal("ancient_city", "Ancient Cities", 12),
                new StructureTotal("buried_treasure", "Buried Treasures", "Treasures", 53),
                new StructureTotal("desert_well", "Desert Wells", 1),
                new StructureTotal("jungle_temple", "Jungle Temples", 11),
                new StructureTotal("ocean_ruin", "Ocean Ruins", 169),
                new StructureTotal("pillager_outpost", "Pillager Outposts", 8),
                new StructureTotal("shipwreck", "Shipwrecks", 126),
                new StructureTotal("trail_ruins", "Trail Ruins", 19)
        );
        ExplorationTotals totals = new ExplorationTotals(28, 0, 50, 28, 28, 0, structures, 9, 9);
        PreparedBook book = ExplorationBook.prepare("Lowothra", totals, List.of());

        assertEquals(3, book.pages().size());
        assertTrue(text(book, 1).startsWith("Structures (0%)\n\n"));
        assertTrue(text(book, 1).contains(BookText.countedLine("Treasures", "0/53")));
        assertTrue(text(book, 1).endsWith(BookText.countedLine("Trail Ruins", "0/19")));
        int counted = 0;
        for (String structureLine : text(book, 1).split("\n", -1)) {
            if (BookText.countSplit(structureLine) < 0) {
                continue;
            }
            counted++;
            assertEquals(1, BookText.visualLines(structureLine));
            assertTrue(BookText.PAGE_WIDTH - BookText.width(structureLine) < 4);
        }
        assertTrue(counted > 0);
    }

    @Test
    void structureLinesMoveToTheNextPageOnlyWhenThisPageIsFull() {
        List<String> shortLines = new ArrayList<>();
        for (int index = 0; index < 15; index++) {
            shortLines.add("Ruin " + index);
        }
        List<String> shortPages = BookText.paginate(shortLines);
        assertEquals(2, shortPages.size());
        assertEquals(14, shortPages.get(0).split("\n", -1).length);
        assertEquals("Ruin 14", shortPages.get(1));

        assertEquals(1, BookText.visualLines(BookText.rule()));
        assertEquals(2, BookText.visualLines(BookText.rule() + "-"));
        assertEquals(2, BookText.visualLines("M".repeat(20)));
        assertTrue(BookText.displayedLines("M".repeat(19), true).size() > 1);
        assertEquals("M".repeat(19), String.join("", BookText.displayedLines("M".repeat(19), true)));
        assertEquals(List.of("M".repeat(19), "M"), BookText.displayedLines("M".repeat(20)));
        List<String> buried = BookText.displayedLines("Buried Treasures 0/53");
        assertEquals(2, buried.size());
        assertEquals("Buried Treasures 0/53", String.join("", buried));
        assertEquals(List.of("a", "", "b"), BookText.displayedLines("a\n\nb"));
        List<String> wideLines = new ArrayList<>();
        for (int index = 0; index < 8; index++) {
            wideLines.add("M".repeat(20));
        }
        List<String> widePages = BookText.paginate(wideLines);
        assertEquals(2, widePages.size());
        assertEquals(7, widePages.get(0).split("\n", -1).length);
        assertEquals(6, BookText.visualLines("M".repeat(100)));
    }

    @Test
    void worldExploredPercentageUsesAvailableDiscoveries() {
        ExplorationTotals totals = new ExplorationTotals(
                1,
                3,
                0,
                0,
                0,
                0,
                List.of(new StructureTotal("shipwreck", "Shipwrecks", 40)),
                0,
                0
        );
        PreparedBook book = ExplorationBook.prepare("Lowothra", totals, List.of(
                new DiscoveryEntry("overworld", "region", null, "alpha", "Alpha")
        ));

        assertEquals(4, totals.available());
        assertTrue(text(book, 0).contains(BookText.countedLine("World explored", "25%")));
        assertTrue(text(book, 1).contains(BookText.countedLine("Shipwrecks", "0/40")));
    }

    @Test
    void noStructurePageWhenTheFileHasNone() {
        ExplorationTotals totals = new ExplorationTotals(1, 0, 1, 1, 1, 0, List.of(), 1, 1);
        PreparedBook book = ExplorationBook.prepare("Lowothra", totals, List.of());

        assertEquals(2, book.pages().size());
        assertTrue(text(book, 1).startsWith("Nether"));
    }

    @Test
    void shortenedStructureNamesKeepTheCountOnOneLine() {
        ExplorationTotals totals = new ExplorationTotals(1, 0, 1, 1, 1, 0, List.of(
                new StructureTotal("buried_treasure", "Buried Treasures", "Treasures", 89),
                new StructureTotal("woodland_mansion", "Woodland Mansions", "WL Mansions", 3),
                new StructureTotal("desert_well", "Desert Wells", 1)
        ), 1, 1);
        PreparedBook book = ExplorationBook.prepare("Lowothra", totals, List.of());

        assertEquals(String.join("\n",
                "Structures (0%)",
                "",
                BookText.countedLine("Treasures", "0/89"),
                BookText.countedLine("WL Mansions", "0/3"),
                BookText.countedLine("Desert Wells", "0/1")
        ), text(book, 1));
        assertFalse(text(book, 1).contains("Buried"));
        assertFalse(text(book, 1).contains("Woodland"));
    }

    private static String text(PreparedBook book, int page) {
        return BookLine.join(book.pages().get(page));
    }

    private static BookLine line(PreparedBook book, int page, String text) {
        for (BookLine row : book.pages().get(page)) {
            if (row.text().equals(text)) {
                return row;
            }
        }
        throw new AssertionError(text);
    }

    private static List<DiscoveryEntry> sampleDiscoveries() {
        List<DiscoveryEntry> rows = new ArrayList<>();
        rows.addAll(copies("overworld", "region", null, "Region", 40));
        rows.addAll(copies("overworld", "heart", null, "Heart", 2));
        rows.addAll(copies("overworld", "nerve", null, "Nerve", 1));
        rows.addAll(copies("overworld", "structure", "shipwreck", "Shipwreck", 3));
        rows.addAll(copies("overworld", "structure", "Ocean_Ruin", "Ruin", 9));
        rows.addAll(copies("nether", "region", null, "Nether Region", 4));
        rows.addAll(copies("nether", "heart", null, "Nether Heart", 1));
        rows.addAll(copies("nether", "village", null, "Nether Village", 6));
        return rows;
    }

    private static List<DiscoveryEntry> copies(String world, String type, String structureType, String name, int count) {
        List<DiscoveryEntry> rows = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            rows.add(new DiscoveryEntry(world, type, structureType, name + "#" + index, name + " " + index));
        }
        return rows;
    }

    private static ExplorationTotals sampleTotals() {
        return new ExplorationTotals(
                28,
                19,
                50,
                28,
                28,
                0,
                List.of(
                        new StructureTotal("shipwreck", "Shipwrecks", 126),
                        new StructureTotal("ancient_city", "Ancient Cities", 12)
                ),
                9,
                9
        );
    }
}
