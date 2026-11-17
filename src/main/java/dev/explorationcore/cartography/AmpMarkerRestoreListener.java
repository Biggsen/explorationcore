package dev.explorationcore.cartography;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.ItemStack;

public final class AmpMarkerRestoreListener implements Listener {
    private final AmpMarkerRestore restore;

    public AmpMarkerRestoreListener(AmpMarkerRestore restore) {
        this.restore = restore;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        restore.ensureItem(player.getInventory().getItemInMainHand());
        restore.ensureItem(player.getInventory().getItemInOffHand());
    }

    @EventHandler(ignoreCancelled = true)
    public void onHeld(PlayerItemHeldEvent event) {
        ItemStack item = event.getPlayer().getInventory().getItem(event.getNewSlot());
        restore.ensureItem(item);
    }

    @EventHandler(ignoreCancelled = true)
    public void onSwap(PlayerSwapHandItemsEvent event) {
        restore.ensureItem(event.getMainHandItem());
        restore.ensureItem(event.getOffHandItem());
    }

    @EventHandler
    public void onChunk(ChunkLoadEvent event) {
        restore.ensureChunk(event.getChunk());
    }
}
