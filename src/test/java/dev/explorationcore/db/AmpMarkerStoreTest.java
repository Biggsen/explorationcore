package dev.explorationcore.db;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AmpMarkerStoreTest {
    @TempDir
    Path tempDir;

    @Test
    void aMarkerIsStoredOncePerMapAndAmp() throws Exception {
        Path database = tempDir.resolve("exploration.db");
        AmpMarkerStore.Marker heart = new AmpMarkerStore.Marker(
                12,
                "heart_of_monkvos",
                "heart",
                "Heart of Monkvos",
                4,
                70,
                8
        );
        AmpMarkerStore.Marker nerve = new AmpMarkerStore.Marker(
                12,
                "nerve_of_monkvos",
                "nerve",
                "Nerve of Monkvos",
                10,
                64,
                -20
        );

        try (DiscoveryRepository repository = DiscoveryRepository.open(database)) {
            AmpMarkerStore store = repository.ampMarkers();
            assertEquals(List.of(), store.forMap(12));
            assertTrue(store.insert(heart));
            assertFalse(store.insert(heart));
            assertTrue(store.insert(nerve));
            assertEquals(List.of(heart, nerve), store.forMap(12));
            assertEquals(List.of(), store.forMap(99));
        }
    }
}
