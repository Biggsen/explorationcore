package dev.explorationcore.detect;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DetectionChanceTest {
    @Test
    void centerIsTheMaximumProbability() {
        assertEquals(0.9, DetectionChance.chance(0, 0.9, 1.5), 0.0);
    }

    @Test
    void rimsAreTheEdgeOfTheEllipsoid() {
        assertEquals(1.0, DetectionChance.distance(256, 0, 0, 256, 64), 0.0);
        assertEquals(1.0, DetectionChance.distance(0, 64, 0, 256, 64), 0.0);
        assertEquals(1.0, DetectionChance.distance(0, 32, 0, 256, 32), 0.0);
        assertEquals(0.0, DetectionChance.chance(1.0, 0.9, 1.5), 0.0);
    }

    @Test
    void oneBlockPastEitherRimIsOutside() {
        assertTrue(DetectionChance.distance(257, 0, 0, 256, 64) > 1.0);
        assertTrue(DetectionChance.distance(0, 65, 0, 256, 64) > 1.0);
        assertTrue(DetectionChance.distance(0, 33, 0, 256, 32) > 1.0);
    }

    @Test
    void falloffUsesTheSharedExponent() {
        assertEquals(0.9 * Math.pow(0.5, 1.5), DetectionChance.chance(0.5, 0.9, 1.5), 0.0);
    }

    @Test
    void eachDrawIsIndependent() {
        assertTrue(DetectionChance.succeeds(0.9, 0.0));
        assertTrue(DetectionChance.succeeds(0.9, 0.899));
        assertFalse(DetectionChance.succeeds(0.9, 0.9));
        assertFalse(DetectionChance.succeeds(0.0, 0.0));
    }
}
