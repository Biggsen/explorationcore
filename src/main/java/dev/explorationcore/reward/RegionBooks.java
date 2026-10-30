package dev.explorationcore.reward;

import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Locale;
import java.util.Optional;
import java.util.logging.Logger;

public final class RegionBooks {
    private static final String PLAYER = "{player}";
    private static final String REGION = "{region}";

    private final boolean enabled;
    private final String commandTemplate;

    private RegionBooks(boolean enabled, String commandTemplate) {
        this.enabled = enabled;
        this.commandTemplate = commandTemplate;
    }

    public static RegionBooks none() {
        return new RegionBooks(false, "");
    }

    public static RegionBooks load(Configuration configuration, Logger logger) {
        if (configuration == null || !configuration.contains("region-books")) {
            return none();
        }
        ConfigurationSection books = configuration.getConfigurationSection("region-books");
        if (books == null) {
            logger.severe("region-books is malformed. Region book permissions are disabled.");
            return none();
        }
        String commandTemplate = template(books, logger);
        if (commandTemplate.isEmpty()) {
            return none();
        }
        return new RegionBooks(books.getBoolean("enabled", false), commandTemplate);
    }

    public Optional<String> command(String world, String entityType, String entityId, String playerName) {
        if (!enabled || commandTemplate.isEmpty() || !singleToken(playerName)) {
            return Optional.empty();
        }
        if (!"overworld".equals(normalize(world)) || !"region".equals(normalize(entityType))) {
            return Optional.empty();
        }
        String region = regionToken(entityId);
        if (region.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(commandTemplate.replace(REGION, region).replace(PLAYER, playerName));
    }

    boolean enabled() {
        return enabled;
    }

    public String configuredCommand() {
        return commandTemplate;
    }

    private static String template(ConfigurationSection books, Logger logger) {
        String raw = books.getString("command");
        if (raw == null) {
            logger.severe("region-books.command is missing or malformed. Region book permissions are disabled.");
            return "";
        }
        String command = raw.trim();
        if (command.isEmpty()
                || command.startsWith("/")
                || !command.contains(PLAYER)
                || !command.contains(REGION)) {
            logger.severe("region-books.command is missing or malformed. Region book permissions are disabled.");
            return "";
        }
        return command;
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private static String regionToken(String entityId) {
        if (entityId == null) {
            return "";
        }
        String region = entityId.trim().toLowerCase(Locale.ROOT);
        if (region.isEmpty()) {
            return "";
        }
        for (int index = 0; index < region.length(); index++) {
            char character = region.charAt(index);
            boolean letter = character >= 'a' && character <= 'z';
            boolean digit = character >= '0' && character <= '9';
            if (!letter && !digit && character != '_' && character != '-') {
                return "";
            }
        }
        return region;
    }

    private static boolean singleToken(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        for (int index = 0; index < value.length(); index++) {
            if (Character.isWhitespace(value.charAt(index))) {
                return false;
            }
        }
        return true;
    }
}
