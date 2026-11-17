package dev.explorationcore.cartography;

import dev.explorationcore.ExplorationCorePlugin;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapView;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.logging.Level;

public final class CartographyTestCommand implements CommandExecutor {
    public static final String PERMISSION = "explorationcore.cartographytest";

    private final ExplorationCorePlugin plugin;

    public CartographyTestCommand(ExplorationCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        try {
            inspect(sender);
        } catch (RuntimeException exception) {
            plugin.getLogger().log(Level.SEVERE, "Cartography test failed", exception);
            sender.sendMessage(Component.text("That map could not be inspected."));
        }
        return true;
    }

    private void inspect(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only a player can inspect a held map."));
            return;
        }
        if (!player.hasPermission(PERMISSION)) {
            player.sendMessage(Component.text("You do not have permission to use this command."));
            return;
        }
        ItemStack held = player.getInventory().getItemInMainHand();
        if (held == null || held.getType() != Material.FILLED_MAP) {
            player.sendMessage(Component.text("Hold a filled map in your main hand."));
            return;
        }
        MapView view = mapView(held);
        if (view == null) {
            player.sendMessage(Component.text("That map has no stored data."));
            return;
        }
        AmpMarkerRestore restore = plugin.ampMarkerRestore();
        if (restore != null) {
            restore.ensure(view);
        }
        OptionalInt stored = restore == null || !restore.available()
                ? OptionalInt.empty()
                : storedCount(restore, view.getId());
        for (String line : CartographyReport.lines(inspection(player, held, view, stored))) {
            player.sendMessage(Component.text(line));
        }
    }

    private static CartographyReport.Inspection inspection(
            Player player,
            ItemStack held,
            MapView view,
            OptionalInt storedMarkers
    ) {
        World world = view.getWorld();
        String worldName = null;
        UUID worldId = null;
        String dimension = null;
        if (world != null) {
            worldName = world.getName();
            worldId = world.getUID();
            dimension = CartographyReport.dimensionName(world.getKey().toString());
        }
        return new CartographyReport.Inspection(
                view.getId(),
                worldName,
                worldId,
                dimension,
                MapScale.of(view.getScale()),
                view.getCenterX(),
                view.getCenterZ(),
                view.isLocked(),
                view.isVirtual(),
                StoredMapColors.explored(view),
                held.getAmount(),
                others(player),
                SharedMapMarkers.read(view),
                storedMarkers
        );
    }

    private OptionalInt storedCount(AmpMarkerRestore restore, int mapId) {
        try {
            return OptionalInt.of(restore.storedCount(mapId));
        } catch (SQLException exception) {
            plugin.getLogger().log(Level.WARNING, "Could not read stored amp markers for map " + mapId, exception);
            return OptionalInt.empty();
        }
    }

    private static List<CartographyReport.Sighting> others(Player player) {
        List<CartographyReport.Sighting> sightings = new ArrayList<>();
        PlayerInventory inventory = player.getInventory();
        ItemStack[] storage = inventory.getStorageContents();
        int heldSlot = inventory.getHeldItemSlot();
        if (storage != null) {
            for (int slot = 0; slot < storage.length; slot++) {
                if (slot == heldSlot) {
                    continue;
                }
                String place = slot < 9 ? "hotbar " + (slot + 1) : "inventory " + (slot - 8);
                add(sightings, storage[slot], place);
            }
        }
        add(sightings, inventory.getItemInOffHand(), "offhand");
        ItemStack[] ender = player.getEnderChest().getContents();
        if (ender != null) {
            for (int slot = 0; slot < ender.length; slot++) {
                add(sightings, ender[slot], "ender " + (slot + 1));
            }
        }
        return sightings;
    }

    private static void add(List<CartographyReport.Sighting> sightings, ItemStack stack, String place) {
        CartographyReport.Sighting sighting = sighting(stack, place);
        if (sighting != null) {
            sightings.add(sighting);
        }
    }

    private static CartographyReport.Sighting sighting(ItemStack stack, String place) {
        MapView view = mapView(stack);
        if (view == null) {
            return null;
        }
        World world = view.getWorld();
        UUID worldId = null;
        String dimension = null;
        if (world != null) {
            worldId = world.getUID();
            dimension = CartographyReport.dimensionName(world.getKey().toString());
        }
        return new CartographyReport.Sighting(
                view.getId(),
                place,
                worldId,
                dimension,
                MapScale.of(view.getScale()),
                view.getCenterX(),
                view.getCenterZ()
        );
    }

    private static MapView mapView(ItemStack stack) {
        if (stack == null || stack.getType() != Material.FILLED_MAP) {
            return null;
        }
        if (!(stack.getItemMeta() instanceof MapMeta meta) || !meta.hasMapView()) {
            return null;
        }
        return meta.getMapView();
    }
}
