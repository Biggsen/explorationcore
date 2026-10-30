package dev.explorationcore.reward;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiscoveryCratesTest {
    private static final Logger LOGGER = quietLogger();

    @Test
    void shippedConfigKeepsDeliveryOffAndRemembersTheCrates() throws IOException {
        DiscoveryCrates crates = loadResource("config.yml");

        assertFalse(crates.enabled());
        assertEquals("cc give virtual {crate} 1 {player}", crates.configuredCommand());
        assertEquals("RegionCrate", crates.configuredCrate("overworld_region"));
        assertEquals("RegionCrate", crates.configuredCrate("nether_region"));
        assertEquals("VillageCrate", crates.configuredCrate("overworld_village"));
        assertEquals("HeartCrate", crates.configuredCrate("overworld_heart"));
        assertEquals("HeartCrate", crates.configuredCrate("nether_heart"));
        assertEquals("NerveCrate", crates.configuredCrate("overworld_nerve"));
        assertEquals("", crates.configuredCrate("nether_village"));
        assertEquals("", crates.configuredCrate("nether_nerve"));
        assertEquals(Optional.empty(), crates.crate("overworld", "region"));
        assertEquals(Optional.empty(), crates.command("overworld", "region", "Steve"));
    }

    @Test
    void enabledConfigSubstitutesTheCrateAndThePlayer() {
        DiscoveryCrates crates = load("""
                discovery-crates:
                  enabled: true
                  command: "cc give virtual {crate} 1 {player}"
                  rewards:
                    overworld_region: RegionCrate
                    nether_heart: HeartCrate
                    overworld_village: VillageCrate
                """);

        assertTrue(crates.enabled());
        assertEquals(Optional.of("RegionCrate"), crates.crate("overworld", "region"));
        assertEquals(Optional.of("cc give virtual RegionCrate 1 Steve"), crates.command("overworld", "region", "Steve"));
        assertEquals(Optional.of("cc give virtual HeartCrate 1 Steve"), crates.command("nether", "heart", "Steve"));
        assertEquals(Optional.of("cc give virtual VillageCrate 1 Steve"), crates.command("overworld", "village", "Steve"));
    }

    @Test
    void missingKeyAndSpacedPlayerNamePayNothing() {
        DiscoveryCrates crates = load("""
                discovery-crates:
                  enabled: true
                  command: "cc give virtual {crate} 1 {player}"
                  rewards:
                    overworld_region: RegionCrate
                """);

        assertEquals(Optional.empty(), crates.crate("nether", "region"));
        assertEquals(Optional.empty(), crates.command("nether", "region", "Steve"));
        assertEquals(Optional.empty(), crates.command("overworld", "nerve", "Steve"));
        assertEquals(Optional.empty(), crates.command("overworld", "region", "Steve Two"));
        assertEquals(Optional.empty(), crates.command("overworld", "region", " "));
    }

    @Test
    void disabledConfigPaysNothing() {
        DiscoveryCrates crates = load("""
                discovery-crates:
                  enabled: false
                  command: "cc give virtual {crate} 1 {player}"
                  rewards:
                    overworld_region: RegionCrate
                """);

        assertFalse(crates.enabled());
        assertEquals("cc give virtual {crate} 1 {player}", crates.configuredCommand());
        assertEquals("RegionCrate", crates.configuredCrate("overworld_region"));
        assertEquals(Optional.empty(), crates.crate("overworld", "region"));
        assertEquals(Optional.empty(), crates.command("overworld", "region", "Steve"));
    }

    @Test
    void missingSectionPaysNothing() {
        DiscoveryCrates crates = load("server-name: Lowothra\n");

        assertFalse(crates.enabled());
        assertEquals("", crates.configuredCommand());
        assertEquals(Optional.empty(), crates.command("overworld", "region", "Steve"));
    }

    @Test
    void malformedCommandDisablesGrants() {
        DiscoveryCrates missing = load("""
                discovery-crates:
                  enabled: true
                  rewards:
                    overworld_region: RegionCrate
                """);
        DiscoveryCrates blank = load("""
                discovery-crates:
                  enabled: true
                  command: "  "
                  rewards:
                    overworld_region: RegionCrate
                """);
        DiscoveryCrates slash = load("""
                discovery-crates:
                  enabled: true
                  command: "/cc give virtual {crate} 1 {player}"
                  rewards:
                    overworld_region: RegionCrate
                """);
        DiscoveryCrates playerOnly = load("""
                discovery-crates:
                  enabled: true
                  command: "cc give virtual 1 {player}"
                  rewards:
                    overworld_region: RegionCrate
                """);

        assertFalse(missing.enabled());
        assertEquals("", missing.configuredCommand());
        assertEquals("", missing.configuredCrate("overworld_region"));
        assertEquals(Optional.empty(), missing.command("overworld", "region", "Steve"));
        assertFalse(blank.enabled());
        assertEquals("", blank.configuredCommand());
        assertFalse(slash.enabled());
        assertEquals("", slash.configuredCommand());
        assertFalse(playerOnly.enabled());
        assertEquals(Optional.empty(), playerOnly.command("overworld", "region", "Steve"));
    }

    @Test
    void malformedRewardDisablesGrants() {
        DiscoveryCrates badKind = load("""
                discovery-crates:
                  enabled: true
                  command: "cc give virtual {crate} 1 {player}"
                  rewards:
                    overworld_region: RegionCrate
                    overworld_regoin: RegionCrate
                """);
        DiscoveryCrates spacedCrate = load("""
                discovery-crates:
                  enabled: true
                  command: "cc give virtual {crate} 1 {player}"
                  rewards:
                    overworld_region: Region Crate
                """);
        DiscoveryCrates rewardsShape = load("""
                discovery-crates:
                  enabled: true
                  command: "cc give virtual {crate} 1 {player}"
                  rewards: RegionCrate
                """);

        assertFalse(badKind.enabled());
        assertEquals("", badKind.configuredCrate("overworld_region"));
        assertEquals(Optional.empty(), badKind.command("overworld", "region", "Steve"));
        assertFalse(spacedCrate.enabled());
        assertEquals(Optional.empty(), spacedCrate.command("overworld", "region", "Steve"));
        assertFalse(rewardsShape.enabled());
        assertEquals(Optional.empty(), rewardsShape.command("overworld", "region", "Steve"));
    }

    @Test
    void successfulGrantProducesNoNotification() {
        CrateGrant grant = CrateGrant.fromDispatch(true);

        assertTrue(grant.accepted());
        assertEquals(Optional.empty(), grant.playerMessage());
    }

    private static DiscoveryCrates loadResource(String name) throws IOException {
        try (InputStream in = DiscoveryCratesTest.class.getClassLoader().getResourceAsStream(name)) {
            if (in == null) {
                throw new IOException("missing resource " + name);
            }
            return load(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    private static DiscoveryCrates load(String yaml) {
        YamlConfiguration configuration = new YamlConfiguration();
        try {
            configuration.loadFromString(yaml);
        } catch (InvalidConfigurationException exception) {
            throw new AssertionError(exception);
        }
        return DiscoveryCrates.load(configuration, LOGGER);
    }

    private static Logger quietLogger() {
        Logger logger = Logger.getLogger("discovery-crates-test");
        logger.setUseParentHandlers(false);
        return logger;
    }
}
