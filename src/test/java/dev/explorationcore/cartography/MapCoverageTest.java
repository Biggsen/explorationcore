package dev.explorationcore.cartography;

import org.junit.jupiter.api.Test;

import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapCoverageTest {
    @Test
    void anEmptyBufferIsUnexplored() {
        OptionalInt explored = MapCoverage.explored(new byte[MapCoverage.PIXELS]);

        assertEquals(0, explored.getAsInt());
        assertEquals("0.0%", MapCoverage.percent(0));
        assertFalse(MapCoverage.meetsRequired(0));
    }

    @Test
    void partialExplorationCountsOnlyNonZeroBytes() {
        byte[] colors = new byte[MapCoverage.PIXELS];
        for (int i = 0; i < 100; i++) {
            colors[i] = 4;
        }

        assertEquals(100, MapCoverage.explored(colors).getAsInt());
        assertFalse(MapCoverage.meetsRequired(100));
    }

    @Test
    void negativePaletteBytesCountAsExplored() {
        byte[] colors = new byte[MapCoverage.PIXELS];
        for (int i = 0; i < colors.length; i++) {
            colors[i] = (byte) 0xFF;
        }

        assertEquals(MapCoverage.PIXELS, MapCoverage.explored(colors).getAsInt());
        assertTrue(MapCoverage.meetsRequired(MapCoverage.PIXELS));
        assertEquals("100.0%", MapCoverage.percent(MapCoverage.PIXELS));
    }

    @Test
    void aHighMapColourIdIsNotTreatedAsUnexplored() {
        byte[] colors = new byte[MapCoverage.PIXELS];
        colors[0] = (byte) (36 * 4);

        assertTrue((colors[0] & 0xFF) > 127);
        assertTrue(colors[0] < 0);
        assertEquals(1, MapCoverage.explored(colors).getAsInt());
    }

    @Test
    void theDisplayRounds16056To98PercentWhileTheThresholdDoesNot() {
        assertEquals("98.0%", MapCoverage.percent(16056));
        assertEquals("98.0%", MapCoverage.percent(16057));
        assertFalse(MapCoverage.meetsRequired(16056));
        assertTrue(MapCoverage.meetsRequired(16057));
    }

    @Test
    void aMissingOrShortBufferIsUnavailable() {
        assertTrue(MapCoverage.explored(null).isEmpty());
        assertTrue(MapCoverage.explored(new byte[10]).isEmpty());
        assertTrue(MapCoverage.explored(new byte[MapCoverage.PIXELS + 1]).isEmpty());
    }
}
