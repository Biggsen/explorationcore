package dev.explorationcore.location;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LocationResolverTest {
    private final LocationIndex index =
            new LocationIndex(
                    Map.of(
                            "world",
                            Map.of(
                                    "warriotos", new LocationIndex.Entry("region", "Warriotos", "&a"),
                                    "cherrybrook", new LocationIndex.Entry("village", "Cherrybrook", null),
                                    "inner_core", new LocationIndex.Entry("structure", "Inner Core", null),
                                    "heart_of_monkvos", new LocationIndex.Entry("heart", "Heart of Monkvos", null),
                                    "cold_sea", new LocationIndex.Entry("water", "Cold Sea", "&4"),
                                    "spawn", new LocationIndex.Entry("system", "Spawn", null)),
                            "world_nether",
                            Map.of("ash", new LocationIndex.Entry("region", "Ash", "&6")),
                            "world_the_end",
                            Map.of("end_city", new LocationIndex.Entry("region", "End City", null))));

    @Test
    void nestedVillageUsesColoredParentAndInnerVillage() {
        List<String> ids = List.of("cherrybrook", "warriotos");
        assertEquals("&aWarriotos", resolve(ids, LocationResolver.CURRENT_REGION));
        assertEquals("Cherrybrook", resolve(ids, LocationResolver.CURRENT_VILLAGE));
        assertEquals("-", resolve(ids, LocationResolver.CURRENT_STRUCTURE));
    }

    @Test
    void loneStructureDoesNotBorrowTheRegionLine() {
        List<String> ids = List.of("inner_core");
        assertEquals("-", resolve(ids, LocationResolver.CURRENT_REGION));
        assertEquals("-", resolve(ids, LocationResolver.CURRENT_VILLAGE));
        assertEquals("Inner Core", resolve(ids, LocationResolver.CURRENT_STRUCTURE));
    }

    @Test
    void heartInsideRegionUsesTheParentRegion() {
        List<String> ids = List.of("heart_of_monkvos", "warriotos");
        assertEquals("&aWarriotos", resolve(ids, LocationResolver.CURRENT_REGION));
        assertEquals("-", resolve(ids, LocationResolver.CURRENT_VILLAGE));
        assertEquals("-", resolve(ids, LocationResolver.CURRENT_STRUCTURE));
    }

    @Test
    void waterIncludesColorAndSystemDoesNot() {
        assertEquals("&4Cold Sea", resolve(List.of("cold_sea"), LocationResolver.CURRENT_REGION));
        assertEquals("Spawn", resolve(List.of("spawn"), LocationResolver.CURRENT_REGION));
        assertEquals("-", resolve(List.of("spawn"), LocationResolver.CURRENT_VILLAGE));
        assertEquals("-", resolve(List.of("cold_sea"), LocationResolver.CURRENT_STRUCTURE));
    }

    @Test
    void unknownIdAndEmptyListResolveToDash() {
        assertEquals("-", resolve(List.of("missing"), LocationResolver.CURRENT_REGION));
        assertEquals("-", resolve(List.of("missing"), LocationResolver.CURRENT_VILLAGE));
        assertEquals("-", resolve(List.of("missing"), LocationResolver.CURRENT_STRUCTURE));
        assertEquals("-", resolve(List.of(), LocationResolver.CURRENT_REGION));
        assertEquals("-", resolve(List.of(), LocationResolver.CURRENT_VILLAGE));
        assertEquals("-", resolve(List.of(), LocationResolver.CURRENT_STRUCTURE));
    }

    @Test
    void netherAndEndUseMinecraftWorldFolders() {
        assertEquals("&6Ash", resolve("world_nether", List.of("ash"), LocationResolver.CURRENT_REGION));
        assertEquals("End City", resolve("world_the_end", List.of("end_city"), LocationResolver.CURRENT_REGION));
        assertEquals("-", resolve("world_nether", List.of("warriotos"), LocationResolver.CURRENT_REGION));
    }

    @Test
    void unknownParameterReturnsNull() {
        assertNull(resolve(List.of("warriotos"), "current_biome"));
    }

    private String resolve(List<String> ids, String params) {
        return resolve("world", ids, params);
    }

    private String resolve(String world, List<String> ids, String params) {
        return LocationResolver.resolve(index, world, ids, params);
    }
}
