package dev.explorationcore.feedback;

import dev.explorationcore.record.AttemptResult;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiscoveryNoticesTest {
    @Test
    void shipwreckUsesTheFamilyAndTheDiscoveredName() {
        DiscoveryNotice notice = DiscoveryNotices.found("shipwreck", "Little Ivory");

        assertEquals("Shipwreck Found", notice.title());
        assertEquals("You found Little Ivory", notice.subtitle());
        assertEquals("You found Little Ivory!", notice.chat());
    }

    @Test
    void ancientCityTitleSplitsTheFamilyName() {
        DiscoveryNotice notice = DiscoveryNotices.found("ancient_city", "Deep Dark Hall");

        assertEquals("Ancient City Found", notice.title());
        assertEquals("You found Deep Dark Hall", notice.subtitle());
        assertEquals("You found Deep Dark Hall!", notice.chat());
    }

    @Test
    void recordedStructureKeepsTheFoundWordingInEitherWorld() {
        DiscoveryNotice overworld = notice("overworld", "structure", "Little Ivory", "shipwreck");
        DiscoveryNotice nether = notice("nether", "structure", "Little Ivory", "shipwreck");

        assertEquals(DiscoveryNotices.found("shipwreck", "Little Ivory"), overworld);
        assertEquals(DiscoveryNotices.found("shipwreck", "Little Ivory"), nether);
    }

    @Test
    void overworldCategoriesUseTheStoredDisplayName() {
        assertDiscovered("overworld", "region", "Sakonotur", "Region Discovered", "You discovered Sakonotur");
        assertDiscovered("overworld", "village", "Widecombe", "Village Discovered", "You discovered Widecombe");
        assertDiscovered("overworld", "heart", "Granos", "Heart Discovered", "You discovered the Heart of Granos");
        assertDiscovered("overworld", "nerve", "Granos", "Nerve Discovered", "You discovered the Nerve of Granos");
    }

    @Test
    void netherCategoriesUseADistinctTitleForTheSameName() {
        assertDiscovered("nether", "region", "Ash Barrow", "Nether Region Discovered", "You discovered Ash Barrow");
        assertDiscovered(
                "nether",
                "heart",
                "Umbral of Ruin",
                "Nether Heart Discovered",
                "You discovered the Heart of Umbral of Ruin"
        );

        DiscoveryNotice overworldRegion = notice("overworld", "region", "Sakonotur", null);
        DiscoveryNotice netherRegion = notice("nether", "region", "Sakonotur", null);
        assertEquals("Region Discovered", overworldRegion.title());
        assertEquals("Nether Region Discovered", netherRegion.title());
        assertEquals(overworldRegion.subtitle(), netherRegion.subtitle());
        assertEquals(overworldRegion.chat(), netherRegion.chat());

        DiscoveryNotice overworldHeart = notice("overworld", "heart", "Sakonotur", null);
        DiscoveryNotice netherHeart = notice("nether", "heart", "Sakonotur", null);
        assertEquals("Heart Discovered", overworldHeart.title());
        assertEquals("Nether Heart Discovered", netherHeart.title());
        assertEquals("You discovered the Heart of Sakonotur", overworldHeart.subtitle());
        assertEquals("You discovered the Heart of Sakonotur", netherHeart.subtitle());
        assertEquals("You discovered the Heart of Sakonotur!", netherHeart.chat());
    }

    @Test
    void experienceMessageUsesTheGrantedAmount() {
        assertEquals("You received 80 experience!", DiscoveryNotices.experience(80));
        assertEquals("You received 500 experience!", DiscoveryNotices.experience(500));
    }

    @Test
    void claimblockMessageUsesTheGrantedAmount() {
        assertEquals("You received your reward: 10 claimblocks", DiscoveryNotices.claimblocks(10));
        assertEquals("You received your reward: 15 claimblocks", DiscoveryNotices.claimblocks(15));
    }

    @Test
    void silentResultsProduceNoNotice() {
        assertTrue(DiscoveryNotices.notice(new AttemptResult.Duplicate(
                "verzion", "overworld", "region", "sakonotur"
        )).isEmpty());
        assertTrue(DiscoveryNotices.notice(new AttemptResult.Rejected("unknown world: world")).isEmpty());
        assertTrue(DiscoveryNotices.notice(new AttemptResult.Failed(
                "verzion", "overworld", "region", "sakonotur", null
        )).isEmpty());
        assertTrue(DiscoveryNotices.notice(recorded(
                "overworld", "structure", "Inner Core", null
        )).isEmpty());
        assertTrue(DiscoveryNotices.notice(recorded(
                "nether", "village", "Widecombe", null
        )).isEmpty());
        assertTrue(DiscoveryNotices.notice(recorded(
                "nether", "nerve", "Sakonotur", null
        )).isEmpty());
    }

    private static void assertDiscovered(
            String world,
            String entityType,
            String displayName,
            String title,
            String subtitle
    ) {
        DiscoveryNotice notice = notice(world, entityType, displayName, null);

        assertEquals(title, notice.title());
        assertEquals(subtitle, notice.subtitle());
        assertEquals(subtitle + "!", notice.chat());
    }

    private static DiscoveryNotice notice(String world, String entityType, String displayName, String structureType) {
        Optional<DiscoveryNotice> notice = DiscoveryNotices.notice(recorded(world, entityType, displayName, structureType));
        assertTrue(notice.isPresent());
        return notice.get();
    }

    private static AttemptResult.Recorded recorded(
            String world,
            String entityType,
            String displayName,
            String structureType
    ) {
        return new AttemptResult.Recorded(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "verzion",
                world,
                entityType,
                "id",
                displayName,
                structureType
        );
    }
}
