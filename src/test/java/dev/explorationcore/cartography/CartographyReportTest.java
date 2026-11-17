package dev.explorationcore.cartography;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CartographyReportTest {
    private static final UUID WORLD = UUID.fromString("12345678-1234-1234-1234-123456789abc");
    private static final UUID OTHER_WORLD = UUID.fromString("abcdefab-1234-1234-1234-123456789abc");

    @Test
    void reportsAValidScaleFourMap() {
        assertEquals(List.of(
                "Cartography Test",
                "Map ID: 12",
                "World: world",
                "Dimension: overworld",
                "Scale: 4",
                "Centre: 960, -1088",
                "Grid: 0, -1",
                "Aligned: Yes",
                "Locked: No",
                "Coverage: 98.4% (16122/16384)",
                "Required: 98%",
                "Same map id: none",
                "Same grid: none",
                "Valid: Yes",
                "Amp markers: 0",
                "Shared data: yes"
        ), CartographyReport.lines(held(12, 4, 960, -1088, false, false, OptionalInt.of(16122), 1, List.of())));
    }

    @Test
    void roundsBothSidesOfTheThresholdButOnlyOnePasses() {
        List<String> shortOf = CartographyReport.lines(
                held(1, 4, 960, 960, false, false, OptionalInt.of(16056), 1, List.of())
        );
        List<String> passing = CartographyReport.lines(
                held(1, 4, 960, 960, true, false, OptionalInt.of(16057), 1, List.of())
        );

        assertEquals("Coverage: 98.0% (16056/16384)", shortOf.get(9));
        assertEquals("Valid: No", shortOf.get(13));
        assertEquals("Locked: Yes", passing.get(8));
        assertEquals("Coverage: 98.0% (16057/16384)", passing.get(9));
        assertEquals("Valid: Yes", passing.get(13));
    }

    @Test
    void rejectsTheWrongScaleAMisalignedCenterAVirtualMapAndAMissingBuffer() {
        assertEquals("Valid: No", line(held(1, 3, 448, 448, false, false, OptionalInt.of(16384), 1, List.of()), "Valid"));
        assertEquals("Grid: 0, 0", line(held(1, 3, 448, 448, false, false, OptionalInt.of(16384), 1, List.of()), "Grid"));

        List<String> askew = CartographyReport.lines(
                held(1, 4, 1024, 1024, false, false, OptionalInt.of(16384), 1, List.of())
        );
        assertEquals("Grid: 0, 0", askew.get(6));
        assertEquals("Aligned: No", askew.get(7));
        assertEquals("Valid: No", askew.get(13));

        assertEquals("Valid: No", line(held(1, 4, 960, 960, false, true, OptionalInt.of(16384), 1, List.of()), "Valid"));

        List<String> unread = CartographyReport.lines(
                held(1, 4, 960, 960, false, false, OptionalInt.empty(), 1, List.of())
        );
        assertEquals("Coverage: unavailable", unread.get(9));
        assertEquals("Valid: No", unread.get(13));
    }

    @Test
    void anUnloadedWorldIsUnknownAndInvalid() {
        List<String> lines = CartographyReport.lines(new CartographyReport.Inspection(
                4,
                null,
                null,
                null,
                4,
                960,
                960,
                false,
                false,
                OptionalInt.of(16384),
                1,
                List.of(),
                Optional.of(List.of())
        ));

        assertEquals("World: unknown", lines.get(2));
        assertEquals("Dimension: unknown", lines.get(3));
        assertEquals("Same grid: none", lines.get(12));
        assertEquals("Valid: No", lines.get(13));
    }

    @Test
    void copiesShareAnIdAndIndependentMapsShareAGrid() {
        CartographyReport.Sighting copy = sighting(12, "offhand", WORLD, "overworld", 4, 960, -1088);
        CartographyReport.Sighting sameTile = sighting(88, "hotbar 2", WORLD, "overworld", 4, 960, -1088);
        CartographyReport.Sighting sameTileAgain = sighting(88, "ender 1", WORLD, "overworld", 4, 960, -1088);
        CartographyReport.Sighting otherWorld = sighting(90, "inventory 3", OTHER_WORLD, "overworld", 4, 960, -1088);
        CartographyReport.Sighting otherScale = sighting(91, "hotbar 4", WORLD, "overworld", 3, 448, 448);

        List<String> lines = CartographyReport.lines(held(
                12,
                4,
                960,
                -1088,
                false,
                false,
                OptionalInt.of(16384),
                2,
                List.of(copy, sameTile, sameTileAgain, otherWorld, otherScale)
        ));

        assertEquals("Same map id: held stack x2, offhand", lines.get(11));
        assertEquals("Same grid: map 88 (hotbar 2, ender 1)", lines.get(12));
    }

    @Test
    void namesTheVanillaDimensionsAndLeavesCustomKeysAlone() {
        assertEquals("overworld", CartographyReport.dimensionName("minecraft:overworld"));
        assertEquals("nether", CartographyReport.dimensionName("minecraft:the_nether"));
        assertEquals("the_end", CartographyReport.dimensionName("minecraft:the_end"));
        assertEquals("custom:sky", CartographyReport.dimensionName("custom:sky"));
        assertNull(CartographyReport.dimensionName(null));
        assertNull(CartographyReport.dimensionName("  "));
    }

    @Test
    void listsAmpMarkersAfterValidity() {
        List<String> lines = CartographyReport.lines(held(
                12,
                4,
                960,
                -1088,
                false,
                false,
                OptionalInt.of(16122),
                1,
                List.of(),
                Optional.of(List.of(
                        new CartographyReport.AmpMarker("nerve_of_monkvos", "nerve"),
                        new CartographyReport.AmpMarker("heart_of_monkvos", "heart")
                ))
        ));

        assertEquals("Valid: Yes", lines.get(13));
        assertEquals("Amp markers: 2", lines.get(14));
        assertEquals("Shared data: yes", lines.get(15));
        assertEquals("heart_of_monkvos heart shared decoration", lines.get(16));
        assertEquals("nerve_of_monkvos nerve shared decoration", lines.get(17));
    }

    @Test
    void saysMarkersAreUnavailableWhenTheReadFailed() {
        List<String> lines = CartographyReport.lines(new CartographyReport.Inspection(
                12,
                "world",
                WORLD,
                "overworld",
                4,
                960,
                -1088,
                false,
                false,
                OptionalInt.of(16122),
                1,
                List.of(),
                Optional.empty()
        ));
        assertEquals("Valid: Yes", lines.get(13));
        assertEquals("Amp markers: unavailable", lines.get(14));
        assertEquals(15, lines.size());
    }

    private static String line(CartographyReport.Inspection inspection, String label) {
        String prefix = label + ": ";
        for (String line : CartographyReport.lines(inspection)) {
            if (line.startsWith(prefix)) {
                return line;
            }
        }
        throw new AssertionError("Missing " + label);
    }

    private static CartographyReport.Inspection held(
            int mapId,
            int scale,
            int centerX,
            int centerZ,
            boolean locked,
            boolean virtual,
            OptionalInt explored,
            int heldCount,
            List<CartographyReport.Sighting> others
    ) {
        return held(mapId, scale, centerX, centerZ, locked, virtual, explored, heldCount, others, Optional.of(List.of()));
    }

    private static CartographyReport.Inspection held(
            int mapId,
            int scale,
            int centerX,
            int centerZ,
            boolean locked,
            boolean virtual,
            OptionalInt explored,
            int heldCount,
            List<CartographyReport.Sighting> others,
            Optional<List<CartographyReport.AmpMarker>> markers
    ) {
        return new CartographyReport.Inspection(
                mapId,
                "world",
                WORLD,
                "overworld",
                scale,
                centerX,
                centerZ,
                locked,
                virtual,
                explored,
                heldCount,
                others,
                markers
        );
    }

    private static CartographyReport.Sighting sighting(
            int mapId,
            String place,
            UUID worldId,
            String dimension,
            int scale,
            int centerX,
            int centerZ
    ) {
        return new CartographyReport.Sighting(mapId, place, worldId, dimension, scale, centerX, centerZ);
    }
}
