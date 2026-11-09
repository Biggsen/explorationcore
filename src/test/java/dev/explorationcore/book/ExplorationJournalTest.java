package dev.explorationcore.book;

import dev.explorationcore.db.DiscoveryEntry;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExplorationJournalTest {
    @Test
    void namesSortByDisplayNameThenEntityIdAndStayInTheirSection() {
        List<DiscoveryEntry> rows = List.of(
                new DiscoveryEntry("overworld", "region", null, "beta", "beta"),
                new DiscoveryEntry("overworld", "region", null, "alpha", "Alpha"),
                new DiscoveryEntry("overworld", "region", null, "alpine", "alpine"),
                new DiscoveryEntry("overworld", "region", null, "echo-z", "Echo"),
                new DiscoveryEntry("overworld", "region", null, "echo-a", "Echo"),
                new DiscoveryEntry("overworld", "water", null, "tide", "Tide"),
                new DiscoveryEntry("overworld", "water", null, "azure-b", "Azure"),
                new DiscoveryEntry("overworld", "water", null, "azure-a", "Azure"),
                new DiscoveryEntry("overworld", "heart", null, "heart", "Echo"),
                new DiscoveryEntry("nether", "region", null, "nether-alpha", "Alpha"),
                new DiscoveryEntry("overworld", "structure", "shipwreck", "ocean_ruin_52", "Ocean Ruin"),
                new DiscoveryEntry("overworld", "structure", "shipwreck", "ocean_ruin_37", "Ocean Ruin"),
                new DiscoveryEntry("overworld", "structure", null, "blank", "Nameless"),
                new DiscoveryEntry("overworld", "structure", "mineshaft", "hidden", "Hidden Shaft")
        );
        assertEquals(
                List.of("echo-a", "echo-z"),
                ExplorationBook.inJournalOrder(List.of(rows.get(3), rows.get(4))).stream().map(DiscoveryEntry::entityId).toList()
        );

        PreparedBook book = ExplorationBook.prepare("Lowothra", sampleTotals(), rows);
        List<BookLine> regions = pageStarting(book, "Land Regions found", 0);
        assertEquals(anchored("Land Regions found", BookText.rule(), "Alpha", "alpine", "beta", "Echo", "Echo", ExplorationBook.SUMMARY_LINK), texts(regions));
        assertNull(regions.get(0).page());
        assertEquals(1, regions.get(regions.size() - 1).page());
        assertFalse(texts(regions).contains("Hidden Shaft"));
        assertFalse(texts(regions).contains("Nameless"));
        assertFalse(texts(regions).contains("Ocean Ruin"));
        assertFalse(texts(regions).contains("Azure"));
        assertFalse(texts(regions).contains("Tide"));

        List<BookLine> water = pageStarting(book, "Water Regions found", 0);
        assertEquals(anchored("Water Regions found", BookText.rule(), "Azure", "Azure", "Tide", ExplorationBook.SUMMARY_LINK), texts(water));

        List<BookLine> hearts = pageStarting(book, "Hearts found", 0);
        assertEquals(anchored("Hearts found", BookText.rule(), "Echo", ExplorationBook.SUMMARY_LINK), texts(hearts));

        List<BookLine> netherRegions = pageStarting(book, "Nether Regions found", 0);
        assertEquals(anchored("Nether Regions found", BookText.rule(), "Alpha", ExplorationBook.SUMMARY_LINK), texts(netherRegions));

        List<BookLine> shipwrecks = pageStarting(book, "Shipwrecks found", 0);
        assertEquals(anchored("Shipwrecks found", BookText.rule(), "Ocean Ruin", "Ocean Ruin", ExplorationBook.SUMMARY_LINK), texts(shipwrecks));
        assertFalse(joined(book).contains("Hidden Shaft"));
        assertFalse(joined(book).contains("Nameless"));
        assertEquals(List.of(
                "Left out 1 structure discovery with an empty structure_type",
                "Left out structure_type mineshaft (1 discovery): no totals.yml entry"
        ), book.notices());

        int regionPage = indexOf(book, regions) + 1;
        assertEquals(regionPage, line(book.pages().get(0), "Land Regions").page());
        assertNull(line(book.pages().get(0), "Villages").page());
        for (List<BookLine> page : book.pages()) {
            for (BookLine line : page) {
                if (line.page() != null) {
                    assertTrue(line.page() >= 1 && line.page() <= book.pages().size());
                }
            }
        }
    }

    @Test
    void fifteenNamesUseTwoPagesAndAWideNameStaysWhole() {
        List<DiscoveryEntry> rows = new ArrayList<>();
        for (int index = 1; index <= 15; index++) {
            rows.add(new DiscoveryEntry(
                    "overworld", "region", null, "region-" + index, String.format(Locale.ROOT, "Name %02d", index)
            ));
        }
        String wide = "M".repeat(20);
        rows.add(new DiscoveryEntry("overworld", "village", null, "wide", wide));
        for (int index = 0; index < 11; index++) {
            rows.add(new DiscoveryEntry("overworld", "village", null, "village-" + index, "Hamlet " + index));
        }

        PreparedBook book = ExplorationBook.prepare("Lowothra", sampleTotals(), rows);
        List<Integer> regionPages = pagesStarting(book, "Land Regions found");
        assertEquals(2, regionPages.size());
        List<BookLine> first = book.pages().get(regionPages.get(0));
        List<BookLine> second = book.pages().get(regionPages.get(1));
        assertEquals(14, first.size());
        assertEquals("Land Regions found", first.get(0).text());
        assertEquals(BookText.rule(), first.get(1).text());
        assertEquals("Name 01", first.get(2).text());
        assertEquals("Name 10", first.get(11).text());
        assertEquals("", first.get(12).text());
        assertEquals(ExplorationBook.SUMMARY_LINK, first.get(13).text());
        assertEquals(anchored("Land Regions found", BookText.rule(), "Name 11", "Name 12", "Name 13", "Name 14", "Name 15", ExplorationBook.SUMMARY_LINK), texts(second));
        assertEquals(ExplorationBook.SUMMARY_LINK, second.get(BookText.LINES_PER_PAGE - 1).text());

        List<Integer> villagePages = pagesStarting(book, "Villages found");
        assertEquals(2, villagePages.size());
        assertFalse(texts(book.pages().get(villagePages.get(0))).contains(wide));
        List<BookLine> villageNext = book.pages().get(villagePages.get(1));
        assertTrue(texts(villageNext).contains(wide));
        assertEquals(1, texts(villageNext).stream().filter(wide::equals).count());
        assertEquals(2, BookText.visualLines(wide));
    }

    @Test
    void reflowKeepsALongListInsideTheBookAndDropsContinuationHeaders() {
        PreparedBook book = ExplorationBook.prepare("Lowothra", bareTotals(), places(981));

        assertTrue(book.pages().size() <= ExplorationBook.MAX_PAGES);
        assertTrue(book.notices().isEmpty());
        assertEquals(981, countPlaces(book));
        assertEquals("Land Regions found", book.pages().get(2).get(0).text());
        assertEquals(BookText.rule(), book.pages().get(2).get(1).text());
        assertEquals("Place 0011", book.pages().get(3).get(0).text());
        assertEquals(ExplorationBook.SUMMARY_LINK, book.pages().get(3).get(book.pages().get(3).size() - 1).text());
    }

    @Test
    void booksPastThePageCapAreTrimmed() {
        PreparedBook book = ExplorationBook.prepare("Lowothra", bareTotals(), places(1175));

        assertEquals(ExplorationBook.MAX_PAGES, book.pages().size());
        assertEquals(List.of("Left out 1 journal page past the 100 page book limit"), book.notices());
        assertEquals(1174, countPlaces(book));
        assertEquals("Place 0011", book.pages().get(3).get(0).text());
        for (List<BookLine> page : book.pages()) {
            for (BookLine line : page) {
                if (line.page() != null) {
                    assertTrue(line.page() >= 1 && line.page() <= book.pages().size());
                }
            }
        }
    }

    @Test
    void fixturesCoverEmptyPartialAndLongJournals() {
        PreparedBook empty = JournalFixtures.book("Lowothra", "empty");
        assertEquals(3, empty.pages().size());
        assertTrue(BookLine.join(empty.pages().get(0)).contains(BookText.countedLine("Land Regions", "0/544")));
        assertTrue(BookLine.join(empty.pages().get(0)).contains(BookText.countedLine("Water Regions", "0/0")));
        for (List<BookLine> page : empty.pages()) {
            for (BookLine line : page) {
                assertNull(line.page());
            }
        }

        PreparedBook partial = JournalFixtures.book("Lowothra", "partial");
        assertEquals(
                anchored("Land Regions found", BookText.rule(), "Alpha", "alpine", "beta", "Echo", "Echo", "Sakonotur", ExplorationBook.SUMMARY_LINK),
                texts(pageStarting(partial, "Land Regions found", 0))
        );
        assertEquals(anchored("Nether Regions found", BookText.rule(), "Ashen Veins", "Sakonotur", ExplorationBook.SUMMARY_LINK), texts(pageStarting(partial, "Nether Regions found", 0)));
        assertEquals(anchored("Nether Hearts found", BookText.rule(), "Ash Heart", "Cinder Heart", "Vein Heart", ExplorationBook.SUMMARY_LINK), texts(pageStarting(partial, "Nether Hearts found", 0)));
        assertNull(line(partial.pages().get(0), "Nerves").page());
        assertEquals(
                anchored("Souls found", BookText.rule(), "Ash", "Bram", ExplorationBook.SUMMARY_LINK),
                texts(pageStarting(partial, "Souls found", 0))
        );
        assertTrue(line(partial.pages().get(0), "Souls").page() > 0);
        assertFalse(joined(partial).contains("Cinder Soul"));
        assertFalse(joined(partial).contains("Hidden Shaft"));
        assertFalse(joined(partial).contains("Nameless"));
        assertFalse(joined(partial).contains("Ignored Village"));
        assertTrue(partial.notices().contains("Left out 1 structure discovery with an empty structure_type"));
        assertTrue(partial.notices().contains("Left out structure_type mineshaft (1 discovery): no totals.yml entry"));

        PreparedBook longer = JournalFixtures.book("Lowothra", "long");
        List<Integer> regionPages = pagesStarting(longer, "Land Regions found");
        assertEquals(2, regionPages.size());
        assertEquals("Long Region 01", longer.pages().get(regionPages.get(0)).get(2).text());
        assertEquals("Long Region 11", longer.pages().get(regionPages.get(1)).get(2).text());
        assertEquals("Ashen Veins", pageStarting(longer, "Nether Regions found", 0).get(2).text());
        assertEquals(2, pagesStarting(longer, "Shipwrecks found").size());
        assertNull(line(longer.pages().get(0), "Nerves").page());
        assertNull(line(longer.pages().get(0), "Souls").page());

        assertThrows(IllegalArgumentException.class, () -> JournalFixtures.book("Lowothra", "nope"));
        assertNull(JournalFixtures.scenario("nope"));
        assertEquals("partial", JournalFixtures.scenario("PARTIAL"));
    }

    private static List<DiscoveryEntry> places(int count) {
        List<DiscoveryEntry> rows = new ArrayList<>();
        for (int index = 1; index <= count; index++) {
            rows.add(new DiscoveryEntry(
                    "overworld",
                    "region",
                    null,
                    "place-" + index,
                    String.format(Locale.ROOT, "Place %04d", index)
            ));
        }
        return rows;
    }

    private static int countPlaces(PreparedBook book) {
        int shown = 0;
        for (List<BookLine> page : book.pages()) {
            for (BookLine line : page) {
                if (line.text().startsWith("Place ")) {
                    shown++;
                }
            }
        }
        return shown;
    }

    private static ExplorationTotals sampleTotals() {
        return new ExplorationTotals(
                28,
                0,
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

    private static ExplorationTotals bareTotals() {
        return new ExplorationTotals(1, 0, 1, 1, 1, 0, List.of(), 1, 1);
    }

    private static List<BookLine> pageStarting(PreparedBook book, String header, int occurrence) {
        return book.pages().get(pagesStarting(book, header).get(occurrence));
    }

    private static List<Integer> pagesStarting(PreparedBook book, String header) {
        List<Integer> found = new ArrayList<>();
        List<List<BookLine>> pages = book.pages();
        for (int index = 0; index < pages.size(); index++) {
            BookLine first = pages.get(index).get(0);
            if (header.equals(first.text()) && first.page() == null) {
                found.add(index);
            }
        }
        return found;
    }

    private static int indexOf(PreparedBook book, List<BookLine> page) {
        return book.pages().indexOf(page);
    }

    private static BookLine line(List<BookLine> page, String prefix) {
        for (BookLine row : page) {
            if (row.text().startsWith(prefix)) {
                return row;
            }
        }
        throw new AssertionError(prefix);
    }

    private static List<String> anchored(String... lines) {
        List<String> page = new ArrayList<>();
        for (int index = 0; index < lines.length - 1; index++) {
            page.add(lines[index]);
        }
        for (int index = lines.length; index < BookText.LINES_PER_PAGE; index++) {
            page.add("");
        }
        page.add(lines[lines.length - 1]);
        return page;
    }

    private static List<String> texts(List<BookLine> lines) {
        List<String> texts = new ArrayList<>();
        for (BookLine line : lines) {
            texts.add(line.text());
        }
        return texts;
    }

    private static String joined(PreparedBook book) {
        StringBuilder out = new StringBuilder();
        for (List<BookLine> page : book.pages()) {
            out.append(BookLine.join(page)).append('\n');
        }
        return out.toString();
    }
}
