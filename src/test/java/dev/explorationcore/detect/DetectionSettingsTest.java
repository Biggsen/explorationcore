package dev.explorationcore.detect;

import org.bukkit.Material;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DetectionSettingsTest {
    private static final Logger LOGGER = quietLogger();

    @Test
    void shippedConfigUsesTheProvisionalEllipsoid() throws IOException {
        DetectionSettings settings = loadResource("config.yml");

        assertTrue(settings.enabled());
        assertEquals("world", settings.overworldFolder());
        assertEquals(0.9, settings.maxProbability(), 0.0);
        assertEquals(1.5, settings.falloff(), 0.0);
        assertEquals(256, settings.heart().horizontalRadius(), 0.0);
        assertEquals(64, settings.heart().verticalRadius(), 0.0);
        assertEquals(Material.SHORT_GRASS, settings.heart().place());
        assertEquals(Material.POPPY, settings.heart().become());
        assertEquals(256, settings.nerve().horizontalRadius(), 0.0);
        assertEquals(32, settings.nerve().verticalRadius(), 0.0);
        assertEquals(Material.COAL, settings.nerve().drop());
        assertTrue(settings.nerve().ores().contains(Material.REDSTONE_ORE));
        assertTrue(settings.nerve().ores().contains(Material.DEEPSLATE_REDSTONE_ORE));
    }

    @Test
    void aBadRadiusDisablesDetection() {
        DetectionSettings settings = load(shipped().replace("vertical-radius: 32", "vertical-radius: 0"));
        assertFalse(settings.enabled());
        assertNull(settings.heart());
    }

    @Test
    void aProbabilityOutsideZeroThroughOneDisablesDetection() {
        assertFalse(load(shipped().replace("max-probability: 0.9", "max-probability: 1.2")).enabled());
        assertFalse(load(shipped().replace("falloff: 1.5", "falloff: 0")).enabled());
    }

    @Test
    void anUnknownMaterialDisablesDetection() {
        assertFalse(load(shipped().replace("become: POPPY", "become: NOT_A_FLOWER")).enabled());
    }

    @Test
    void aBlankOverworldFolderDisablesDetection() {
        assertFalse(load(shipped().replace("overworld: world", "overworld: \"\"")).enabled());
    }

    private static String shipped() {
        return """
                detection:
                  enabled: true
                  max-probability: 0.9
                  falloff: 1.5
                  worlds:
                    overworld: world
                  heart:
                    horizontal-radius: 256
                    vertical-radius: 64
                    place: SHORT_GRASS
                    become: POPPY
                  nerve:
                    horizontal-radius: 256
                    vertical-radius: 32
                    ores:
                      - REDSTONE_ORE
                      - DEEPSLATE_REDSTONE_ORE
                    drop: COAL
                """;
    }

    private static DetectionSettings loadResource(String name) throws IOException {
        try (InputStream in = DetectionSettingsTest.class.getClassLoader().getResourceAsStream(name)) {
            if (in == null) {
                throw new IOException("missing resource " + name);
            }
            return load(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    private static DetectionSettings load(String yaml) {
        YamlConfiguration configuration = new YamlConfiguration();
        try {
            configuration.loadFromString(yaml);
        } catch (InvalidConfigurationException exception) {
            throw new AssertionError(exception);
        }
        return DetectionSettings.load(configuration, LOGGER);
    }

    private static Logger quietLogger() {
        Logger logger = Logger.getLogger("detection-settings-test");
        logger.setUseParentHandlers(false);
        return logger;
    }
}
