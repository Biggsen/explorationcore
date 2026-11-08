package dev.explorationcore.detect;

import org.bukkit.Material;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

public final class DetectionSettings {
    private static final String DISABLED = "Environmental detection is disabled.";

    private final boolean enabled;
    private final String overworldFolder;
    private final double maxProbability;
    private final double falloff;
    private final HeartEffect heart;
    private final NerveEffect nerve;

    private DetectionSettings(
            boolean enabled,
            String overworldFolder,
            double maxProbability,
            double falloff,
            HeartEffect heart,
            NerveEffect nerve
    ) {
        this.enabled = enabled;
        this.overworldFolder = overworldFolder;
        this.maxProbability = maxProbability;
        this.falloff = falloff;
        this.heart = heart;
        this.nerve = nerve;
    }

    public static DetectionSettings none() {
        return new DetectionSettings(false, "", 0, 0, null, null);
    }

    public static DetectionSettings load(Configuration configuration, Logger logger) {
        if (configuration == null || !configuration.contains("detection")) {
            return none();
        }
        ConfigurationSection detection = configuration.getConfigurationSection("detection");
        if (detection == null) {
            logger.severe("detection is malformed. " + DISABLED);
            return none();
        }
        boolean enabled = detection.getBoolean("enabled", false);
        Double maxProbability = unitInterval(detection, "max-probability", logger);
        if (maxProbability == null) {
            return none();
        }
        Double falloff = positive(detection, "falloff", "detection.falloff", logger);
        if (falloff == null) {
            return none();
        }
        String folder = overworldFolder(detection, logger);
        if (folder == null) {
            return none();
        }
        HeartEffect heart = heart(detection, logger);
        if (heart == null) {
            return none();
        }
        NerveEffect nerve = nerve(detection, logger);
        if (nerve == null) {
            return none();
        }
        return new DetectionSettings(enabled, folder, maxProbability, falloff, heart, nerve);
    }

    public boolean enabled() {
        return enabled;
    }

    public String overworldFolder() {
        return overworldFolder;
    }

    public double maxProbability() {
        return maxProbability;
    }

    public double falloff() {
        return falloff;
    }

    public HeartEffect heart() {
        return heart;
    }

    public NerveEffect nerve() {
        return nerve;
    }

    private static String overworldFolder(ConfigurationSection detection, Logger logger) {
        if (detection.contains("worlds") && !detection.isConfigurationSection("worlds")) {
            logger.severe("detection.worlds is malformed. " + DISABLED);
            return null;
        }
        ConfigurationSection worlds = detection.getConfigurationSection("worlds");
        if (worlds == null) {
            logger.severe("detection.worlds.overworld is missing or blank. " + DISABLED);
            return null;
        }
        Object raw = worlds.get("overworld");
        if (!(raw instanceof String folder) || folder.isBlank()) {
            logger.severe("detection.worlds.overworld is missing or blank. " + DISABLED);
            return null;
        }
        return folder.trim();
    }

    private static HeartEffect heart(ConfigurationSection detection, Logger logger) {
        ConfigurationSection heart = section(detection, "heart", logger);
        if (heart == null) {
            return null;
        }
        Double horizontal = positive(heart, "horizontal-radius", "detection.heart.horizontal-radius", logger);
        Double vertical = positive(heart, "vertical-radius", "detection.heart.vertical-radius", logger);
        Material place = material(heart, "place", "detection.heart.place", logger);
        Material become = material(heart, "become", "detection.heart.become", logger);
        if (horizontal == null || vertical == null || place == null || become == null) {
            return null;
        }
        return new HeartEffect(horizontal, vertical, place, become);
    }

    private static NerveEffect nerve(ConfigurationSection detection, Logger logger) {
        ConfigurationSection nerve = section(detection, "nerve", logger);
        if (nerve == null) {
            return null;
        }
        Double horizontal = positive(nerve, "horizontal-radius", "detection.nerve.horizontal-radius", logger);
        Double vertical = positive(nerve, "vertical-radius", "detection.nerve.vertical-radius", logger);
        Material drop = material(nerve, "drop", "detection.nerve.drop", logger);
        Set<Material> ores = ores(nerve, logger);
        if (horizontal == null || vertical == null || drop == null || ores == null) {
            return null;
        }
        return new NerveEffect(horizontal, vertical, ores, drop);
    }

    private static ConfigurationSection section(ConfigurationSection detection, String name, Logger logger) {
        if (detection.contains(name) && !detection.isConfigurationSection(name)) {
            logger.severe("detection." + name + " is malformed. " + DISABLED);
            return null;
        }
        ConfigurationSection section = detection.getConfigurationSection(name);
        if (section == null) {
            logger.severe("detection." + name + " is malformed. " + DISABLED);
            return null;
        }
        return section;
    }

    private static Set<Material> ores(ConfigurationSection nerve, Logger logger) {
        if (!nerve.isList("ores")) {
            logger.severe("detection.nerve.ores is malformed. " + DISABLED);
            return null;
        }
        List<?> rawOres = nerve.getList("ores");
        if (rawOres == null || rawOres.isEmpty()) {
            logger.severe("detection.nerve.ores is malformed. " + DISABLED);
            return null;
        }
        Set<Material> ores = new LinkedHashSet<>();
        for (Object raw : rawOres) {
            if (!(raw instanceof String name) || name.isBlank()) {
                logger.severe("detection.nerve.ores is malformed. " + DISABLED);
                return null;
            }
            Material material = Material.matchMaterial(name.trim());
            if (material == null || !ores.add(material)) {
                logger.severe("detection.nerve.ores is malformed. " + DISABLED);
                return null;
            }
        }
        return Set.copyOf(ores);
    }

    private static Material material(ConfigurationSection section, String key, String path, Logger logger) {
        Object raw = section.get(key);
        if (!(raw instanceof String name) || name.isBlank()) {
            logger.severe(path + " is not a material. " + DISABLED);
            return null;
        }
        Material material = Material.matchMaterial(name.trim());
        if (material == null) {
            logger.severe(path + " is not a material. " + DISABLED);
            return null;
        }
        return material;
    }

    private static Double unitInterval(ConfigurationSection section, String key, Logger logger) {
        Object raw = section.get(key);
        if (!(raw instanceof Number number)) {
            logger.severe("detection." + key + " must be from 0 through 1. " + DISABLED);
            return null;
        }
        double value = number.doubleValue();
        if (!Double.isFinite(value) || value < 0 || value > 1) {
            logger.severe("detection." + key + " must be from 0 through 1. " + DISABLED);
            return null;
        }
        return value;
    }

    private static Double positive(ConfigurationSection section, String key, String path, Logger logger) {
        Object raw = section.get(key);
        if (!(raw instanceof Number number)) {
            logger.severe(path + " must be greater than zero. " + DISABLED);
            return null;
        }
        double value = number.doubleValue();
        if (!Double.isFinite(value) || value <= 0) {
            logger.severe(path + " must be greater than zero. " + DISABLED);
            return null;
        }
        return value;
    }
}
