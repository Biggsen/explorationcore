package dev.explorationcore.reward;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StructureRewardsTest {
    private static final Logger LOGGER = quietLogger();

    @Test
    void shippedConfigKeepsDeliveryOffAndRemembersTheStructureAmounts() throws IOException {
        StructureRewards rewards = loadResource("config.yml");

        assertFalse(rewards.enabled());
        assertEquals("acb {player} {amount}", rewards.configuredCommand());
        assertEquals(OptionalInt.empty(), rewards.xp("shipwreck"));
        assertEquals(OptionalInt.empty(), rewards.xp("ancient_city"));
        assertEquals(OptionalInt.empty(), rewards.claimblocks("shipwreck"));
        assertEquals(Optional.empty(), rewards.claimblocksCommand("shipwreck", "Steve"));
        assertEquals(80, rewards.configuredXp("shipwreck"));
        assertEquals(500, rewards.configuredXp("ancient_city"));
        assertEquals(130, rewards.configuredXp("buried_treasure"));
        assertEquals(100, rewards.configuredXp("desert_pyramid"));
        assertEquals(110, rewards.configuredXp("desert_well"));
        assertEquals(180, rewards.configuredXp("igloo"));
        assertEquals(150, rewards.configuredXp("jungle_temple"));
        assertEquals(80, rewards.configuredXp("ocean_ruin"));
        assertEquals(80, rewards.configuredXp("pillager_outpost"));
        assertEquals(80, rewards.configuredXp("ruined_portal"));
        assertEquals(200, rewards.configuredXp("trail_ruins"));
        assertEquals(0, rewards.configuredXp("swamp_hut"));
        assertEquals(0, rewards.configuredXp("woodland_mansion"));
        assertEquals(50, rewards.configuredClaimblocks("ancient_city"));
        assertEquals(15, rewards.configuredClaimblocks("buried_treasure"));
        assertEquals(12, rewards.configuredClaimblocks("desert_pyramid"));
        assertEquals(13, rewards.configuredClaimblocks("desert_well"));
        assertEquals(22, rewards.configuredClaimblocks("igloo"));
        assertEquals(18, rewards.configuredClaimblocks("jungle_temple"));
        assertEquals(10, rewards.configuredClaimblocks("ocean_ruin"));
        assertEquals(10, rewards.configuredClaimblocks("pillager_outpost"));
        assertEquals(10, rewards.configuredClaimblocks("ruined_portal"));
        assertEquals(10, rewards.configuredClaimblocks("shipwreck"));
        assertEquals(25, rewards.configuredClaimblocks("trail_ruins"));
        assertEquals(0, rewards.configuredClaimblocks("swamp_hut"));
    }

    @Test
    void enabledConfigSubstitutesThePlayerAndTheClaimblockAmount() {
        StructureRewards rewards = load("""
                rewards:
                  enabled: true
                  claimblocks-command: "acb {player} {amount}"
                  structures:
                    shipwreck:
                      xp: 80
                      claimblocks: 10
                    ancient_city:
                      xp: 500
                      claimblocks: 50
                    desert_well:
                      xp: 0
                      claimblocks: 13
                    buried_treasure:
                      xp: 130
                      claimblocks: 0
                    igloo:
                      xp: -4
                      claimblocks: -2
                    swamp_hut:
                      claimblocks: 12
                """);

        assertTrue(rewards.enabled());
        assertEquals(OptionalInt.of(80), rewards.xp("shipwreck"));
        assertEquals(OptionalInt.of(500), rewards.xp("ancient_city"));
        assertEquals(Optional.of("acb Steve 10"), rewards.claimblocksCommand("shipwreck", "Steve"));
        assertEquals(Optional.of("acb Steve 50"), rewards.claimblocksCommand("ancient_city", "Steve"));
        assertEquals(Optional.of("acb Steve 13"), rewards.claimblocksCommand("desert_well", "Steve"));
        assertEquals(OptionalInt.empty(), rewards.xp("desert_well"));
        assertEquals(OptionalInt.of(130), rewards.xp("buried_treasure"));
        assertEquals(Optional.empty(), rewards.claimblocksCommand("buried_treasure", "Steve"));
        assertEquals(OptionalInt.empty(), rewards.xp("igloo"));
        assertEquals(Optional.empty(), rewards.claimblocksCommand("igloo", "Steve"));
        assertEquals(OptionalInt.empty(), rewards.xp("swamp_hut"));
        assertEquals(Optional.of("acb Steve 12"), rewards.claimblocksCommand("swamp_hut", "Steve"));
        assertEquals(Optional.empty(), rewards.claimblocksCommand("woodland_mansion", "Steve"));
        assertEquals(Optional.empty(), rewards.claimblocksCommand("shipwreck", "Steve Two"));
    }

    @Test
    void plusAmountTemplateKeepsTheSignWithTheAmount() {
        StructureRewards rewards = load("""
                rewards:
                  enabled: true
                  claimblocks-command: "acb {player} +{amount}"
                  structures:
                    shipwreck:
                      xp: 80
                      claimblocks: 10
                """);

        assertEquals(Optional.of("acb Steve +10"), rewards.claimblocksCommand("shipwreck", "Steve"));
    }

    @Test
    void disabledConfigPaysNothing() {
        StructureRewards rewards = load("""
                rewards:
                  enabled: false
                  claimblocks-command: "acb {player} {amount}"
                  structures:
                    shipwreck:
                      xp: 80
                      claimblocks: 10
                """);

        assertFalse(rewards.enabled());
        assertEquals("acb {player} {amount}", rewards.configuredCommand());
        assertEquals(80, rewards.configuredXp("shipwreck"));
        assertEquals(10, rewards.configuredClaimblocks("shipwreck"));
        assertEquals(OptionalInt.empty(), rewards.xp("shipwreck"));
        assertEquals(OptionalInt.empty(), rewards.claimblocks("shipwreck"));
        assertEquals(Optional.empty(), rewards.claimblocksCommand("shipwreck", "Steve"));
    }

    @Test
    void missingRewardsSectionPaysNothing() {
        StructureRewards rewards = load("server-name: Lowothra\n");

        assertFalse(rewards.enabled());
        assertEquals(OptionalInt.empty(), rewards.xp("shipwreck"));
    }

    @Test
    void malformedXpDisablesDelivery() {
        StructureRewards rewards = load("""
                rewards:
                  enabled: true
                  structures:
                    shipwreck:
                      xp: 80
                    ancient_city:
                      xp: lots
                """);

        assertFalse(rewards.enabled());
        assertEquals(OptionalInt.empty(), rewards.xp("shipwreck"));
        assertEquals(OptionalInt.empty(), rewards.xp("ancient_city"));
    }

    @Test
    void malformedStructuresSectionDisablesDelivery() {
        StructureRewards rewards = load("""
                rewards:
                  enabled: true
                  structures: shipwreck
                """);

        assertFalse(rewards.enabled());
        assertEquals(OptionalInt.empty(), rewards.xp("shipwreck"));
        assertEquals(Optional.empty(), rewards.claimblocksCommand("shipwreck", "Steve"));
    }

    @Test
    void invalidClaimblockAmountSkipsThatFamilyAndLeavesXp() {
        StructureRewards rewards = load("""
                rewards:
                  enabled: true
                  claimblocks-command: "acb {player} {amount}"
                  structures:
                    shipwreck:
                      xp: 80
                      claimblocks: lots
                    ancient_city:
                      xp: 500
                      claimblocks: 50
                """);

        assertTrue(rewards.enabled());
        assertEquals(OptionalInt.of(80), rewards.xp("shipwreck"));
        assertEquals(Optional.empty(), rewards.claimblocksCommand("shipwreck", "Steve"));
        assertEquals(Optional.of("acb Steve 50"), rewards.claimblocksCommand("ancient_city", "Steve"));
    }

    @Test
    void missingOrMalformedCommandTemplateLeavesXpAndSendsNoCommand() {
        StructureRewards missing = load("""
                rewards:
                  enabled: true
                  structures:
                    shipwreck:
                      xp: 80
                      claimblocks: 10
                """);
        StructureRewards blank = load("""
                rewards:
                  enabled: true
                  claimblocks-command: "  "
                  structures:
                    shipwreck:
                      xp: 80
                      claimblocks: 10
                """);
        StructureRewards slash = load("""
                rewards:
                  enabled: true
                  claimblocks-command: "/acb {player} {amount}"
                  structures:
                    shipwreck:
                      xp: 80
                      claimblocks: 10
                """);
        StructureRewards playerOnly = load("""
                rewards:
                  enabled: true
                  claimblocks-command: "acb {player}"
                  structures:
                    shipwreck:
                      xp: 80
                      claimblocks: 10
                """);

        assertTrue(missing.enabled());
        assertEquals(OptionalInt.of(80), missing.xp("shipwreck"));
        assertEquals("", missing.configuredCommand());
        assertEquals(Optional.empty(), missing.claimblocksCommand("shipwreck", "Steve"));
        assertEquals("", blank.configuredCommand());
        assertEquals(OptionalInt.of(80), blank.xp("shipwreck"));
        assertEquals("", slash.configuredCommand());
        assertEquals(OptionalInt.of(80), slash.xp("shipwreck"));
        assertEquals("", playerOnly.configuredCommand());
        assertEquals(Optional.empty(), playerOnly.claimblocksCommand("shipwreck", "Steve"));
    }

    private static StructureRewards loadResource(String name) throws IOException {
        try (InputStream in = StructureRewardsTest.class.getClassLoader().getResourceAsStream(name)) {
            if (in == null) {
                throw new IOException("missing resource " + name);
            }
            return load(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    private static StructureRewards load(String yaml) {
        YamlConfiguration configuration = new YamlConfiguration();
        try {
            configuration.loadFromString(yaml);
        } catch (InvalidConfigurationException exception) {
            throw new AssertionError(exception);
        }
        return StructureRewards.load(configuration, LOGGER);
    }

    private static Logger quietLogger() {
        Logger logger = Logger.getLogger("structure-rewards-test");
        logger.setUseParentHandlers(false);
        return logger;
    }
}
