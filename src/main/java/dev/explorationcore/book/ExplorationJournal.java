package dev.explorationcore.book;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public final class ExplorationJournal {
    public static final String NAME = "Exploration Journal";
    private static final NamespacedKey MARKER = new NamespacedKey("explorationcore", "journal");
    private static final NamespacedKey MODEL = NamespacedKey.minecraft("written_book");
    private static final Component DISPLAY_NAME = Component.text(NAME, NamedTextColor.WHITE)
            .decoration(TextDecoration.ITALIC, false);

    private ExplorationJournal() {
    }

    public static ItemStack create() {
        ItemStack stack = new ItemStack(Material.KNOWLEDGE_BOOK, 1);
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            throw new IllegalStateException("knowledge book has no item meta");
        }
        meta.displayName(DISPLAY_NAME);
        meta.setItemModel(MODEL);
        meta.getPersistentDataContainer().set(MARKER, PersistentDataType.BYTE, (byte) 1);
        stack.setItemMeta(meta);
        return stack;
    }

    public static boolean isJournal(ItemStack stack) {
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
