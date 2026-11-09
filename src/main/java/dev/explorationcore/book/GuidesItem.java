package dev.explorationcore.book;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public final class GuidesItem {
    public static final String NAME = "Guides";
    private static final NamespacedKey MARKER = new NamespacedKey("explorationcore", "guides");
    private static final Component DISPLAY_NAME = Component.text(NAME).decoration(TextDecoration.ITALIC, false);

    private GuidesItem() {
    }

    public static ItemStack create() {
        ItemStack stack = new ItemStack(Material.KNOWLEDGE_BOOK, 1);
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            throw new IllegalStateException("knowledge book has no item meta");
        }
        meta.displayName(DISPLAY_NAME);
        meta.getPersistentDataContainer().set(MARKER, PersistentDataType.BYTE, (byte) 1);
        stack.setItemMeta(meta);
        return stack;
    }

    public static boolean isGuide(ItemStack stack) {
        if (stack == null || stack.getType() != Material.KNOWLEDGE_BOOK) {
            return false;
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return false;
        }
        Byte mark = meta.getPersistentDataContainer().get(MARKER, PersistentDataType.BYTE);
        return mark != null && mark == (byte) 1;
    }
}
