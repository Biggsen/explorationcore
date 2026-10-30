package dev.explorationcore.reward;

import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

public final class DiscoveryCrates {
    private static final String PLAYER = "{player}";
    private static final String CRATE = "{crate}";
    private static final Set<String> WORLDS = Set.of("overworld", "nether");
    private static final Set<String> KINDS = Set.of("region", "village", "heart", "nerve");

    private final boolean enabled;
    private final Map<String, String> cratesByKey;
    private final String commandTemplate;

    private DiscoveryCrates(boolean enabled, Map<String, String> cratesByKey, String commandTemplate) {
        this.enabled = enabled;
        this.cratesByKey = cratesByKey;
        this.commandTemplate = commandTemplate;
    }

    public static DiscoveryCrates none() {
        return new DiscoveryCrates(false, Map.of(), "");
    }

    public static DiscoveryCrates load(Configuration configuration, Logger logger) {
        if (configuration == null || !configuration.contains("discovery-crates")) {
            return none();
        }
        ConfigurationSection crates = configuration.getConfigurationSection("discovery-crates");
        if (crates == null) {
            logger.severe("discovery-crates is malformed. Discovery crate rewards are disabled.");
            return none();
        }
        boolean enabled = crates.getBoolean("enabled", false);
        String commandTemplate = template(crates, logger);
        if (commandTemplate.isEmpty()) {
            return none();
        }
        if (crates.contains("rewards") && !crates.isConfigurationSection("rewards")) {
            logger.severe("discovery-crates.rewards is malformed. Discovery crate rewards are disabled.");
            return none();
        }
        ConfigurationSection rewards = crates.getConfigurationSection("rewards");
        Map<String, String> cratesByKey = new HashMap<>();
        if (rewards != null) {
            for (String key : rewards.getKeys(false)) {
                String kind = key.toLowerCase(Locale.ROOT);
                if (!validKind(kind)) {
                    logger.severe(
                            "discovery-crates.rewards." + key
                                    + " is not a discovery kind. Discovery crate rewards are disabled."
                    );
                    return none();
                }
                if (cratesByKey.containsKey(kind)) {
                    logger.severe(
                            "discovery-crates.rewards." + key
                                    + " repeats a discovery kind. Discovery crate rewards are disabled."
                    );
                    return none();
                }
                Object raw = rewards.get(key);
                if (!(raw instanceof String crateName) || !singleToken(crateName.trim())) {
                    logger.severe(
                            "discovery-crates.rewards." + key
                                    + " must be a single token. Discovery crate rewards are disabled."
                    );
                    return none();
                }
                cratesByKey.put(kind, crateName.trim());
            }
        }
        return new DiscoveryCrates(enabled, Map.copyOf(cratesByKey), commandTemplate);
    }

    public Optional<String> crate(String world, String entityType) {
        if (!enabled) {
            return Optional.empty();
        }
        String name = cratesByKey.get(key(world, entityType));
        if (name == null) {
            return Optional.empty();
        }
        return Optional.of(name);
    }

    public Optional<String> command(String world, String entityType, String playerName) {
        if (!enabled || commandTemplate.isEmpty() || !singleToken(playerName)) {
            return Optional.empty();
        }
        String crateName = cratesByKey.get(key(world, entityType));
        if (crateName == null) {
            return Optional.empty();
        }
        return Optional.of(commandTemplate.replace(CRATE, crateName).replace(PLAYER, playerName));
    }

    boolean enabled() {
        return enabled;
    }

    String configuredCrate(String key) {
        if (key == null) {
            return "";
        }
        String name = cratesByKey.get(key.toLowerCase(Locale.ROOT));
        return name == null ? "" : name;
    }

    public String configuredCommand() {
        return commandTemplate;
    }

    private static String template(ConfigurationSection crates, Logger logger) {
        String raw = crates.getString("command");
        if (raw == null) {
            logger.severe("discovery-crates.command is missing or malformed. Discovery crate rewards are disabled.");
            return "";
        }
        String command = raw.trim();
        if (command.isEmpty()
                || command.startsWith("/")
                || !command.contains(PLAYER)
                || !command.contains(CRATE)) {
            logger.severe("discovery-crates.command is missing or malformed. Discovery crate rewards are disabled.");
            return "";
        }
        return command;
    }

    private static boolean validKind(String key) {
        int split = key.indexOf('_');
        if (split <= 0 || split != key.lastIndexOf('_')) {
            return false;
        }
        return WORLDS.contains(key.substring(0, split)) && KINDS.contains(key.substring(split + 1));
    }

    private static String key(String world, String entityType) {
        if (world == null || entityType == null) {
            return "";
        }
        return world.toLowerCase(Locale.ROOT) + "_" + entityType.toLowerCase(Locale.ROOT);
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
}
