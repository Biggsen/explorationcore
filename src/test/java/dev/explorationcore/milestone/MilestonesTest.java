package dev.explorationcore.milestone;

import dev.explorationcore.feedback.DiscoveryNotices;
import dev.explorationcore.milestone.Milestone.ItemReward;
import dev.explorationcore.milestone.Milestone.ItemReward.StoredEnchant;
import org.bukkit.Material;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.potion.PotionType;
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

    @Test
    void potionsKeepTheirTypeAndDeriveTheConfirmation() {
        Milestones milestones = load("""
                milestones:
                  hearts_discovered:
                    - id: hearts_potions
                      threshold: 1
                      display-name: Heart Wanderer
                      message: Hearts.
                      rewards:
                        items:
                          - material: potion
                            amount: 2
                            potion-type: strong_healing
                          - material: splash_potion
                            amount: 1
                            potion-type: long_swiftness
                          - material: lingering_potion
                            amount: 1
                            potion-type: minecraft:regeneration
                          - material: potion
                            amount: 1
                            potion-type: healing
                            name: Bottle o' Healing
                          - potion 2
                """);

        List<ItemReward> items = milestones.of("hearts_discovered").get(0).items();
        assertEquals(
                new ItemReward(Material.POTION, 2, null, List.of(), PotionType.STRONG_HEALING),
                items.get(0)
        );
        assertEquals("Potion of Healing II", items.get(0).label());
        assertEquals("2 potion[strong_healing]", items.get(0).recoveryToken());
        assertEquals(
                new ItemReward(Material.SPLASH_POTION, 1, null, List.of(), PotionType.LONG_SWIFTNESS),
                items.get(1)
        );
        assertEquals("Splash Potion of Swiftness (extended)", items.get(1).label());
        assertEquals(
                new ItemReward(Material.LINGERING_POTION, 1, null, List.of(), PotionType.REGENERATION),
                items.get(2)
        );
        assertEquals("Lingering Potion of Regeneration", items.get(2).label());
        assertEquals("Bottle o' Healing", items.get(3).label());
        assertEquals(PotionType.HEALING, items.get(3).potionType());
        assertEquals(Material.POTION, items.get(4).material());
        assertEquals(2, items.get(4).amount());
        assertNull(items.get(4).potionType());
        assertEquals("Potion", items.get(4).label());
        assertEquals(
                "You received your reward: Potion of Healing II ×2",
                DiscoveryNotices.item(items.get(0).amount(), items.get(0).label())
        );
        assertEquals(
                "milestone=hearts_potions items=2 potion[strong_healing],1 splash_potion[long_swiftness],"
                        + "1 lingering_potion[regeneration],1 potion[healing],2 potion",
                milestones.of("hearts_discovered").get(0).recoveryText()
        );
    }

    @Test
    void invalidPotionsAndMisplacedKeysDisableMilestones() {
        assertTrue(itemMilestone("speed").isEmpty());
        assertTrue(itemMilestone("long_healing").isEmpty());
        assertTrue(itemMilestone("").isEmpty());
        assertTrue(load(itemYaml("""
                - material: diamond
                  amount: 1
                  potion-type: healing
                """)).isEmpty());
        assertTrue(load(itemYaml("""
                - material: potion
                  amount: 1
                """)).isEmpty());
        assertTrue(load(itemYaml("""
                - material: diamond
                  amount: 1
                  enchantments:
                    efficiency: 4
                """)).isEmpty());
        assertTrue(load(itemYaml("""
                - material: enchanted_book
                  amount: 1
                  enchantments: {}
                """)).isEmpty());
        assertTrue(load(itemYaml("""
                - material: enchanted_book
                  amount: 1
                  enchantments:
                    efficiency: 0
                """)).isEmpty());
        assertTrue(load(itemYaml("""
                - material: enchanted_book
                  amount: 1
                  enchantments:
                    "eff iciency": 1
                """)).isEmpty());
        assertTrue(load(itemYaml("""
                - material: enchanted_book
                  amount: 1
                  enchantments:
                    efficiency: 4
                  potion-type: healing
                """)).isEmpty());
    }

    @Test
    void plainBooksStillLoadAndEnchantedBooksWaitForTheRegistry() {
        Milestones plain = load(itemYaml("""
                - material: enchanted_book
                  amount: 1
                  name: Plain Book
                """));
        ItemReward book = plain.of("hearts_discovered").get(0).items().get(0);
        assertEquals(Material.ENCHANTED_BOOK, book.material());
        assertTrue(book.enchantments().isEmpty());
        assertEquals("Plain Book", book.label());

        assertTrue(load(itemYaml("""
                - material: enchanted_book
                  amount: 1
                  enchantments:
                    efficiency: 4
                    unbreaking: 3
                """)).isEmpty());
    }

    @Test
    void bookLabelsComeFromTheConfiguredEnchantments() {
        ItemReward book = new ItemReward(
                Material.ENCHANTED_BOOK,
                1,
                null,
                List.of(new StoredEnchant("efficiency", 4), new StoredEnchant("unbreaking", 3)),
                null
        );
        assertEquals("Enchanted Book (Efficiency IV, Unbreaking III)", book.label());
        assertEquals("1 enchanted_book[efficiency=4+unbreaking=3]", book.recoveryToken());
        assertEquals(
                "You received your reward: Enchanted Book (Efficiency IV, Unbreaking III)",
                DiscoveryNotices.item(1, book.label())
        );

        ItemReward named = new ItemReward(
                Material.ENCHANTED_BOOK,
                1,
                "Gift Book",
                List.of(new StoredEnchant("efficiency", 1)),
                null
        );
        assertEquals("Gift Book", named.label());
        assertEquals("Enchanted Book (Mending I)", new ItemReward(
                Material.ENCHANTED_BOOK,
                1,
                null,
                List.of(new StoredEnchant("mending", 1)),
                null
        ).label());
    }

    private static Milestones itemMilestone(String potionType) {
        return load(itemYaml("""
                - material: potion
                  amount: 1
                  potion-type: %s
                """.formatted(potionType)));
    }

    private static String itemYaml(String items) {
        return """
                milestones:
                  hearts_discovered:
                    - id: hearts_items
                      threshold: 1
                      display-name: Heart Wanderer
                      message: Hearts.
                      rewards:
                        items:
                """ + indent(items);
    }

    private static String indent(String yaml) {
        StringBuilder builder = new StringBuilder();
        for (String line : yaml.split("\\R", -1)) {
            builder.append("          ").append(line).append('\n');
        }
        return builder.toString();
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
