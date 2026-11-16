package dev.explorationcore.cartography;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SharedMapMarkersTest {
    @Test
    void aSecondWriteOfTheSameAmpIsRefused() {
        Saved saved = new Saved();
        saved.bannerMarkers.put("ec:heart_of_monkvos", "already");

        SharedMapMarkers.Outcome outcome = SharedMapMarkers.markSaved(
                saved,
                "heart_of_monkvos",
                "GREEN",
                "Heart of Monkvos",
                4,
                70,
                8,
                null
        );

        assertEquals(SharedMapMarkers.Outcome.ALREADY, outcome);
        assertEquals(1, saved.bannerMarkers.size());
        assertEquals(false, saved.dirty);
    }

    @Test
    void aMissingServerClassLeavesTheMapUntouched() {
        Saved saved = new Saved();

        SharedMapMarkers.Outcome outcome = SharedMapMarkers.markSaved(
                saved,
                "heart_of_monkvos",
                "GREEN",
                "Heart of Monkvos",
                4,
                70,
                8,
                null
        );

        assertEquals(SharedMapMarkers.Outcome.UNAVAILABLE, outcome);
        assertTrue(saved.bannerMarkers.isEmpty());
        assertEquals(false, saved.dirty);
    }

    @Test
    void readKeepsOnlyAmpKeysAndSortsThem() {
        Saved saved = new Saved();
        saved.bannerMarkers.put("banner-1,2,3", new Banner(new Pos(1, 2, 3), Dye.RED));
        saved.bannerMarkers.put("ec:nerve_of_monkvos", new Banner(new Pos(10, 64, -20), Dye.RED));
        saved.bannerMarkers.put("ec:heart_of_monkvos", new Banner(new Pos(4, 70, 8), Dye.GREEN));

        Optional<List<CartographyReport.AmpMarker>> read = SharedMapMarkers.readSaved(saved);

        assertEquals(Optional.of(List.of(
                new CartographyReport.AmpMarker("heart_of_monkvos", "heart", 4, 70, 8),
                new CartographyReport.AmpMarker("nerve_of_monkvos", "nerve", 10, 64, -20)
        )), read);
    }

    @Test
    void aMissingBannerMapCannotBeRead() {
        assertEquals(Optional.empty(), SharedMapMarkers.readSaved(new Object()));
    }

    private enum Dye {
        GREEN,
        RED,
        WHITE
    }

    private record Pos(int x, int y, int z) {
        int getX() {
            return x;
        }

        int getY() {
            return y;
        }

        int getZ() {
            return z;
        }
    }

    private record Banner(Pos pos, Dye color) {
    }

    private static final class Saved {
        private final Map<String, Object> bannerMarkers = new HashMap<>();
        private boolean dirty;

        @SuppressWarnings("unused")
        void setDirty() {
            dirty = true;
        }
    }
}
