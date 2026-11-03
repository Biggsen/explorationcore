package dev.explorationcore.milestone;

import dev.explorationcore.counter.DiscoveryCounters;
import dev.explorationcore.milestone.Milestone.ItemReward;
import org.bukkit.Material;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.potion.PotionType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

public final class Milestones {
    private static final boolean ITEM_REGISTRY_READY = itemRegistryReady();
    private static final Set<String> MILESTONE_FIELDS = Set.of(
            "id", "threshold", "display-name", "message", "rewards", "promotion", "broadcast"
    );
    private static final Set<String> REWARD_FIELDS = Set.of("xp", "items", "claimblocks");
    private static final Set<String> PROMOTION_FIELDS = Set.of("command");
    private static final Set<String> ITEM_FIELDS = Set.of(
            "material", "amount", "name", "enchantments", "potion-type"
    );

    private final Map<String, List<Milestone>> byCounter;

    private Milestones(Map<String, List<Milestone>> byCounter) {
        this.byCounter = byCounter;
    }

    public static Milestones none() {
        return new Milestones(Map.of());
    }

    public static Milestones load(
            Configuration configuration,
            Logger logger,
            String claimblocksCommand,
            DiscoveryCounters counters
    ) {
        if (configuration == null || !configuration.contains("milestones")) {
            return fail(logger, "milestones.yml has no milestones section.");
        }
        ConfigurationSection section = configuration.getConfigurationSection("milestones");
        if (section == null) {
            return fail(logger, "milestones is malformed.");
        }
        Map<String, List<Milestone>> byCounter = new LinkedHashMap<>();
        Set<String> ids = new HashSet<>();
        try {
            for (String counter : section.getKeys(false)) {
                if (!counters.known(counter)) {
                    throw new IllegalArgumentException("milestones." + counter + " is not a discovery counter.");
                }
                if (!section.isList(counter)) {
                    throw new IllegalArgumentException("milestones." + counter + " is malformed.");
                }
                List<?> raw = section.getList(counter);
                if (raw == null) {
                    throw new IllegalArgumentException("milestones." + counter + " is malformed.");
                }
                List<Milestone> milestones = new ArrayList<>();
                for (int index = 0; index < raw.size(); index++) {
                    Object entry = raw.get(index);
                    if (!(entry instanceof Map<?, ?> map)) {
                        throw new IllegalArgumentException(
                                "milestones." + counter + "[" + index + "] is malformed."
                        );
                    }
                    Milestone milestone = parse(counter, index, map);
                    if (!ids.add(milestone.id())) {
                        throw new IllegalArgumentException(
                                "milestones." + counter + "[" + index + "].id is duplicated."
                        );
                    }
                    milestones.add(milestone);
                }
                byCounter.put(counter, List.copyOf(milestones));
            }
        } catch (IllegalArgumentException exception) {
            return fail(logger, exception.getMessage());
        }
        Milestones loaded = new Milestones(Map.copyOf(byCounter));
        if (loaded.needsClaimblocks() && (claimblocksCommand == null || claimblocksCommand.isBlank())) {
            return fail(
                    logger,
                    "milestones grant claimblocks, and rewards.claimblocks-command is missing or malformed."
            );
        }
        if (loaded.isEmpty()) {
            logger.info("No milestones configured.");
            return none();
        }
        logger.info("Milestones loaded: " + loaded.size());
        return loaded;
    }

    public List<Milestone> of(String counter) {
        List<Milestone> milestones = byCounter.get(counter);
        return milestones == null ? List.of() : milestones;
    }

    public boolean isEmpty() {
        return byCounter.isEmpty() || byCounter.values().stream().allMatch(List::isEmpty);
    }

    public int size() {
        int total = 0;
        for (List<Milestone> milestones : byCounter.values()) {
            total += milestones.size();
        }
        return total;
    }

    public String summary() {
        StringBuilder builder = new StringBuilder();
        for (List<Milestone> milestones : byCounter.values()) {
            for (Milestone milestone : milestones) {
                if (!builder.isEmpty()) {
                    builder.append(", ");
                }
                builder.append(milestone.id()).append(" at ").append(milestone.threshold());
            }
        }
        return builder.toString();
    }

    private boolean needsClaimblocks() {
        for (List<Milestone> milestones : byCounter.values()) {
            for (Milestone milestone : milestones) {
                if (milestone.claimblocks() > 0) {
                    return true;
                }
            }
        }
        return false;
    }

