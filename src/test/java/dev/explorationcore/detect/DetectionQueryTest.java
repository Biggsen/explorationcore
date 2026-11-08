package dev.explorationcore.detect;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DetectionQueryTest {
    private static final DetectionSite HEART = new DetectionSite("heart_of_sakonotur", "heart", 0, 100, 0);
    private static final DetectionSite NERVE = new DetectionSite("nerve_of_sakonotur", "nerve", 0, 0, 0);

    @Test
    void horizontalAndVerticalRimsAreOutOfRange() {
        assertTrue(hearts(0, 100, 256).isEmpty());
        assertTrue(hearts(0, 100, 257).isEmpty());
        assertTrue(hearts(0, 164, 0).isEmpty());
        assertTrue(hearts(0, 165, 0).isEmpty());
        assertTrue(nerves(0, 32, 0).isEmpty());
        assertTrue(nerves(0, 33, 0).isEmpty());
    }

    @Test
    void combinedOffsetFollowsTheEllipsoid() {
        DetectionHit inside = hearts(128, 132, 128).orElseThrow();
        assertEquals(Math.sqrt(0.75), inside.distance(), 1e-9);
        assertEquals(HEART, inside.site());
        assertTrue(hearts(200, 164, 200).isEmpty());
    }

    @Test
    void elevatedHeartUsesItsOwnHeight() {
        assertTrue(hearts(0, 160, 0).isPresent());
        assertTrue(hearts(0, 170, 0).isEmpty());
        assertEquals(0.0, hearts(0, 100, 0).orElseThrow().distance(), 0.0);
    }

    @Test
    void undergroundNerveStaysBelowTheSurface() {
        assertTrue(nerves(0, 30, 0).isPresent());
        assertTrue(nerves(0, 64, 0).isEmpty());
        assertEquals(0.0, nerves(0, 0, 0).orElseThrow().distance(), 0.0);
    }

    @Test
    void nearestMatchingAmpWins() {
        DetectionSite far = new DetectionSite("heart_of_far", "heart", 0, 64, 0);
        DetectionSite near = new DetectionSite("heart_of_near", "heart", 40, 64, 0);
        DetectionSite nerve = new DetectionSite("nerve_of_sakonotur", "nerve", 50, 64, 0);
        DetectionHit hit = DetectionQuery.nearest(
                List.of(far, nerve, near),
                "heart",
                "world",
                "world",
                50,
                64,
                0,
                256,
                64
        ).orElseThrow();
        assertEquals("heart_of_near", hit.site().id());
    }

    @Test
    void anotherBukkitWorldIsOutOfRange() {
        assertTrue(DetectionQuery.nearest(
                List.of(HEART),
                "heart",
                "world_nether",
                "world",
                0,
                100,
                0,
                256,
                64
        ).isEmpty());
    }

    private static Optional<DetectionHit> hearts(int x, int y, int z) {
        return DetectionQuery.nearest(List.of(HEART), "heart", "world", "world", x, y, z, 256, 64);
    }

    private static Optional<DetectionHit> nerves(int x, int y, int z) {
        return DetectionQuery.nearest(List.of(NERVE), "nerve", "world", "world", x, y, z, 256, 32);
    }
}
