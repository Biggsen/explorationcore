package dev.explorationcore.reward;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
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
        assertEquals(OptionalInt.empty(), rewards.xp("shipwreck"));
        assertEquals(OptionalInt.empty(), rewards.xp("ancient_city"));
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
    }

    @Test
    void enabledConfigPaysConfiguredXpAndIgnoresClaimBlocks() {
        StructureRewards rewards = load("""
                rewards:
                  enabled: true
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
                    igloo:
                      xp: -4
                    swamp_hut:
                      claimblocks: 12
                """);

        assertTrue(rewards.enabled());
        assertEquals(OptionalInt.of(80), rewards.xp("shipwreck"));
        assertEquals(OptionalInt.of(500), rewards.xp("ancient_city"));
        assertEquals(80, rewards.configuredXp("shipwreck"));
        assertEquals(OptionalInt.empty(), rewards.xp("desert_well"));
        assertEquals(OptionalInt.empty(), rewards.xp("igloo"));
        assertEquals(OptionalInt.empty(), rewards.xp("swamp_hut"));
        assertEquals(OptionalInt.empty(), rewards.xp("woodland_mansion"));
    }

    @Test
    void disabledConfigPaysNothing() {
        StructureRewards rewards = load("""
                rewards:
                  enabled: false
                  structures:
                    shipwreck:
                      xp: 80
                      claimblocks: 10
                """);

        assertFalse(rewards.enabled());
        assertEquals(80, rewards.configuredXp("shipwreck"));
        assertEquals(OptionalInt.empty(), rewards.xp("shipwreck"));
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
