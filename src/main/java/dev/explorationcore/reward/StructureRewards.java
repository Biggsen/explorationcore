package dev.explorationcore.reward;

import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalInt;
import java.util.logging.Logger;

public final class StructureRewards {
    private final boolean enabled;
    private final Map<String, Integer> xpByType;

    private StructureRewards(boolean enabled, Map<String, Integer> xpByType) {
        this.enabled = enabled;
        this.xpByType = xpByType;
    }

    public static StructureRewards none() {
        return new StructureRewards(false, Map.of());
    }

    public static StructureRewards load(Configuration configuration, Logger logger) {
        if (configuration == null || !configuration.contains("rewards")) {
            return none();
        }
        ConfigurationSection rewards = configuration.getConfigurationSection("rewards");
        if (rewards == null) {
            logger.severe("rewards is malformed. Structure XP rewards are disabled.");
            return none();
        }
        boolean enabled = rewards.getBoolean("enabled", false);
        if (rewards.contains("structures") && !rewards.isConfigurationSection("structures")) {
            logger.severe("rewards.structures is malformed. Structure XP rewards are disabled.");
            return none();
        }
        ConfigurationSection structures = rewards.getConfigurationSection("structures");
        Map<String, Integer> xpByType = new HashMap<>();
        if (structures != null) {
            for (String key : structures.getKeys(false)) {
                if (!structures.isConfigurationSection(key)) {
                    logger.severe(
                            "rewards.structures." + key + " is malformed. Structure XP rewards are disabled."
                    );
                    return none();
                }
                ConfigurationSection entry = structures.getConfigurationSection(key);
                if (entry == null || !entry.contains("xp")) {
                    continue;
                }
                Integer amount = integer(entry.get("xp"));
                if (amount == null) {
                    logger.severe(
                            "rewards.structures." + key
                                    + ".xp must be an integer. Structure XP rewards are disabled."
                    );
                    return none();
                }
                if (amount > 0) {
                    xpByType.put(key.toLowerCase(Locale.ROOT), amount);
                }
            }
        }
        return new StructureRewards(enabled, Map.copyOf(xpByType));
    }

    public OptionalInt xp(String structureType) {
        if (!enabled || structureType == null) {
            return OptionalInt.empty();
        }
        Integer amount = xpByType.get(structureType);
        if (amount == null || amount <= 0) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(amount);
    }

    boolean enabled() {
        return enabled;
    }

    int configuredXp(String structureType) {
        Integer amount = xpByType.get(structureType);
        return amount == null ? 0 : amount;
    }

    private static Integer integer(Object raw) {
        if (raw instanceof Byte || raw instanceof Short || raw instanceof Integer) {
            return ((Number) raw).intValue();
        }
        if (raw instanceof Long value && value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE) {
            return value.intValue();
        }
        return null;
    }
}