    private static Milestone parse(String counter, int index, Map<?, ?> map) {
        String where = "milestones." + counter + "[" + index + "]";
        requireFields(map, where, MILESTONE_FIELDS);
        String id = text(map.get("id"), where + ".id");
        if (containsWhitespace(id)) {
            throw new IllegalArgumentException(where + ".id must be a single token.");
        }
        long threshold = positiveLong(map.get("threshold"), where + ".threshold");
        String displayName = text(map.get("display-name"), where + ".display-name");
        String message = text(map.get("message"), where + ".message");
        int xp = 0;
        List<ItemReward> items = List.of();
        int claimblocks = 0;
        if (map.containsKey("rewards")) {
            Object raw = map.get("rewards");
            if (!(raw instanceof Map<?, ?> rewards)) {
                throw new IllegalArgumentException(where + ".rewards is malformed.");
            }
            requireFields(rewards, where + ".rewards", REWARD_FIELDS);
            if (rewards.containsKey("xp")) {
                xp = positiveInt(rewards.get("xp"), where + ".rewards.xp");
            }
            if (rewards.containsKey("items")) {
                items = items(rewards.get("items"), where + ".rewards.items");
            }
            if (rewards.containsKey("claimblocks")) {
                claimblocks = positiveInt(rewards.get("claimblocks"), where + ".rewards.claimblocks");
            }
        }
        String promotion = null;
        if (map.containsKey("promotion")) {
            Object raw = map.get("promotion");
            if (!(raw instanceof Map<?, ?> promotionMap)) {
                throw new IllegalArgumentException(where + ".promotion is malformed.");
            }
            requireFields(promotionMap, where + ".promotion", PROMOTION_FIELDS);
            promotion = text(promotionMap.get("command"), where + ".promotion.command");
            if (promotion.startsWith("/")
                    || promotion.indexOf('\n') >= 0
                    || promotion.indexOf('\r') >= 0
                    || !promotion.contains("{player}")) {
                throw new IllegalArgumentException(where + ".promotion.command is malformed.");
            }
        }
        String broadcast = null;
        if (map.containsKey("broadcast")) {
            broadcast = text(map.get("broadcast"), where + ".broadcast");
            if (broadcast.indexOf('\n') >= 0 || broadcast.indexOf('\r') >= 0) {
                throw new IllegalArgumentException(where + ".broadcast is malformed.");
            }
        }
        return new Milestone(
                id,
                counter,
                threshold,
                displayName,
                message,
                xp,
                items,
                claimblocks,
                promotion,
                broadcast
        );
    }

    private static List<ItemReward> items(Object raw, String where) {
        if (!(raw instanceof List<?> list)) {
            throw new IllegalArgumentException(where + " is malformed.");
        }
        List<ItemReward> items = new ArrayList<>();
        for (int index = 0; index < list.size(); index++) {
            Object entry = list.get(index);
            String at = where + "[" + index + "]";
            if (entry instanceof String text) {
                items.add(compactItem(text, at));
            } else if (entry instanceof Map<?, ?> map) {
                items.add(structuredItem(map, at));
            } else {
                throw new IllegalArgumentException(at + " is malformed.");
            }
        }
        return List.copyOf(items);
    }

    private static ItemReward compactItem(String text, String where) {
        String[] parts = text.trim().split("\\s+");
        if (parts.length != 2) {
            throw new IllegalArgumentException(where + " is malformed.");
        }
        int amount;
        try {
            amount = Integer.parseInt(parts[1]);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(where + " is malformed.");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException(where + " must be a positive quantity.");
        }
        return new ItemReward(itemMaterial(parts[0], where), amount);
    }

    private static ItemReward structuredItem(Map<?, ?> map, String where) {
        requireFields(map, where, ITEM_FIELDS);
        int amount = positiveInt(map.get("amount"), where + ".amount");
        String displayName = null;
        if (map.containsKey("name")) {
            displayName = text(map.get("name"), where + ".name");
        }
        Material material = itemMaterial(text(map.get("material"), where + ".material"), where);
        boolean enchanted = map.containsKey("enchantments");
        boolean potion = map.containsKey("potion-type");
        if (enchanted && potion) {
            throw new IllegalArgumentException(where + " combines enchantments and potion-type.");
        }
        List<ItemReward.StoredEnchant> enchantments = List.of();
        PotionType potionType = null;
        if (enchanted) {
            if (material != Material.ENCHANTED_BOOK) {
                throw new IllegalArgumentException(where + ".enchantments is not valid for this item.");
            }
            enchantments = enchantments(map.get("enchantments"), where + ".enchantments");
        }
        if (potionMaterial(material)) {
            if (!potion) {
                throw new IllegalArgumentException(where + ".potion-type is missing or malformed.");
            }
            potionType = potionType(map.get("potion-type"), where + ".potion-type");
        } else if (potion) {
            throw new IllegalArgumentException(where + ".potion-type is not valid for this item.");
        }
        return new ItemReward(material, amount, displayName, enchantments, potionType);
    }

    private static boolean potionMaterial(Material material) {
        return material == Material.POTION
                || material == Material.SPLASH_POTION
                || material == Material.LINGERING_POTION;
    }

