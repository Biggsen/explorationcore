package dev.explorationcore.feedback;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StructureNoticesTest {
    @Test
    void shipwreckUsesTheFamilyAndTheDiscoveredName() {
        DiscoveryNotice notice = StructureNotices.found("shipwreck", "Little Ivory");

        assertEquals("Shipwreck Found", notice.title());
        assertEquals("You found Little Ivory", notice.subtitle());
        assertEquals("You found Little Ivory!", notice.chat());
    }

    @Test
    void ancientCityTitleSplitsTheFamilyName() {
        DiscoveryNotice notice = StructureNotices.found("ancient_city", "Deep Dark Hall");

        assertEquals("Ancient City Found", notice.title());
        assertEquals("You found Deep Dark Hall", notice.subtitle());
        assertEquals("You found Deep Dark Hall!", notice.chat());
    }

    @Test
    void experienceMessageUsesTheGrantedAmount() {
        assertEquals("You received 80 experience!", StructureNotices.experience(80));
        assertEquals("You received 500 experience!", StructureNotices.experience(500));
    }
}
