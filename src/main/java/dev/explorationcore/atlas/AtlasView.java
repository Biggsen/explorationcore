package dev.explorationcore.atlas;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class AtlasView {
    private AtlasView() {
    }

    public static Inventory create(AtlasHolder holder, ItemStack[] maps) {
        Inventory inventory = Bukkit.createInventory(holder, AtlasLayout.GUI_SIZE, Component.text(AtlasItem.NAME));
        ItemStack filler = filler();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            if (!AtlasLayout.mapGuiSlot(slot)) {
                inventory.setItem(slot, filler.clone());
            }
        }
        show(inventory, maps);
        return inventory;
    }

    public static void show(Inventory inventory, ItemStack[] maps) {
        for (int index = 0; index < AtlasLayout.CELLS; index++) {
            ItemStack map = maps == null ? null : maps[index];
            inventory.setItem(AtlasLayout.guiSlot(index), map == null || map.isEmpty() ? null : map.clone());
        }
    }

    private static ItemStack filler() {
        ItemStack stack = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setHideTooltip(true);
            stack.setItemMeta(meta);
        }
        return stack;
    }
}
