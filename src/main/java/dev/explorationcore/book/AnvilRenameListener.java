package dev.explorationcore.book;

import dev.explorationcore.atlas.AtlasItem;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.ItemStack;

public final class AnvilRenameListener implements Listener {
    @EventHandler
    public void blockRename(PrepareAnvilEvent event) {
        if (!protectedItem(event.getInventory().getItem(0))) {
            return;
        }
        event.setResult(null);
    }

    @EventHandler(ignoreCancelled = true)
    public void blockTake(InventoryClickEvent event) {
        if (event.getView().getType() != InventoryType.ANVIL || event.getRawSlot() != 2) {
            return;
        }
        if (!protectedItem(event.getInventory().getItem(0))) {
            return;
        }
        event.setCancelled(true);
    }

    private static boolean protectedItem(ItemStack stack) {
        return ExplorationJournal.isJournal(stack) || GuidesItem.isGuide(stack) || AtlasItem.isAtlas(stack);
    }
}
