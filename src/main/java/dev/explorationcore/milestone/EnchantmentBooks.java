package dev.explorationcore.milestone;

import dev.explorationcore.milestone.Milestone.ItemReward.StoredEnchant;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;

import java.util.List;

final class EnchantmentBooks {
    private EnchantmentBooks() {
    }

    static void check(List<StoredEnchant> enchantments, String where) {
        for (StoredEnchant stored : enchantments) {
            Enchantment enchantment = find(stored.id());
            String at = where + "." + stored.id();
            if (enchantment == null) {
                throw new IllegalArgumentException(at + " is not an enchantment.");
            }
            if (stored.level() < enchantment.getStartLevel() || stored.level() > enchantment.getMaxLevel()) {
                throw new IllegalArgumentException(at + " has an invalid level.");
            }
        }
    }

    static void apply(ItemStack stack, List<StoredEnchant> enchantments) {
        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) stack.getItemMeta();
        if (meta == null) {
            throw new IllegalStateException("enchanted book has no item meta");
        }
        for (StoredEnchant stored : enchantments) {
            Enchantment enchantment = find(stored.id());
            if (enchantment == null || !meta.addStoredEnchant(enchantment, stored.level(), false)) {
                throw new IllegalStateException("could not store " + stored.id() + " " + stored.level());
            }
        }
        stack.setItemMeta(meta);
    }

    private static Enchantment find(String id) {
        NamespacedKey key;
        try {
            key = NamespacedKey.minecraft(id);
        } catch (IllegalArgumentException exception) {
            return null;
        }
        return RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT).get(key);
    }
}
