package dev.explorationcore.cartography;

import org.junit.jupiter.api.Test;

import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StoredMapColorsTest {
    @Test
    void readsExploredBytesFromTheVanillaBuffer() {
        byte[] colors = new byte[MapCoverage.PIXELS];
        colors[0] = 4;
        colors[1] = (byte) 0xFF;
        SavedColors saved = new SavedColors(colors);

        OptionalInt explored = MapCoverage.explored(StoredMapColors.colors(new MapHandle(saved)));

        assertEquals(2, explored.getAsInt());
    }

    @Test
    void readsABufferDeclaredOnASuperclass() {
        byte[] colors = new byte[MapCoverage.PIXELS];
        colors[10] = 8;

        assertEquals(1, MapCoverage.explored(StoredMapColors.colors(new ChildHandle(new SavedColors(colors)))).getAsInt());
    }

    @Test
    void rejectsAMissingShortOrForeignBuffer() {
        assertNull(StoredMapColors.colors(null));
        assertNull(StoredMapColors.colors(new Object()));
        assertNull(StoredMapColors.colors(new MapHandle(null)));
        assertNull(StoredMapColors.colors(new MapHandle(new SavedColors(null))));
        assertNull(StoredMapColors.colors(new MapHandle(new SavedColors(new byte[3]))));
        assertNull(StoredMapColors.colors(new MapHandle(new WrongColors())));
        assertTrue(MapCoverage.explored(StoredMapColors.colors(new MapHandle(new SavedColors(new byte[3])))).isEmpty());
    }

    private static final class SavedColors {
        private final byte[] colors;

        private SavedColors(byte[] colors) {
            this.colors = colors;
        }
    }

    private static class MapHandle {
        private final Object worldMap;

        private MapHandle(Object worldMap) {
            this.worldMap = worldMap;
        }
    }

    private static final class ChildHandle extends MapHandle {
        private ChildHandle(Object worldMap) {
            super(worldMap);
        }
    }

    private static final class WrongColors {
        private final int[] colors = new int[MapCoverage.PIXELS];
    }
}
