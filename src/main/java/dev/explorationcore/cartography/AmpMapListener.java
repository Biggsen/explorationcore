package dev.explorationcore.cartography;

import dev.explorationcore.ExplorationCorePlugin;
import dev.explorationcore.db.AmpMarkerStore;
import dev.explorationcore.feedback.NoticeChat;
import dev.explorationcore.location.LocationIndexLoader;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapView;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public final class AmpMapListener implements Listener {
    private final ExplorationCorePlugin plugin;
    private final LocationIndexLoader index;
    private final AmpMarkerStore store;
    private final AmpMarkerRestore restore;

    public AmpMapListener(
            ExplorationCorePlugin plugin,
            LocationIndexLoader index,
            AmpMarkerStore store,
            AmpMarkerRestore restore
    ) {
        this.plugin = plugin;
        this.index = index;
        this.store = store;
        this.restore = restore;
    }

    @EventHandler(ignoreCancelled = true)
    public void mark(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        EquipmentSlot hand = event.getHand();
        if (hand != EquipmentSlot.HAND && hand != EquipmentSlot.OFF_HAND) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.LODESTONE) {
            return;
        }
        Player player = event.getPlayer();
        EquipmentSlot chosen = mapHand(player);
        if (chosen == null) {
            return;
        }
        List<String> regions;
        try {
            regions = regionIds(block.getLocation());
        } catch (RuntimeException exception) {
            return;
        }
        AmpSelection.Amp amp = AmpSelection.select(index.current(), block.getWorld().getName(), regions);
        if (amp == null) {
            return;
        }
        event.setCancelled(true);
        event.setUseInteractedBlock(Event.Result.DENY);
        event.setUseItemInHand(Event.Result.DENY);
        if (hand != chosen) {
            return;
        }
        if (toggle(player, block, amp, player.getInventory().getItem(chosen))) {
            player.swingHand(chosen);
        }
    }

    private boolean toggle(Player player, Block block, AmpSelection.Amp amp, ItemStack held) {
        MapView view = mapView(held);
        if (view == null) {
            tell(player, "That map has no stored data.");
            return false;
        }
        World mapWorld = view.getWorld();
        UUID mapWorldId = mapWorld == null ? null : mapWorld.getUID();
        if (mapWorldId == null
                || !mapWorldId.equals(block.getWorld().getUID())
                || !MapGrid.contains(
                        MapScale.of(view.getScale()),
                        view.getCenterX(),
                        view.getCenterZ(),
                        block.getX(),
                        block.getZ()
                )) {
            tell(player, "This location is outside your map.");
            return false;
        }
        if (store == null) {
            tell(player, "That map could not be marked.");
            return false;
        }
        restore.ensure(view);
        Toggle removed = remove(player, view, amp, block);
        if (removed == Toggle.CHANGED) {
            return true;
        }
        if (removed == Toggle.UNCHANGED) {
            return false;
        }
        boolean inserted;
        try {
            inserted = store.insert(new AmpMarkerStore.Marker(
                    view.getId(),
                    amp.id(),
                    amp.kind(),
                    amp.name(),
                    block.getX(),
                    block.getY(),
                    block.getZ()
            ));
        } catch (SQLException exception) {
            plugin.getLogger().log(Level.WARNING, "Could not store amp marker " + amp.id(), exception);
            tell(player, "That map could not be marked.");
            return false;
        }
        SharedMapMarkers.Outcome outcome = SharedMapMarkers.mark(
                view,
                amp,
                block.getX(),
                block.getY(),
                block.getZ(),
                block.getWorld()
        );
        if (outcome == SharedMapMarkers.Outcome.UNAVAILABLE) {
            restore.forget(view.getId());
            tell(player, "That map could not be marked.");
            return false;
        }
        if (inserted) {
            tell(player, "Marked " + amp.name() + " on your map.");
            return true;
        }
        tell(player, amp.name() + " is already marked on this map.");
        return false;
    }

    private Toggle remove(Player player, MapView view, AmpSelection.Amp amp, Block block) {
        List<AmpMarkerStore.Marker> stored;
        try {
            stored = store.forMap(view.getId());
        } catch (SQLException exception) {
            plugin.getLogger().log(Level.WARNING, "Could not read amp marker " + amp.id(), exception);
            tell(player, "That marker could not be removed.");
            return Toggle.UNCHANGED;
        }
        AmpMarkerStore.Marker existing = null;
        for (AmpMarkerStore.Marker marker : stored) {
            if (amp.id().equals(marker.ampId())) {
                existing = marker;
                break;
            }
        }
        if (existing == null) {
            return Toggle.ABSENT;
        }
        if (SharedMapMarkers.unmark(view, amp.id()) == SharedMapMarkers.Outcome.UNAVAILABLE) {
            restore.forget(view.getId());
            tell(player, "That marker could not be removed.");
            return Toggle.UNCHANGED;
        }
        try {
            store.delete(view.getId(), amp.id());
        } catch (SQLException exception) {
            plugin.getLogger().log(Level.WARNING, "Could not remove amp marker " + amp.id(), exception);
            SharedMapMarkers.mark(view, amp, existing.x(), existing.y(), existing.z(), block.getWorld());
            tell(player, "That marker could not be removed.");
            return Toggle.UNCHANGED;
        }
        tell(player, "Removed " + amp.name() + " from your map.");
        return Toggle.CHANGED;
    }

    private static EquipmentSlot mapHand(Player player) {
        if (filledMap(player.getInventory().getItemInMainHand())) {
            return EquipmentSlot.HAND;
        }
        if (filledMap(player.getInventory().getItemInOffHand())) {
            return EquipmentSlot.OFF_HAND;
        }
        return null;
    }

    private static boolean filledMap(ItemStack stack) {
        return stack != null && stack.getType() == Material.FILLED_MAP;
    }

    private void tell(Player player, String message) {
        player.sendMessage(NoticeChat.line(plugin.messagePrefix(), message));
    }

    private static MapView mapView(ItemStack stack) {
        if (!(stack.getItemMeta() instanceof MapMeta meta) || !meta.hasMapView()) {
            return null;
        }
        return meta.getMapView();
    }

    private static List<String> regionIds(Location location) {
        ApplicableRegionSet set = WorldGuard.getInstance()
                .getPlatform()
                .getRegionContainer()
                .createQuery()
                .getApplicableRegions(BukkitAdapter.adapt(location));
        List<ProtectedRegion> regions = new ArrayList<>();
        for (ProtectedRegion region : set.getRegions()) {
            if (region != null && !ProtectedRegion.GLOBAL_REGION.equals(region.getId())) {
                regions.add(region);
            }
        }
        regions.sort(Comparator.comparingInt(ProtectedRegion::getPriority).reversed());
        List<String> ids = new ArrayList<>(regions.size());
        for (ProtectedRegion region : regions) {
            ids.add(region.getId());
        }
        return ids;
    }

    private enum Toggle {
        ABSENT,
        CHANGED,
        UNCHANGED
    }
}
