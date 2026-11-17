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
        saved.decorations.put("ec:heart_of_monkvos", new Decoration("banner_green"));

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
        assertEquals(1, saved.decorations.size());
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
        assertTrue(saved.decorations.isEmpty());
    }

    @Test
    void readKeepsOnlyAmpKeysAndSortsThem() {
        Saved saved = new Saved();
        saved.decorations.put("banner-1,2,3", new Decoration("banner_white"));
        saved.decorations.put("Player", new Decoration("player"));
        saved.decorations.put("ec:nerve_of_monkvos", new Decoration("banner_red"));
        saved.decorations.put("ec:heart_of_monkvos", new Decoration("banner_green"));

        Optional<List<CartographyReport.AmpMarker>> read = SharedMapMarkers.readSaved(saved);

        assertEquals(Optional.of(List.of(
                new CartographyReport.AmpMarker("heart_of_monkvos", "heart"),
                new CartographyReport.AmpMarker("nerve_of_monkvos", "nerve")
        )), read);
    }

    @Test
    void aMissingDecorationMapCannotBeRead() {
        assertEquals(Optional.empty(), SharedMapMarkers.readSaved(new Object()));
    }

    private record Type(String id) {
        @Override
        public String toString() {
            return id;
        }
    }

    private record Decoration(String typeId) {
        Type type() {
            return new Type(typeId);
        }
    }

    private static final class Saved {
        private final Map<String, Object> decorations = new HashMap<>();
    }
}
