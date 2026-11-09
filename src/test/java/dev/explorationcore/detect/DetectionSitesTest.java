package dev.explorationcore.detect;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DetectionSitesTest {
    @Test
    void readsOverworldHeartsAndNerves() {
        DetectionCatalogue catalogue = DetectionSites.parse(
                """
                # mc-plugin-manager: generator-version=001; plugin=explorationcore
                sites:
                  - id: heart_of_sakonotur
                    type: Heart
                    world: Overworld
                    x: 100
                    y: 90
                    z: -200
                  - id: nerve_of_sakonotur
                    type: nerve
                    world: overworld
                    x: 12
                    y: -15
                    z: 4
                """
        );

        assertEquals(1, catalogue.hearts());
        assertEquals(1, catalogue.nerves());
        assertEquals(0, catalogue.souls());
        assertEquals(new DetectionSite("heart_of_sakonotur", "heart", 100, 90, -200), catalogue.sites().get(0));
        assertEquals(new DetectionSite("nerve_of_sakonotur", "nerve", 12, -15, 4), catalogue.sites().get(1));
    }

    @Test
    void readsAnOverworldSoul() {
        DetectionCatalogue catalogue = DetectionSites.parse(
                """
                sites:
                  - id: soul_of_sakonotur
                    type: Soul
                    world: overworld
                    x: 8
                    y: 64
                    z: -3
                """
        );

        assertEquals(0, catalogue.hearts());
        assertEquals(0, catalogue.nerves());
        assertEquals(1, catalogue.souls());
        assertEquals(new DetectionSite("soul_of_sakonotur", "soul", 8, 64, -3), catalogue.sites().get(0));
    }

    @Test
    void rejectsADuplicateId() {
        assertRejected(
                """
                sites:
                  - id: heart_of_sakonotur
                    type: heart
                    world: overworld
                    x: 0
                    y: 0
                    z: 0
                  - id: heart_of_sakonotur
                    type: nerve
                    world: overworld
                    x: 1
                    y: 1
                    z: 1
                """
        );
    }

    @Test
    void rejectsABadType() {
        assertRejected(row("type: village"));
    }

    @Test
    void rejectsAWorldOtherThanOverworld() {
        assertRejected(row("world: nether"));
    }

    @Test
    void rejectsAMissingCoordinate() {
        assertRejected(
                """
                sites:
                  - id: heart_of_sakonotur
                    type: heart
                    world: overworld
                    x: 1
                    y: 2
                """
        );
    }

    @Test
    void rejectsABlankOrSpacedId() {
        assertRejected(row("id: \"\""));
        assertRejected(row("id: heart of sakonotur"));
    }

    @Test
    void rejectsAFractionalCoordinate() {
        assertRejected(row("y: 90.5"));
    }

    private static String row(String replacement) {
        String row = """
                sites:
                  - id: heart_of_sakonotur
                    type: heart
                    world: overworld
                    x: 1
                    y: 2
                    z: 3
                """;
        if (replacement.startsWith("id:")) {
            return row.replace("id: heart_of_sakonotur", replacement);
        }
        if (replacement.startsWith("type:")) {
            return row.replace("type: heart", replacement);
        }
        if (replacement.startsWith("world:")) {
            return row.replace("world: overworld", replacement);
        }
        return row.replace("y: 2", replacement);
    }

    private static void assertRejected(String yaml) {
        assertThrows(IllegalArgumentException.class, () -> DetectionSites.parse(yaml));
    }
}
