package dev.explorationcore.milestone;

import dev.explorationcore.milestone.Milestone.ItemReward;
import org.bukkit.Material;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public final class MilestoneItemProbe extends JavaPlugin {
    @Override
    public void onEnable() {
        Path result;
        try {
            result = Path.of(Files.readString(Path.of("milestone-item-result.path")).trim());
        } catch (IOException exception) {
            exception.printStackTrace();
            getServer().shutdown();
            return;
        }
        String report;
        try {
            report = run();
        } catch (RuntimeException | InvalidConfigurationException exception) {
            report = "EXCEPTION " + exception + "\n" + trace(exception);
        }
        try {
            Files.writeString(result, report);
        } catch (IOException exception) {
            exception.printStackTrace();
        }
        getServer().shutdown();
    }

    private static String run() throws InvalidConfigurationException {
        StringBuilder report = new StringBuilder();
        Milestones books = load("""
                milestones:
                  hearts_discovered:
                    - id: hearts_book
                      threshold: 1
                      display-name: Heart Wanderer
                      message: Hearts.
                      rewards:
                        items:
                          - material: enchanted_book
                            amount: 1
                            enchantments:
                              efficiency: 4
                              unbreaking: 3
                """);
        if (books.isEmpty()) {
            return "FAIL book was rejected\n";
        }
        ItemReward book = books.of("hearts_discovered").get(0).items().get(0);
        report.append("book-label=").append(book.label()).append('\n');
        if (!"Enchanted Book (Efficiency IV, Unbreaking III)".equals(book.label())) {
            return report.append("FAIL book label\n").toString();
        }
        List<ItemStack> bookStacks = book.stacks();
        if (!splitMatches(bookStacks, 1)) {
            return report.append("FAIL book split\n").toString();
        }
        List<String> stored = new ArrayList<>();
        for (ItemStack stack : bookStacks) {
            ItemMeta meta = stack.getItemMeta();
            if (!(meta instanceof EnchantmentStorageMeta storedMeta) || meta.hasDisplayName()) {
                return report.append("FAIL book meta\n").toString();
            }
            for (Map.Entry<Enchantment, Integer> entry : storedMeta.getStoredEnchants().entrySet()) {
                stored.add(entry.getKey().getKey().getKey() + ":" + entry.getValue());
            }
        }
        stored.sort(Comparator.naturalOrder());
        report.append("book-enchants=").append(String.join(",", stored)).append('\n');
        if (!List.of("efficiency:4", "unbreaking:3").equals(stored)) {
            return report.append("FAIL book enchants\n").toString();
        }
        if (!load(levelYaml("efficiency: 6")).isEmpty()) {
            return report.append("FAIL over-max loaded\n").toString();
        }
        report.append("over-max=disabled\n");
        if (!load(levelYaml("not_an_enchant: 1")).isEmpty()) {
            return report.append("FAIL unknown loaded\n").toString();
        }
        report.append("unknown=disabled\n");
        Milestones potions = load("""
                milestones:
                  hearts_discovered:
                    - id: hearts_potions
                      threshold: 1
                      display-name: Heart Wanderer
                      message: Hearts.
                      rewards:
                        items:
                          - material: potion
                            amount: 1
                            potion-type: regeneration
                          - material: splash_potion
                            amount: 2
                            potion-type: strong_healing
                          - material: lingering_potion
                            amount: 1
                            potion-type: long_swiftness
                """);
        if (potions.isEmpty()) {
            return report.append("FAIL potions rejected\n").toString();
        }
        List<ItemReward> items = potions.of("hearts_discovered").get(0).items();
        if (!potion(report, items.get(0), Material.POTION, 1, PotionType.REGENERATION)) {
            return report.append("FAIL regular potion\n").toString();
        }
        if (!potion(report, items.get(1), Material.SPLASH_POTION, 2, PotionType.STRONG_HEALING)) {
            return report.append("FAIL splash potion\n").toString();
        }
        if (!potion(report, items.get(2), Material.LINGERING_POTION, 1, PotionType.LONG_SWIFTNESS)) {
            return report.append("FAIL lingering potion\n").toString();
        }
        report.append("status=ok\n");
        return report.toString();
    }

    private static boolean potion(
            StringBuilder report,
            ItemReward item,
            Material material,
            int amount,
            PotionType type
    ) {
        List<ItemStack> stacks = item.stacks();
        report.append(material.name()).append("-stacks=").append(stacks.size()).append('\n');
        if (item.material() != material || item.potionType() != type || !splitMatches(stacks, amount)) {
            return false;
        }
        int total = 0;
        for (ItemStack stack : stacks) {
            total += stack.getAmount();
            ItemMeta meta = stack.getItemMeta();
            if (!(meta instanceof PotionMeta potionMeta)
                    || potionMeta.hasDisplayName()
                    || potionMeta.getBasePotionType() != type
                    || stack.getType() != material) {
                return false;
            }
        }
        return total == amount;
    }

    private static boolean splitMatches(List<ItemStack> stacks, int amount) {
        if (stacks.isEmpty()) {
            return false;
        }
        int maxStack = stacks.get(0).getMaxStackSize();
        if (maxStack < 1) {
            return false;
        }
        int total = 0;
        for (ItemStack stack : stacks) {
            if (stack.getAmount() < 1 || stack.getAmount() > maxStack || stack.getMaxStackSize() != maxStack) {
                return false;
            }
            total += stack.getAmount();
        }
        int expected = (amount + maxStack - 1) / maxStack;
        return total == amount && stacks.size() == expected;
    }

    private static String levelYaml(String enchantment) {
        return """
                milestones:
                  hearts_discovered:
                    - id: hearts_book
                      threshold: 1
                      display-name: Heart Wanderer
                      message: Hearts.
                      rewards:
                        items:
                          - material: enchanted_book
                            amount: 1
                            enchantments:
                              %s
                """.formatted(enchantment);
    }

    private static Milestones load(String yaml) throws InvalidConfigurationException {
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.loadFromString(yaml);
        return Milestones.load(configuration, Logger.getLogger("milestone-item-probe"), "acb {player} {amount}");
    }

    private static String trace(Throwable throwable) {
        StringBuilder builder = new StringBuilder();
        for (StackTraceElement element : throwable.getStackTrace()) {
            builder.append("  at ").append(element).append('\n');
        }
        return builder.toString();
    }
}
