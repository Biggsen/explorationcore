package dev.explorationcore.cartography;

import dev.explorationcore.location.LocationIndex;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AmpSelectionTest {
    private final LocationIndex index = new LocationIndex(Map.of(
            "world",
            Map.of(
                    "warriotos", new LocationIndex.Entry("region", "Warriotos", null),
                    "heart_of_monkvos", new LocationIndex.Entry("heart", "Heart of Monkvos", null),
                    "nerve_of_monkvos", new LocationIndex.Entry("nerve", "Nerve of Monkvos", null),
                    "soul_of_auronia", new LocationIndex.Entry("soul", "Soul of Auronia", null),
                    "cherrybrook", new LocationIndex.Entry("village", "Cherrybrook", null)
            )
    ));

    @Test
    void theHighestPriorityAmpWinsOverTheRegionAroundIt() {
        AmpSelection.Amp amp = AmpSelection.select(
                index,
                "world",
                List.of("heart_of_monkvos", "warriotos")
        );

        assertEquals("heart_of_monkvos", amp.id());
        assertEquals("heart", amp.kind());
        assertEquals("Heart of Monkvos", amp.name());
    }

    @Test
    void aNerveAboveAHeartIsTheOneThatIsMarked() {
        AmpSelection.Amp amp = AmpSelection.select(
                index,
                "world",
                List.of("nerve_of_monkvos", "heart_of_monkvos")
        );

        assertEquals("nerve", amp.kind());
        assertEquals("Nerve of Monkvos", amp.name());
    }

    @Test
    void aSoulIsAnAmpAndAVillageIsNot() {
        assertEquals("soul", AmpSelection.select(index, "world", List.of("soul_of_auronia")).kind());
        assertNull(AmpSelection.select(index, "world", List.of("cherrybrook", "warriotos")));
    }

    @Test
    void aMissingRegionIsSkippedUntilAnAmpIsFound() {
        AmpSelection.Amp amp = AmpSelection.select(
                index,
                "world",
                List.of("missing", "warriotos", "soul_of_auronia")
        );

        assertEquals("soul_of_auronia", amp.id());
    }

    @Test
    void anEmptyOrUnknownWorldSelectsNothing() {
        assertNull(AmpSelection.select(index, "world", List.of()));
        assertNull(AmpSelection.select(index, "world_nether", List.of("heart_of_monkvos")));
        assertNull(AmpSelection.select(null, "world", List.of("heart_of_monkvos")));
    }
}
