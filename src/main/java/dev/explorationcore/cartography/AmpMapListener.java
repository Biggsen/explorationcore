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
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.LODESTONE) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack held = player.getInventory().getItemInMainHand();
        if (held == null || held.getType() != Material.FILLED_MAP) {
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
        MapView view = mapView(held);
        if (view == null) {
            tell(player, "That map has no stored data.");
            return;
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
            return;
        }
        SharedMapMarkers.Outcome outcome;
        if (store == null) {
            tell(player, "That map could not be marked.");
            return;
        }
        restore.ensure(view);
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
            return;
        }
        outcome = SharedMapMarkers.mark(
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
            return;
        }
        if (inserted) {
            tell(player, "Marked " + amp.name() + " on your map.");
        } else {
            tell(player, amp.name() + " is already marked on this map.");
        }
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
}
