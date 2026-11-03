package dev.explorationcore.book;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TotalsFileTest {
    @TempDir
    Path tempDir;

    @Test
    void readsTotalsInFileOrder() throws Exception {
        Path file = write("""
                # generated totals
                overworld:
                  regions:
                    total: 28
                  villages:
                    total: 50
                  hearts:
                    total: 28
                  nerves:
                    total: 28
                  structures:
                    shipwreck:
                      name: "Shipwrecks"
                      short: Wrecks
                      total: 126
                    ancient_city:
                      name: Ancient Cities
                      total: 12
                nether:
                  regions:
                    total: 9
                  hearts:
                    total: 9
                """);

        ExplorationTotals totals = TotalsFile.load(file);

        assertEquals(28, totals.overworldRegions());
        assertEquals(50, totals.villages());
        assertEquals(28, totals.hearts());
        assertEquals(28, totals.nerves());
        assertEquals(9, totals.netherRegions());
        assertEquals(9, totals.netherHearts());
        assertEquals(2, totals.structures().size());
        assertEquals("shipwreck", totals.structures().get(0).key());
        assertEquals("Shipwrecks", totals.structures().get(0).name());
        assertEquals("Wrecks", totals.structures().get(0).shortLabel());
        assertEquals("Wrecks", totals.structures().get(0).label());
        assertEquals(126, totals.structures().get(0).total());
        assertEquals("ancient_city", totals.structures().get(1).key());
        assertEquals("Ancient Cities", totals.structures().get(1).name());
        assertNull(totals.structures().get(1).shortLabel());
        assertEquals("Ancient Cities", totals.structures().get(1).label());
    }

    @Test
    void allowsAnEmptyStructureMapAndAZeroTotal() throws Exception {
        ExplorationTotals totals = TotalsFile.load(write("""
                overworld:
                  regions:
                    total: 0
                  villages:
                    total: 0
                  hearts:
                    total: 0
                  nerves:
                    total: 0
                  structures: {}
                nether:
                  regions:
                    total: 0
                  hearts:
                    total: 0
                """));

        assertTrue(totals.structures().isEmpty());
        assertEquals(0, totals.overworldRegions());
    }

    @Test
    void missingFileIsRejected() {
        TotalsException exception = assertThrows(TotalsException.class, () -> TotalsFile.load(tempDir.resolve("totals.yml")));
        assertEquals("totals.yml is missing", exception.getMessage());
    }

    @Test
    void malformedFilesAreRejected() {
        assertRejected("""
                overworld: []
                """, "overworld must be a map");
        assertRejected("""
                overworld:
                  regions:
                    total: 1
                  villages:
                    total: 1
                  hearts:
                    total: 1
                  nerves:
                    total: 1
                  structures: {}
                """, "nether must be a map");
        assertRejected("""
                overworld:
                  regions:
                    total: -1
                  villages:
                    total: 0
                  hearts:
                    total: 0
                  nerves:
                    total: 0
                  structures: {}
                nether:
                  regions:
                    total: 0
                  hearts:
                    total: 0
                """, "overworld.regions.total must be an integer of zero or more");
        assertRejected("""
                overworld:
                  regions:
                    total: 1.5
                  villages:
                    total: 0
                  hearts:
                    total: 0
                  nerves:
                    total: 0
                  structures: {}
                nether:
                  regions:
                    total: 0
                  hearts:
                    total: 0
                """, "overworld.regions.total must be an integer of zero or more");
        assertRejected("""
                overworld:
                  regions:
                    total: 0
                  villages:
                    total: 0
                  hearts:
                    total: 0
                  nerves:
                    total: 0
                  structures:
                    ocean_ruin:
                      name: " "
                      total: 2
                nether:
                  regions:
                    total: 0
                  hearts:
                    total: 0
                """, "overworld.structures.ocean_ruin.name must be a non-blank string");
        assertRejected("""
                overworld:
                  regions:
                    total: 0
                  villages:
                    total: 0
                  hearts:
                    total: 0
                  nerves:
                    total: 0
                  structures:
                    ocean_ruin:
                      name: Ocean Ruins
                      total: 0
                nether:
                  regions:
                    total: 0
                  hearts:
                    total: 0
                """, "overworld.structures.ocean_ruin.total must be at least 1");
        assertRejected("""
                overworld:
                  regions:
                    total: 0
                  villages:
                    total: 0
                  hearts:
                    total: 0
                  nerves:
                    total: 0
                  structures:
                    ocean_ruin:
                      name: Ocean Ruins
                      short: " "
                      total: 2
                nether:
                  regions:
                    total: 0
                  hearts:
                    total: 0
                """, "overworld.structures.ocean_ruin.short must be a non-blank string");
    }

    private void assertRejected(String yaml, String message) {
        TotalsException exception = assertThrows(TotalsException.class, () -> TotalsFile.load(write(yaml)));
        assertEquals(message, exception.getMessage());
    }

    private Path write(String yaml) throws Exception {
        Path file = tempDir.resolve("totals-" + System.nanoTime() + ".yml");
        Files.writeString(file, yaml);
        return file;
    }
}
