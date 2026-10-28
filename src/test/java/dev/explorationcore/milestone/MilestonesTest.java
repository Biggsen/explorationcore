package dev.explorationcore.milestone;

import dev.explorationcore.milestone.Milestone.ItemReward;
import org.bukkit.Material;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MilestonesTest {
    private static final Logger LOGGER = quietLogger();
    private static final String COMMAND = "acb {player} {amount}";

    @Test
    void shippedFileLoadsTheSixTestMilestones() throws IOException {
        Milestones milestones = loadResource("milestones.yml");

        assertEquals(6, milestones.size());
        assertEquals(
                Set.of(
                        "villages_discovered_1 at 1",
                        "regions_discovered_1 at 1",
                        "hearts_discovered_1 at 1",
                        "nerves_discovered_1 at 1",
                        "total_discovered_10 at 4",
                        "structures_found_5 at 5"
                ),
                Set.of(milestones.summary().split(", "))
        );

        Milestone village = milestones.of("villages_discovered").get(0);
        assertEquals("villages_discovered_1", village.id());
        assertEquals(1L, village.threshold());
        assertEquals(50, village.xp());
        assertEquals(List.of(new ItemReward(Material.DIAMOND, 1)), village.items());
        assertNull(village.promotionCommand());

        Milestone heart = milestones.of("hearts_discovered").get(0);
        assertEquals("Heart Wanderer", heart.displayName());
        assertEquals("You discovered a Heart of a region!", heart.message());
        assertEquals(100, heart.xp());
        assertEquals(
                List.of(
                        new ItemReward(Material.DIAMOND, 4),
                        new ItemReward(Material.NETHERITE_SCRAP, 2),
                        new ItemReward(Material.COMPASS, 1)
                ),
                heart.items()
        );
        assertEquals("Diamond", heart.items().get(0).label());
        assertEquals("Netherite Scrap", heart.items().get(1).label());
        assertEquals("Compass", heart.items().get(2).label());
        assertEquals(
                "milestone=hearts_discovered_1 xp=100 items=4 diamond,2 netherite_scrap,1 compass",
                heart.recoveryText()
        );

        Milestone nerve = milestones.of("nerves_discovered").get(0);
        assertEquals(Material.TORCH, nerve.items().get(3).material());
        assertEquals(32, nerve.items().get(3).amount());
        assertEquals("Torch", nerve.items().get(3).label());

        Milestone total = milestones.of("total_discovered").get(0);
        assertEquals("total_discovered_10", total.id());
        assertEquals(4L, total.threshold());
        assertEquals(0, total.xp());
        assertTrue(total.items().isEmpty());
        assertEquals("lp user {player} promote explorer", total.promotionCommand());
        assertEquals("§6{player}§7 has become a §6WANDERER§7!", total.broadcast());
        assertEquals(
                "milestone=total_discovered_10 promotion=\"lp user {player} promote explorer\" "
                        + "broadcast=\"§6{player}§7 has become a §6WANDERER§7!\"",
                total.recoveryText()
        );

        Milestone structure = milestones.of("structures_found").get(0);
        assertEquals(100, structure.claimblocks());
        assertEquals(0, structure.xp());
    }

    @Test
    void structuredItemsKeepTheMaterialAndUseTheOverrideInTheMessage() {
        Milestones milestones = load("""
                milestones:
                  hearts_discovered:
                    - id: hearts_named
                      threshold: 1
                      display-name: Heart Wanderer
                      message: Hearts.
                      rewards:
                        items:
                          - diamond 4
                          - material: experience_bottle
                            amount: 3
                            name: Bottle o' Enchanting
                          - material: turtle_helmet
                            amount: 1
                            name: Turtle Shell
                          - diamond_sword 1
                """);

        List<ItemReward> items = milestones.of("hearts_discovered").get(0).items();
        assertEquals(Material.DIAMOND, items.get(0).material());
        assertEquals(4, items.get(0).amount());
        assertNull(items.get(0).displayName());
        assertEquals("Diamond", items.get(0).label());
        assertEquals(Material.EXPERIENCE_BOTTLE, items.get(1).material());
        assertEquals("Bottle o' Enchanting", items.get(1).label());
        assertEquals(Material.TURTLE_HELMET, items.get(2).material());
        assertEquals(1, items.get(2).amount());
        assertEquals("Turtle Shell", items.get(2).label());
        assertEquals("Diamond Sword", items.get(3).label());
        assertEquals(
                "milestone=hearts_named items=4 diamond,3 experience_bottle,1 turtle_helmet,1 diamond_sword",
                milestones.of("hearts_discovered").get(0).recoveryText()
        );
    }

    @Test
    void malformedFileDisablesEveryMilestone() {
        assertTrue(load("milestones: villages").isEmpty());
        assertTrue(load("""
                milestones:
                  village_discovered:
                    - id: village_discovered_1
                      threshold: 1
                      display-name: Village Wanderer
                      message: You discovered a village!
                """).isEmpty());
        assertTrue(load("""
                milestones:
                  villages_discovered:
                    - id: villages_discovered_1
                      threshold: 0
                      display-name: Village Wanderer
                      message: You discovered a village!
                """).isEmpty());
        assertTrue(load("""
                milestones:
                  villages_discovered:
                    - id: shared
                      threshold: 1
                      display-name: Village Wanderer
                      message: You discovered a village!
                  regions_discovered:
                    - id: shared
                      threshold: 1
                      display-name: Region Wanderer
                      message: You discovered a region!
                """).isEmpty());
        assertTrue(load("""
                milestones:
                  villages_discovered:
                    - id: villages discovered
                      threshold: 1
                      display-name: Village Wanderer
                      message: You discovered a village!
                """).isEmpty());
        assertTrue(load("""
                milestones:
                  hearts_discovered:
                    - id: hearts_discovered_1
                      threshold: 1
                      display-name: Heart Wanderer
                      message: You discovered a Heart of a region!
                      rewards:
                        xp: 100
                        items:
                          - not_a_real_item 1
                """).isEmpty());
        assertTrue(load("""
                milestones:
                  hearts_discovered:
                    - id: hearts_discovered_1
                      threshold: 1
                      display-name: Heart Wanderer
                      message: You discovered a Heart of a region!
                      rewards:
                        items:
                          - air 1
                """).isEmpty());
        assertTrue(load("""
                milestones:
                  hearts_discovered:
                    - id: hearts_discovered_1
                      threshold: 1
                      display-name: Heart Wanderer
                      message: You discovered a Heart of a region!
                      rewards:
                        items:
                          - material: experience_bottle
                            amount: 3
                            name: "  "
                """).isEmpty());
        assertTrue(load("""
                milestones:
                  hearts_discovered:
                    - id: hearts_discovered_1
                      threshold: 1
                      display-name: Heart Wanderer
                      message: You discovered a Heart of a region!
                      rewards:
                        items:
                          - material: not_a_real_item
                            amount: 1
                            name: Missing
                """).isEmpty());
        assertTrue(load("""
                milestones:
                  hearts_discovered:
                    - id: hearts_discovered_1
                      threshold: 1
                      display-name: Heart Wanderer
                      message: You discovered a Heart of a region!
                      rewards:
                        items:
                          - diamond
                """).isEmpty());
        assertTrue(load("""
                milestones:
                  hearts_discovered:
                    - id: hearts_discovered_1
                      threshold: 1
                      display-name: Heart Wanderer
                      message: You discovered a Heart of a region!
                      rewards:
                        items:
                          - diamond 0
                """).isEmpty());
        assertTrue(load("""
                milestones:
                  hearts_discovered:
                    - id: hearts_discovered_1
                      threshold: 1
                      display-name: Heart Wanderer
                      message: You discovered a Heart of a region!
                      rewards:
                        items:
                          - diamond -1
                """).isEmpty());
        assertTrue(load("""
                milestones:
                  total_discovered:
                    - id: total_discovered_10
                      threshold: 4
                      display-name: Wanderer
                      message: 10% of Charidh explored!
                      promotion:
                        command: "/lp user {player} promote explorer"
                """).isEmpty());
        assertTrue(load("""
                milestones:
                  total_discovered:
                    - id: total_discovered_10
                      threshold: 4
                      display-name: Wanderer
                      message: 10% of Charidh explored!
                      promotion:
                        command: "lp user promote explorer"
                """).isEmpty());
        assertTrue(load("""
                milestones:
                  villages_discovered:
                    - id: villages_discovered_1
                      threshold: 1
                      display-name: Village Wanderer
                      message: You discovered a village!
                      rewards:
                        potions: 1
                """).isEmpty());
        assertTrue(load("""
                milestones:
                  structures_found:
                    - id: structures_found_5
                      threshold: 5
                      display-name: Structure Wanderer I
                      message: You found 5 structures!
                      rewards:
                        claimblocks: 100
                """, "").isEmpty());
    }

    private static Milestones loadResource(String name) throws IOException {
        try (InputStream in = MilestonesTest.class.getClassLoader().getResourceAsStream(name)) {
            if (in == null) {
                throw new IOException("missing resource " + name);
            }
            return load(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    private static Milestones load(String yaml) {
        return load(yaml, COMMAND);
    }

    private static Milestones load(String yaml, String command) {
        YamlConfiguration configuration = new YamlConfiguration();
        try {
            configuration.loadFromString(yaml);
        } catch (InvalidConfigurationException exception) {
            throw new AssertionError(exception);
        }
        return Milestones.load(configuration, LOGGER, command);
    }

    private static Logger quietLogger() {
        Logger logger = Logger.getLogger("milestones-test");
        logger.setUseParentHandlers(false);
        return logger;
    }
}
