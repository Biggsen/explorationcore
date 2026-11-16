package dev.explorationcore.cartography;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapGridTest {
    @Test
    void levelZeroMapAtTheOriginIsGridZero() {
        MapGrid.Tile tile = MapGrid.tile(0, 0, 0);

        assertEquals(0, tile.x());
        assertEquals(0, tile.z());
        assertTrue(tile.aligned());
        assertEquals(128, MapGrid.size(0));
    }

    @Test
    void levelThreeMapAtTheOriginCoversTheDocumentedSquare() {
        assertEquals(448, MapGrid.canonical(0, MapGrid.size(3)));

        MapGrid.Tile tile = MapGrid.tile(3, 448, 448);

        assertEquals(0, tile.x());
        assertEquals(0, tile.z());
        assertTrue(tile.aligned());
        assertEquals(1024, MapGrid.size(3));
    }

    @Test
    void scaleFourNegativeTileUsesFloorDivision() {
        MapGrid.Tile aligned = MapGrid.tile(4, 960, -1088);

        assertEquals(0, aligned.x());
        assertEquals(-1, aligned.z());
        assertTrue(aligned.aligned());

        MapGrid.Tile crossed = MapGrid.tile(4, 960, -1089);

        assertEquals(0, crossed.x());
        assertEquals(-2, crossed.z());
        assertFalse(crossed.aligned());
    }

    @Test
    void negativeScaleZeroTileStaysOnItsOwnSquare() {
        MapGrid.Tile tile = MapGrid.tile(0, -128, -128);

        assertEquals(-1, tile.x());
        assertEquals(-1, tile.z());
        assertTrue(tile.aligned());
    }

    @Test
    void aCenterOf1024OnScaleFourIsGridZeroAndMisaligned() {
        MapGrid.Tile tile = MapGrid.tile(4, 1024, 1024);

        assertEquals(0, tile.x());
        assertEquals(0, tile.z());
        assertFalse(tile.aligned());
    }

    @Test
    void anArbitraryCenterIsNotAligned() {
        MapGrid.Tile tile = MapGrid.tile(4, 100, 100);

        assertFalse(tile.aligned());
    }
}
