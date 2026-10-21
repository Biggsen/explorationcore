package dev.explorationcore.book;

import dev.explorationcore.db.PlayerTally;
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
        PreparedBook book = ExplorationBook.prepare(
                "Lowothra",
                totals,
                List.of(
                        new PlayerTally("overworld", "region", null, 40),
                        new PlayerTally("overworld", "village", null, 0),
                        new PlayerTally("overworld", "heart", null, 2),
                        new PlayerTally("overworld", "nerve", null, 1),
                        new PlayerTally("overworld", "structure", "shipwreck", 3),
                        new PlayerTally("overworld", "structure", "Ocean_Ruin", 9),
                        new PlayerTally("nether", "region", null, 4),
                        new PlayerTally("nether", "heart", null, 1),
                        new PlayerTally("nether", "village", null, 6)
                )
        );

        assertEquals(3, book.pages().size());
        assertEquals(String.join("\n",
                "Lowothra",
                "Exploration Record",
                BookText.countedLine("World explored", "32%"),
                "",
                "Overworld (32%)",
                "",
                BookText.countedLine("Regions", "40/28"),
                BookText.countedLine("Villages", "0/50"),
                BookText.countedLine("Hearts", "2/28"),
                BookText.countedLine("Nerves", "1/28")
        ), book.pages().get(0));
        assertEquals(String.join("\n",
                "Structures (2%)",
                "",
                BookText.countedLine("Shipwrecks", "3/126"),
                BookText.countedLine("Ancient Cities", "0/12")
        ), book.pages().get(1));
        assertEquals(String.join("\n",
                "Nether (28%)",
                "",
                BookText.countedLine("Regions", "4/9"),
                BookText.countedLine("Hearts", "1/9")
        ), book.pages().get(2));
        assertEquals(List.of("Left out structure_type Ocean_Ruin (9 discoveries): no totals.yml entry"), book.notices());
        assertFalse(book.pages().get(1).contains("9/"));
        assertFalse(book.pages().get(2).contains("Villages"));
    }

    @Test
    void emptyBookStillListsEveryConfiguredLine() {
        PreparedBook book = ExplorationBook.prepare("Lowothra", sampleTotals(), List.of());

        assertTrue(book.pages().get(0).contains(BookText.countedLine("Regions", "0/28")));
        assertTrue(book.pages().get(0).contains(BookText.countedLine("Villages", "0/50")));
        assertTrue(book.pages().get(0).contains("Overworld (0%)"));
        assertTrue(book.pages().get(0).endsWith(BookText.countedLine("Nerves", "0/28")));
        assertTrue(book.pages().get(1).contains(BookText.countedLine("Shipwrecks", "0/126")));
        assertTrue(book.pages().get(1).contains(BookText.countedLine("Ancient Cities", "0/12")));
        assertTrue(book.notices().isEmpty());
    }

    @Test
    void emptyAndUnknownStructureTypesAreLeftOut() {
        PreparedBook book = ExplorationBook.prepare(
                "Lowothra",
                sampleTotals(),
                List.of(
                        new PlayerTally("overworld", "structure", null, 2),
                        new PlayerTally("nether", "structure", "", 1),
                        new PlayerTally("overworld", "structure", "mineshaft", 4)
                )
        );

        assertEquals(String.join("\n",
                "Structures (0%)",
                "",
                BookText.countedLine("Shipwrecks", "0/126"),
                BookText.countedLine("Ancient Cities", "0/12")
        ), book.pages().get(1));
        assertEquals(List.of(
                "Left out 3 structure discoveries with an empty structure_type",
                "Left out structure_type mineshaft (4 discoveries): no totals.yml entry"
        ), book.notices());
    }

    @Test
    void lowothraStructureNamesFitOnOnePage() {
        List<StructureTotal> structures = List.of(
                new StructureTotal("ancient_city", "Ancient Cities", 12),
                new StructureTotal("buried_treasure", "Buried Treasures", 53),
                new StructureTotal("desert_well", "Desert Wells", 1),
                new StructureTotal("jungle_temple", "Jungle Temples", 11),
                new StructureTotal("ocean_ruin", "Ocean Ruins", 169),
                new StructureTotal("pillager_outpost", "Pillager Outposts", 8),
                new StructureTotal("shipwreck", "Shipwrecks", 126),
                new StructureTotal("trail_ruins", "Trail Ruins", 19)
        );
        ExplorationTotals totals = new ExplorationTotals(28, 50, 28, 28, structures, 9, 9);
        PreparedBook book = ExplorationBook.prepare("Lowothra", totals, List.of());

        assertEquals(3, book.pages().size());
        assertTrue(book.pages().get(1).startsWith("Structures (0%)\n\n"));
        assertTrue(book.pages().get(1).contains(BookText.countedLine("Treasures", "0/53")));
        assertTrue(book.pages().get(1).endsWith(BookText.countedLine("Trail Ruins", "0/19")));
        for (String structureLine : book.pages().get(1).split("\n", -1)) {
            if (!structureLine.contains(":")) {
                continue;
            }
            assertEquals(1, BookText.visualLines(structureLine));
            assertTrue(BookText.PAGE_WIDTH - BookText.width(structureLine) < 4);
        }
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
        List<String> buried = BookText.displayedLines("Buried Treasures: 0/53");
        assertEquals(2, buried.size());
        assertEquals("Buried Treasures: 0/53", String.join("", buried));
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
    void noStructurePageWhenTheFileHasNone() {
        ExplorationTotals totals = new ExplorationTotals(1, 1, 1, 1, List.of(), 1, 1);
        PreparedBook book = ExplorationBook.prepare("Lowothra", totals, List.of());

        assertEquals(2, book.pages().size());
        assertTrue(book.pages().get(1).startsWith("Nether"));
    }

    @Test
    void shortenedStructureNamesKeepTheCountOnOneLine() {
        ExplorationTotals totals = new ExplorationTotals(1, 1, 1, 1, List.of(
                new StructureTotal("buried_treasure", "Buried Treasures", 89),
                new StructureTotal("woodland_mansion", "Woodland Mansions", 3)
        ), 1, 1);
        PreparedBook book = ExplorationBook.prepare("Lowothra", totals, List.of());

        assertEquals(String.join("\n",
                "Structures (0%)",
                "",
                BookText.countedLine("Treasures", "0/89"),
                BookText.countedLine("WL Mansions", "0/3")
        ), book.pages().get(1));
        assertFalse(book.pages().get(1).contains("Buried"));
        assertFalse(book.pages().get(1).contains("Woodland"));
    }

    private static ExplorationTotals sampleTotals() {
        return new ExplorationTotals(
                28,
                50,
                28,
                28,
                List.of(
                        new StructureTotal("shipwreck", "Shipwrecks", 126),
                        new StructureTotal("ancient_city", "Ancient Cities", 12)
                ),
                9,
                9
        );
    }
}
