package dev.explorationcore.reward;

import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.logging.Logger;

public final class StructureRewards {
    private static final String PLAYER = "{player}";
    private static final String AMOUNT = "{amount}";

    private final boolean enabled;
    private final Map<String, Integer> xpByType;
    private final Map<String, Integer> claimblocksByType;
    private final String commandTemplate;

    private StructureRewards(
            boolean enabled,
            Map<String, Integer> xpByType,
            Map<String, Integer> claimblocksByType,
            String commandTemplate
    ) {
        this.enabled = enabled;
        this.xpByType = xpByType;
        this.claimblocksByType = claimblocksByType;
        this.commandTemplate = commandTemplate;
    }

    public static StructureRewards none() {
        return new StructureRewards(false, Map.of(), Map.of(), "");
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
        Map<String, Integer> claimblocksByType = new HashMap<>();
        if (structures != null) {
            for (String key : structures.getKeys(false)) {
                if (!structures.isConfigurationSection(key)) {
                    logger.severe(
                            "rewards.structures." + key + " is malformed. Structure XP rewards are disabled."
                    );
                    return none();
                }
                ConfigurationSection entry = structures.getConfigurationSection(key);
                if (entry == null) {
                    continue;
                }
                String type = key.toLowerCase(Locale.ROOT);
                if (entry.contains("xp")) {
                    Integer amount = integer(entry.get("xp"));
                    if (amount == null) {
                        logger.severe(
                                "rewards.structures." + key
                                        + ".xp must be an integer. Structure XP rewards are disabled."
                        );
                        return none();
                    }
                    if (amount > 0) {
                        xpByType.put(type, amount);
                    }
                }
                if (entry.contains("claimblocks")) {
                    Integer amount = integer(entry.get("claimblocks"));
                    if (amount == null) {
                        logger.severe(
                                "rewards.structures." + key
                                        + ".claimblocks must be an integer. That claimblock reward is skipped."
                        );
                    } else if (amount > 0) {
                        claimblocksByType.put(type, amount);
                    }
                }
            }
        }
        return new StructureRewards(enabled, Map.copyOf(xpByType), Map.copyOf(claimblocksByType), template(rewards, logger));
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

    public OptionalInt claimblocks(String structureType) {
        if (!enabled || structureType == null) {
            return OptionalInt.empty();
        }
        Integer amount = claimblocksByType.get(structureType);
        if (amount == null || amount <= 0) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(amount);
    }

    public Optional<String> claimblocksCommand(String structureType, String playerName) {
        if (!enabled || commandTemplate.isEmpty() || !singleToken(playerName)) {
            return Optional.empty();
        }
        OptionalInt amount = claimblocks(structureType);
        if (amount.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(
                commandTemplate.replace(PLAYER, playerName).replace(AMOUNT, Integer.toString(amount.getAsInt()))
        );
    }

    boolean enabled() {
        return enabled;
    }

    int configuredXp(String structureType) {
        Integer amount = xpByType.get(structureType);
        return amount == null ? 0 : amount;
    }

    int configuredClaimblocks(String structureType) {
        Integer amount = claimblocksByType.get(structureType);
        return amount == null ? 0 : amount;
    }

    String configuredCommand() {
        return commandTemplate;
    }

    private static String template(ConfigurationSection rewards, Logger logger) {
        String raw = rewards.getString("claimblocks-command");
        if (raw == null) {
            logger.severe("rewards.claimblocks-command is missing or malformed. Claimblock rewards are disabled.");
            return "";
        }
        String command = raw.trim();
        if (command.isEmpty()
                || command.startsWith("/")
                || !command.contains(PLAYER)
                || !command.contains(AMOUNT)) {
            logger.severe("rewards.claimblocks-command is missing or malformed. Claimblock rewards are disabled.");
            return "";
        }
        return command;
    }

    private static boolean singleToken(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            if (Character.isWhitespace(value.charAt(i))) {
                return false;
            }
        }
        return true;
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