    private static List<ItemReward.StoredEnchant> enchantments(Object raw, String where) {
        if (!(raw instanceof Map<?, ?> map) || map.isEmpty()) {
            throw new IllegalArgumentException(where + " is malformed.");
        }
        List<ItemReward.StoredEnchant> enchantments = new ArrayList<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!(entry.getKey() instanceof String key)) {
                throw new IllegalArgumentException(where + " is malformed.");
            }
            String id = enchantmentId(key, where);
            int level = positiveInt(entry.getValue(), where + "." + id);
            enchantments.add(new ItemReward.StoredEnchant(id, level));
        }
        if (!registryReady()) {
            throw new IllegalArgumentException(where + " needs the server registry.");
        }
        resolveEnchantments(enchantments, where);
        return List.copyOf(enchantments);
    }

    private static void resolveEnchantments(List<ItemReward.StoredEnchant> enchantments, String where) {
        EnchantmentBooks.check(enchantments, where);
    }

    private static String enchantmentId(String raw, String where) {
        String token = raw.trim().toLowerCase(Locale.ROOT);
        if (token.startsWith("minecraft:")) {
            token = token.substring("minecraft:".length());
        }
        if (token.isEmpty() || token.indexOf(':') >= 0 || !enchantmentToken(token)) {
            throw new IllegalArgumentException(where + " is malformed.");
        }
        return token;
    }

    private static boolean enchantmentToken(String token) {
        for (int index = 0; index < token.length(); index++) {
            char character = token.charAt(index);
            boolean ok = (character >= 'a' && character <= 'z')
                    || (character >= '0' && character <= '9')
                    || character == '_'
                    || character == '.'
                    || character == '-'
                    || character == '/';
            if (!ok) {
                return false;
            }
        }
        return true;
    }

    private static PotionType potionType(Object raw, String where) {
        String token = text(raw, where).toLowerCase(Locale.ROOT).replace('-', '_');
        if (token.startsWith("minecraft:")) {
            token = token.substring("minecraft:".length());
        }
        if (token.isEmpty() || token.indexOf(':') >= 0) {
            throw new IllegalArgumentException(where + " is not a potion.");
        }
        try {
            return PotionType.valueOf(token.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(where + " is not a potion.");
        }
    }

    private static boolean registryReady() {
        try {
            Class<?> type = Class.forName("io.papermc.paper.registry.RegistryAccess");
            return type.getMethod("registryAccess").invoke(null) != null;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return false;
        }
    }

    private static Material itemMaterial(String token, String where) {
        Material material = Material.matchMaterial(token);
        if (!giveable(material)) {
            throw new IllegalArgumentException(where + " is not an item.");
        }
        return material;
    }

    private static boolean giveable(Material material) {
        if (material == null
                || material == Material.AIR
                || material == Material.CAVE_AIR
                || material == Material.VOID_AIR) {
            return false;
        }
        if (!ITEM_REGISTRY_READY) {
            return true;
        }
        return material.isItem();
    }

    // Paper resolves items through the server registry. Tests only have the enum.
    private static boolean itemRegistryReady() {
        try {
            Class<?> type = Class.forName("io.papermc.paper.registry.RegistryAccess");
            return type.getMethod("registryAccess").invoke(null) != null;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return false;
        }
    }

    private static void requireFields(Map<?, ?> map, String where, Set<String> allowed) {
        for (Object key : map.keySet()) {
            if (!(key instanceof String name) || !allowed.contains(name)) {
                throw new IllegalArgumentException(where + " has an unknown field.");
            }
        }
    }

    private static String text(Object raw, String where) {
        if (!(raw instanceof String value)) {
            throw new IllegalArgumentException(where + " is missing or malformed.");
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(where + " is missing or malformed.");
        }
        return trimmed;
    }

    private static int positiveInt(Object raw, String where) {
        long value = positiveLong(raw, where);
        if (value > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(where + " must be a positive integer.");
        }
        return (int) value;
    }

    private static long positiveLong(Object raw, String where) {
        long value;
        if (raw instanceof Byte || raw instanceof Short || raw instanceof Integer) {
            value = ((Number) raw).longValue();
        } else if (raw instanceof Long longValue) {
            value = longValue;
        } else {
            throw new IllegalArgumentException(where + " must be a positive integer.");
        }
        if (value <= 0) {
            throw new IllegalArgumentException(where + " must be a positive integer.");
        }
        return value;
    }

    private static boolean containsWhitespace(String value) {
        for (int index = 0; index < value.length(); index++) {
            if (Character.isWhitespace(value.charAt(index))) {
                return true;
            }
        }
        return false;
    }

    private static Milestones fail(Logger logger, String reason) {
        logger.severe(reason + " Milestone rewards are disabled.");
        return none();
    }
}
