package dev.explorationcore.detect;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DetectionQueryTest {
    private static final DetectionSite HEART = new DetectionSite("heart_of_sakonotur", "heart", 0, 100, 0);
    private static final DetectionSite NERVE = new DetectionSite("nerve_of_sakonotur", "nerve", 0, 0, 0);
    private static final DetectionSite SOUL = new DetectionSite("soul_of_sakonotur", "soul", 0, 180, 0);

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
    void soulUpperBoundaryIsThirtyTwoAboveTheSite() {
        int ceiling = SOUL.y() + DetectionQuery.SOUL_BLOCKS_ABOVE;
        assertEquals(0.0, souls(0, 180, 0).orElseThrow().distance(), 0.0);
        assertEquals(32.0 / 128.0, souls(0, ceiling, 0).orElseThrow().distance(), 1e-9);
        assertTrue(souls(0, ceiling + 1, 0).isEmpty());
        assertEquals(Math.sqrt(0.5), souls(128, 180, 128).orElseThrow().distance(), 1e-9);
        assertTrue(souls(256, 180, 0).isEmpty());
    }

    @Test
    void soulDownwardReachStopsAtOneRadiusBelow() {
        assertEquals(127.0 / 128.0, souls(0, 53, 0).orElseThrow().distance(), 1e-9);
        assertTrue(souls(0, 52, 0).isEmpty());
        assertTrue(souls(0, 51, 0).isEmpty());

        double ceiling = DetectionChance.chance(souls(0, 180, 0).orElseThrow().distance(), 0.9, 1.5);
        double midway = DetectionChance.chance(souls(0, 116, 0).orElseThrow().distance(), 0.9, 1.5);
        double nearFloor = DetectionChance.chance(souls(0, 53, 0).orElseThrow().distance(), 0.9, 1.5);
        assertEquals(0.9, ceiling, 0.0);
        assertTrue(ceiling > midway);
        assertTrue(midway > nearFloor);
    }

    @Test
    void soulCombinedOffsetFollowsTheLowerHalfEllipsoid() {
        DetectionHit inside = souls(128, 116, 128).orElseThrow();
        assertEquals(Math.sqrt(0.75), inside.distance(), 1e-9);
        assertEquals(SOUL, inside.site());

        assertTrue(souls(200, 180, 0).isPresent());
        assertTrue(souls(0, 100, 0).isPresent());
        assertTrue(souls(200, 100, 200).isEmpty());
    }

    @Test
    void soulGateDoesNotApplyToHearts() {
        DetectionSite heart = new DetectionSite("heart_of_ceiling", "heart", 0, 180, 0);
        Optional<DetectionHit> above = DetectionQuery.nearest(
                List.of(heart, SOUL),
                "heart",
                "world",
                "world",
                0,
                181,
                0,
                256,
                64
        );
        assertEquals(1.0 / 64.0, above.orElseThrow().distance(), 1e-9);
        assertTrue(souls(0, SOUL.y() + DetectionQuery.SOUL_BLOCKS_ABOVE + 1, 0).isEmpty());
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

    private static Optional<DetectionHit> souls(int x, int y, int z) {
        return DetectionQuery.nearest(List.of(SOUL), "soul", "world", "world", x, y, z, 256, 128);
    }
}
