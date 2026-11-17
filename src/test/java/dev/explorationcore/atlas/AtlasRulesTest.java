package dev.explorationcore.atlas;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AtlasRulesTest {
    @Test
    void theWorldCentredWindowRunsFromNegativeTwoToOne() {
        AtlasLayout.Origin origin = AtlasLayout.origin(-2, -2);

        assertEquals(new AtlasLayout.Origin(-2, -2), origin);
        assertEquals(origin, AtlasLayout.origin(1, 1));
        assertEquals(origin, AtlasLayout.origin(0, 0));
        assertEquals(origin, AtlasLayout.origin(-2, 1));
        assertTrue(AtlasLayout.inside(origin, -2, -2));
        assertTrue(AtlasLayout.inside(origin, 1, 1));
        assertFalse(AtlasLayout.inside(origin, 2, 1));
        assertFalse(AtlasLayout.inside(origin, -3, -2));
        assertEquals(new AtlasLayout.Origin(2, 2), AtlasLayout.origin(2, 2));
        assertEquals(new AtlasLayout.Origin(2, -2), AtlasLayout.origin(5, 1));
        assertEquals(new AtlasLayout.Origin(-6, -6), AtlasLayout.origin(-5, -5));
        assertEquals(new AtlasLayout.Origin(-6, -6), AtlasLayout.origin(-6, -3));
        assertEquals(new AtlasLayout.Origin(-10, -6), AtlasLayout.origin(-7, -5));
    }

    @Test
    void northIsTheTopRowAndWestIsTheLeftColumn() {
        AtlasLayout.Origin origin = AtlasLayout.origin(-2, -2);

        assertEquals(0, AtlasLayout.column(origin, -2));
        assertEquals(0, AtlasLayout.row(origin, -2));
        assertEquals(0, AtlasLayout.guiSlot(origin, -2, -2));
        assertEquals(2, AtlasLayout.column(origin, 0));
        assertEquals(2, AtlasLayout.row(origin, 0));
        assertEquals(20, AtlasLayout.guiSlot(origin, 0, 0));
        assertEquals(3, AtlasLayout.column(origin, 1));
        assertEquals(3, AtlasLayout.row(origin, 1));
        assertEquals(30, AtlasLayout.guiSlot(origin, 1, 1));
        assertTrue(AtlasLayout.mapGuiSlot(0));
        assertTrue(AtlasLayout.mapGuiSlot(30));
        assertFalse(AtlasLayout.mapGuiSlot(4));
    }

    @Test
    void oppositeCornersShareTheCentredWindowAndTheNextWindowIsRefused() {
        AtlasClicks.Decision a1 = AtlasClicks.place(AtlasBoard.empty(), map(true, "overworld", -2, -2, "A1", 1));
        AtlasClicks.Decision c3 = AtlasClicks.place(a1.board(), map(true, "overworld", 0, 0, "C3", 1));
        AtlasClicks.Decision d4 = AtlasClicks.place(c3.board(), map(true, "overworld", 1, 1, "D4", 1));
        AtlasClicks.Decision outside = AtlasClicks.place(d4.board(), map(true, "overworld", 2, 1, "next", 1));

        assertEquals("A1", d4.board().cell(0));
        assertEquals("C3", d4.board().cell(10));
        assertEquals("D4", d4.board().cell(15));
        assertEquals(new AtlasLayout.Origin(-2, -2), d4.board().origin());
        assertEquals(AtlasMessages.OUTSIDE, outside.message());
        assertEquals("A1", outside.board().cell(0));
        assertNull(outside.board().cell(1));
    }

    @Test
    void onlyLevelFourMapsAreAccepted() {
        AtlasClicks.Decision decision = AtlasClicks.place(AtlasBoard.empty(), map(false, "world", 0, -1, "paper", 1));

        assertEquals(AtlasMessages.NOT_LEVEL_FOUR, decision.message());
        assertNull(decision.board().origin());
        assertEquals(1, decision.sourceAmount());
    }

    @Test
    void aMapFromAnotherWorldIsRefused() {
        AtlasClicks.Decision anchored = AtlasClicks.place(AtlasBoard.empty(), map(true, "overworld", 0, -1, "first", 1));
        AtlasClicks.Decision missing = AtlasClicks.place(anchored.board(), map(true, null, 1, 0, "lost", 1));
        AtlasClicks.Decision other = AtlasClicks.place(anchored.board(), map(true, "nether", 1, 0, "other", 2));

        assertEquals(AtlasMessages.OTHER_WORLD, missing.message());
        assertEquals(AtlasMessages.OTHER_WORLD, other.message());
        assertEquals(2, other.sourceAmount());
        assertNull(other.board().cell(11));
    }

    @Test
    void aMapOutsideTheWindowIsRefused() {
        AtlasClicks.Decision anchored = AtlasClicks.place(AtlasBoard.empty(), map(true, "overworld", 0, -1, "first", 1));
        AtlasClicks.Decision outside = AtlasClicks.place(anchored.board(), map(true, "overworld", 2, -1, "east", 1));

        assertEquals(AtlasMessages.OUTSIDE, outside.message());
        assertEquals(anchored.board().origin(), outside.board().origin());
        assertEquals("first", outside.board().cell(6));
    }

    @Test
    void aDuplicateGridCellIsRefused() {
        AtlasClicks.Decision anchored = AtlasClicks.place(AtlasBoard.empty(), map(true, "overworld", 0, -1, "first", 1));
        AtlasClicks.Decision duplicate = AtlasClicks.place(anchored.board(), map(true, "overworld", 0, -1, "copy", 1));

        assertEquals(AtlasMessages.OCCUPIED, duplicate.message());
        assertEquals("first", duplicate.board().cell(6));
    }

    @Test
    void aPartiallyExploredLevelFourMapIsStored() {
        AtlasClicks.Offer exploredPixelsAreNotAnInput = map(true, "overworld", 0, -1, "partial", 1);
        AtlasClicks.Decision decision = AtlasClicks.place(AtlasBoard.empty(), exploredPixelsAreNotAnInput);

        assertNull(decision.message());
        assertEquals("partial", decision.board().cell(6));
        assertEquals("overworld", decision.board().world());
    }

    @Test
    void clickingAnyEmptyCellPlacesTheMapGeographically() {
        AtlasClicks.Offer offer = map(true, "overworld", 0, -1, "south-west", 3);
        AtlasBoard board = AtlasBoard.empty();

        AtlasClicks.Decision northwest = AtlasClicks.press(board, offer, 0, true);
        AtlasClicks.Decision elsewhere = AtlasClicks.press(board, offer, 10, true);

        assertEquals(6, AtlasClicks.changedCell(board, northwest.board()));
        assertEquals(6, AtlasClicks.changedCell(board, elsewhere.board()));
        assertEquals("south-west", northwest.board().cell(6));
        assertNull(northwest.board().cell(0));
        assertEquals(2, northwest.sourceAmount());
        assertNull(northwest.message());

        AtlasClicks.Decision occupied = AtlasClicks.press(northwest.board(), map(true, "overworld", 1, 1, "other", 1), 27, false);
        AtlasClicks.Decision filler = AtlasClicks.press(board, offer, 4, true);

        assertEquals("south-west", occupied.board().cell(6));
        assertNull(occupied.board().cell(15));
        assertNull(occupied.message());
        assertEquals(1, occupied.sourceAmount());
        assertNull(filler.board().origin());
    }

    @Test
    void shiftClickPlacesTheMapGeographically() {
        AtlasBoard board = AtlasBoard.empty();
        AtlasClicks.Decision decision = AtlasClicks.shiftPlace(board, map(true, "overworld", 1, 1, "bottom-right", 1));

        assertEquals(15, AtlasClicks.changedCell(board, decision.board()));
        assertEquals(0, decision.sourceAmount());
        assertEquals(new AtlasLayout.Origin(-2, -2), decision.board().origin());
    }

    @Test
    void aStoredMapCanBeTaken() {
        AtlasClicks.Decision stored = AtlasClicks.place(AtlasBoard.empty(), map(true, "overworld", 0, -1, "taken", 1));
        AtlasClicks.Decision taken = AtlasClicks.take(stored.board(), 6);

        assertEquals("taken", taken.given());
        assertNull(taken.board().cell(6));
        assertNull(taken.board().world());
        assertNull(taken.board().origin());
        assertNull(taken.message());
    }

    @Test
    void shiftClickIntoAFullInventoryLeavesTheMap() {
        AtlasClicks.Decision stored = AtlasClicks.place(AtlasBoard.empty(), map(true, "overworld", 0, -1, "stays", 1));
        AtlasClicks.Decision full = AtlasClicks.shiftTake(stored.board(), 6, false);

        assertNull(full.given());
        assertEquals("stays", full.board().cell(6));
        assertEquals(stored.board().origin(), full.board().origin());
    }

    @Test
    void aNumberKeyDoesNothingWhenBothSidesAreOccupied() {
        AtlasClicks.Decision stored = AtlasClicks.place(AtlasBoard.empty(), map(true, "overworld", -2, -2, "kept", 1));
        AtlasClicks.Decision number = AtlasClicks.number(
                stored.board(),
                false,
                0,
                false,
                map(true, "overworld", 1, 1, "hotbar", 2),
                false
        );

        assertNull(number.given());
        assertNull(number.message());
        assertEquals(2, number.sourceAmount());
        assertEquals("kept", number.board().cell(0));
        assertNull(number.board().cell(15));
    }

    @Test
    void aNumberKeyDoesNothingWhenTheHotbarSlotIsTheAtlas() {
        AtlasClicks.Decision number = AtlasClicks.number(
                AtlasBoard.empty(),
                true,
                0,
                true,
                map(true, "overworld", 0, -1, "atlas-slot", 1),
                false
        );

        assertNull(number.board().origin());
        assertNull(number.message());
        assertEquals(1, number.sourceAmount());
    }

    @Test
    void aDragAcrossSeveralCellsDoesNothing() {
        AtlasClicks.Offer offer = map(true, "overworld", 0, -1, "dragged", 4);
        AtlasClicks.Decision split = AtlasClicks.drag(AtlasBoard.empty(), 2, false, offer);
        AtlasClicks.Decision filler = AtlasClicks.drag(AtlasBoard.empty(), 1, true, offer);
        AtlasClicks.Decision oneCell = AtlasClicks.drag(AtlasBoard.empty(), 1, false, offer);

        assertNull(split.message());
        assertNull(split.board().origin());
        assertEquals(4, split.sourceAmount());
        assertNull(filler.board().cell(6));
        assertEquals("dragged", oneCell.board().cell(6));
        assertEquals(3, oneCell.sourceAmount());
    }

    @Test
    void aFailedWriteRestoresTheMap() {
        AtlasBoard original = AtlasBoard.empty();
        AtlasClicks.Offer offer = map(true, "overworld", 0, -1, "fragile", 3);
        AtlasClicks.Decision applied = AtlasClicks.place(original, offer);
        AtlasClicks.Decision failed = AtlasClicks.finish(original, offer.amount(), applied, true);
        AtlasClicks.Decision kept = AtlasClicks.finish(original, offer.amount(), applied, false);

        assertEquals(AtlasMessages.FAILED, failed.message());
        assertNull(failed.given());
        assertEquals(3, failed.sourceAmount());
        assertNull(failed.board().cell(6));
        assertNull(failed.board().origin());
        assertEquals("fragile", kept.board().cell(6));
        assertEquals(2, kept.sourceAmount());
    }

    private static AtlasClicks.Offer map(boolean levelFour, String world, int gridX, int gridZ, String token, int amount) {
        return new AtlasClicks.Offer(levelFour, world, gridX, gridZ, token, amount);
    }
}
