package dev.explorationcore.atlas;

import dev.explorationcore.ExplorationCorePlugin;
import dev.explorationcore.cartography.MapGrid;
import dev.explorationcore.cartography.MapScale;
import dev.explorationcore.feedback.NoticeChat;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapView;

import java.util.UUID;
import java.util.logging.Level;

public final class AtlasListener implements Listener {
    private final ExplorationCorePlugin plugin;

    public AtlasListener(ExplorationCorePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void openHeldAtlas(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        EquipmentSlot hand = event.getHand();
        if (hand == null || !beginOpen(event.getPlayer(), event.getItem(), hand)) {
            return;
        }
        event.setUseItemInHand(Event.Result.DENY);
        event.setUseInteractedBlock(Event.Result.DENY);
    }

    @EventHandler
    public void openHeldAtlasOnEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        EquipmentSlot hand = event.getHand();
        ItemStack item = hand == EquipmentSlot.OFF_HAND
                ? player.getInventory().getItemInOffHand()
                : player.getInventory().getItemInMainHand();
        if (!beginOpen(player, item, hand)) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!event.isCancelled() && insertIntoClosedAtlas(event, player)) {
            return;
        }
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof AtlasHolder holder)) {
            return;
        }
        UUID atlasId = holder.atlasId();
        if (event.getClick() == ClickType.CREATIVE) {
            if (event.getClickedInventory() == top) {
                event.setCancelled(true);
            }
            return;
        }
        if (event.getClick() == ClickType.DOUBLE_CLICK) {
            event.setCancelled(true);
            return;
        }
        Inventory clicked = event.getClickedInventory();
        if (clicked == null) {
            return;
        }
        if (clicked != top) {
            if (event.getClick() == ClickType.SHIFT_LEFT || event.getClick() == ClickType.SHIFT_RIGHT) {
                event.setCancelled(true);
                ItemStack stack = clone(event.getCurrentItem());
                int slot = event.getSlot();
                later(player, () -> shiftFromPlayer(player, atlasId, slot, stack));
            }
            return;
        }
        event.setCancelled(true);
        int raw = event.getRawSlot();
        if (!AtlasLayout.mapGuiSlot(raw)) {
            return;
        }
        ClickType type = event.getClick();
        if (type == ClickType.LEFT || type == ClickType.RIGHT) {
            ItemStack cursor = clone(event.getCursor());
            later(player, () -> press(player, atlasId, raw, cursor));
        } else if (type == ClickType.SHIFT_LEFT || type == ClickType.SHIFT_RIGHT) {
            later(player, () -> shiftFromAtlas(player, atlasId, raw));
        } else if (type == ClickType.NUMBER_KEY) {
            int hotbar = event.getHotbarButton();
            ItemStack hotbarStack = clone(player.getInventory().getItem(hotbar));
            later(player, () -> number(player, atlasId, raw, hotbar, hotbarStack));
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof AtlasHolder holder)) {
            return;
        }
        int mapCells = 0;
        boolean filler = false;
        int topSlots = 0;
        for (int raw : event.getRawSlots()) {
            if (raw >= top.getSize()) {
                continue;
            }
            topSlots++;
            if (AtlasLayout.mapGuiSlot(raw)) {
                mapCells++;
            } else {
                filler = true;
            }
        }
        if (topSlots == 0) {
            return;
        }
        event.setCancelled(true);
        if (filler || mapCells != 1 || topSlots != 1 || event.getRawSlots().size() != 1) {
            return;
        }
        ItemStack cursor = clone(event.getOldCursor());
        UUID atlasId = holder.atlasId();
        later(player, () -> drag(player, atlasId, cursor));
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof AtlasHolder)) {
            return;
        }
        event.getInventory().clear();
    }

    private boolean insertIntoClosedAtlas(InventoryClickEvent event, Player player) {
        ItemStack atlas = event.getCurrentItem();
        ItemStack cursor = event.getCursor();
        if (!AtlasItem.isAtlas(atlas) || !mapItem(cursor)) {
            return false;
        }
        ClickType click = event.getClick();
        if (click != ClickType.LEFT && click != ClickType.RIGHT) {
            return false;
        }
        if (event.getClickedInventory() == null || !inPlayerSlots(event)) {
            return false;
        }
        event.setCancelled(true);
        place(player, AtlasItem.id(atlas), true, -1, clone(cursor));
        return true;
    }

    private static boolean inPlayerSlots(InventoryClickEvent event) {
        if (event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
            return true;
        }
        return event.getView().getType() == InventoryType.CRAFTING;
    }

    private boolean beginOpen(Player player, ItemStack item, EquipmentSlot hand) {
        if (!AtlasItem.isAtlas(item)) {
            return false;
        }
        player.updateInventory();
        if (hand == EquipmentSlot.OFF_HAND && AtlasItem.isAtlas(player.getInventory().getItemInMainHand())) {
            return true;
        }
        UUID atlasId = AtlasItem.id(item);
        later(player, () -> open(player, atlasId));
        return true;
    }

    private void open(Player player, UUID atlasId) {
        Located atlas = find(player, atlasId);
        if (atlas == null) {
            return;
        }
        AtlasItem.Stored stored = read(player, atlas.stack);
        if (stored == null) {
            return;
        }
        AtlasHolder holder = new AtlasHolder(atlasId);
        Inventory inventory = AtlasView.create(holder, stored.maps());
        holder.attach(inventory);
        sendMaps(player, stored.maps());
        player.openInventory(inventory);
    }

    private void press(Player player, UUID atlasId, int guiSlot, ItemStack expectedCursor) {
        if (!same(player.getItemOnCursor(), expectedCursor)) {
            return;
        }
        Located atlas = find(player, atlasId);
        if (atlas == null) {
            close(player, atlasId);
            return;
        }
        AtlasItem.Stored stored = read(player, atlas.stack);
        if (stored == null) {
            return;
        }
        boolean cursorEmpty = expectedCursor == null || expectedCursor.isEmpty();
        int index = AtlasLayout.index(guiSlot);
        boolean cellEmpty = stored.map(index) == null;
        if (!cursorEmpty) {
            if (atlasId.equals(AtlasItem.id(expectedCursor))) {
                return;
            }
            AtlasClicks.Decision decision = AtlasClicks.press(
                    stored.board(),
                    offer(expectedCursor),
                    guiSlot,
                    cellEmpty
            );
            if (decision.message() != null) {
                tell(player, decision.message());
                return;
            }
            if (AtlasClicks.changedCell(stored.board(), decision.board()) >= 0) {
                place(player, atlasId, true, -1, expectedCursor);
            }
            return;
        }
        if (!cellEmpty) {
            take(player, atlas, stored, index, true, -1);
        }
    }

    private void shiftFromPlayer(Player player, UUID atlasId, int slot, ItemStack expected) {
        if (!same(player.getInventory().getItem(slot), expected) || !mapItem(expected)) {
            return;
        }
        place(player, atlasId, false, slot, expected);
    }

    private void shiftFromAtlas(Player player, UUID atlasId, int guiSlot) {
        Located atlas = find(player, atlasId);
        if (atlas == null) {
            close(player, atlasId);
            return;
        }
        AtlasItem.Stored stored = read(player, atlas.stack);
        if (stored == null) {
            return;
        }
        int index = AtlasLayout.index(guiSlot);
        ItemStack map = stored.map(index);
        if (map == null) {
            return;
        }
        int destination = room(player, map);
        AtlasClicks.Decision decision = AtlasClicks.shiftTake(stored.board(), index, destination >= 0);
        if (decision.given() == null) {
            return;
        }
        take(player, atlas, stored, index, false, destination);
    }

    private void number(Player player, UUID atlasId, int guiSlot, int hotbar, ItemStack expectedHotbar) {
        if (hotbar < 0 || hotbar > 8 || !same(player.getInventory().getItem(hotbar), expectedHotbar)) {
            return;
        }
        Located atlas = find(player, atlasId);
        if (atlas == null) {
            close(player, atlasId);
            return;
        }
        AtlasItem.Stored stored = read(player, atlas.stack);
        if (stored == null) {
            return;
        }
        int index = AtlasLayout.index(guiSlot);
        boolean hotbarEmpty = expectedHotbar == null || expectedHotbar.isEmpty();
        AtlasClicks.Decision decision = AtlasClicks.number(
                stored.board(),
                stored.map(index) == null,
                index,
                !atlas.cursor && atlas.slot == hotbar,
                hotbarEmpty ? null : offer(expectedHotbar),
                hotbarEmpty
        );
        if (decision.message() != null) {
            tell(player, decision.message());
            return;
        }
        if (decision.given() != null) {
            take(player, atlas, stored, index, false, hotbar);
            return;
        }
        if (AtlasClicks.changedCell(stored.board(), decision.board()) >= 0) {
            place(player, atlasId, false, hotbar, expectedHotbar);
        }
    }

    private void drag(Player player, UUID atlasId, ItemStack expectedCursor) {
        if (!same(player.getItemOnCursor(), expectedCursor)) {
            return;
        }
        Located atlas = find(player, atlasId);
        if (atlas == null) {
            close(player, atlasId);
            return;
        }
        AtlasItem.Stored stored = read(player, atlas.stack);
        if (stored == null) {
            return;
        }
        AtlasClicks.Decision decision = AtlasClicks.drag(stored.board(), 1, false, offer(expectedCursor));
        if (decision.message() != null) {
            tell(player, decision.message());
            return;
        }
        if (AtlasClicks.changedCell(stored.board(), decision.board()) >= 0) {
            place(player, atlasId, true, -1, expectedCursor);
        }
    }

    private boolean place(Player player, UUID atlasId, boolean fromCursor, int sourceSlot, ItemStack expected) {
        ItemStack current = fromCursor ? player.getItemOnCursor() : player.getInventory().getItem(sourceSlot);
        if (!same(current, expected) || current == null || current.isEmpty()) {
            return false;
        }
        Located atlas = find(player, atlasId);
        if (atlas == null) {
            close(player, atlasId);
            return false;
        }
        if (fromCursor && atlas.cursor) {
            return false;
        }
        if (!fromCursor && !atlas.cursor && atlas.slot == sourceSlot) {
            return false;
        }
        AtlasItem.Stored stored = read(player, atlas.stack);
        if (stored == null) {
            return false;
        }
        AtlasBoard before = stored.board();
        AtlasClicks.Decision decision = AtlasClicks.place(before, offer(current));
        if (decision.message() != null) {
            tell(player, decision.message());
            return false;
        }
        int index = AtlasClicks.changedCell(before, decision.board());
        if (index < 0) {
            return false;
        }
        ItemStack previousSource = current.clone();
        ItemStack one = current.clone();
        one.setAmount(1);
        ItemStack[] maps = stored.maps();
        maps[index] = one;
        AtlasItem.Stored next = new AtlasItem.Stored(decision.board().world(), decision.board().origin(), maps);
        ItemStack previousAtlas = atlas.stack.clone();
        try {
            setSource(player, fromCursor, sourceSlot, decreased(previousSource));
            put(player, atlas, AtlasItem.write(previousAtlas, next));
            show(player, atlasId, next.maps());
            return true;
        } catch (RuntimeException exception) {
            plugin.getLogger().log(Level.WARNING, "Could not store an Exploration Atlas map", exception);
            setSource(player, fromCursor, sourceSlot, previousSource);
            put(player, atlas, previousAtlas);
            show(player, atlasId, stored.maps());
            tell(player, AtlasMessages.FAILED);
            return false;
        }
    }

    private void take(Player player, Located atlas, AtlasItem.Stored stored, int index, boolean toCursor, int destination) {
        ItemStack map = stored.map(index);
        if (map == null) {
            return;
        }
        if (!toCursor && !atlas.cursor && atlas.slot == destination) {
            return;
        }
        if (toCursor) {
            ItemStack cursor = player.getItemOnCursor();
            if (cursor != null && !cursor.isEmpty()) {
                return;
            }
        }
        ItemStack previousDestination = null;
        if (!toCursor) {
            ItemStack existing = player.getInventory().getItem(destination);
            if (existing != null && !existing.isEmpty()) {
                previousDestination = existing.clone();
            }
        }
        ItemStack previousAtlas = atlas.stack.clone();
        ItemStack[] maps = stored.maps();
        maps[index] = null;
        AtlasBoard cleared = AtlasClicks.take(stored.board(), index).board();
        AtlasItem.Stored next = new AtlasItem.Stored(cleared.world(), cleared.origin(), maps);
        UUID atlasId = AtlasItem.id(previousAtlas);
        try {
            if (toCursor) {
                player.setItemOnCursor(map.clone());
            } else {
                give(player, destination, map);
            }
            put(player, atlas, AtlasItem.write(previousAtlas, next));
            show(player, atlasId, next.maps());
        } catch (RuntimeException exception) {
            plugin.getLogger().log(Level.WARNING, "Could not retrieve an Exploration Atlas map", exception);
            if (toCursor) {
                player.setItemOnCursor(ItemStack.empty());
            } else if (previousDestination == null) {
                player.getInventory().setItem(destination, null);
            } else {
                player.getInventory().setItem(destination, previousDestination);
            }
            put(player, atlas, previousAtlas);
            show(player, atlasId, stored.maps());
            tell(player, AtlasMessages.FAILED);
        }
    }

    private AtlasItem.Stored read(Player player, ItemStack atlas) {
        try {
            return AtlasItem.read(atlas);
        } catch (RuntimeException exception) {
            plugin.getLogger().log(Level.WARNING, "Could not read an Exploration Atlas", exception);
            tell(player, AtlasMessages.FAILED);
            return null;
        }
    }

    private void show(Player player, UUID atlasId, ItemStack[] maps) {
        if (atlasId == null) {
            return;
        }
        Inventory top = player.getOpenInventory().getTopInventory();
        if (top.getHolder() instanceof AtlasHolder holder && atlasId.equals(holder.atlasId())) {
            sendMaps(player, maps);
            AtlasView.show(top, maps);
        }
    }

    private static void sendMaps(Player player, ItemStack[] maps) {
        if (maps == null) {
            return;
        }
        for (ItemStack map : maps) {
            MapView view = storedView(map);
            if (view != null) {
                player.sendMap(view);
            }
        }
    }

    private static MapView storedView(ItemStack map) {
        if (map == null || map.getType() != Material.FILLED_MAP) {
            return null;
        }
        if (!(map.getItemMeta() instanceof MapMeta meta) || !meta.hasMapView()) {
            return null;
        }
        return meta.getMapView();
    }

    private void close(Player player, UUID atlasId) {
        Inventory top = player.getOpenInventory().getTopInventory();
        if (top.getHolder() instanceof AtlasHolder holder && atlasId.equals(holder.atlasId())) {
            player.closeInventory();
        }
    }

    private void tell(Player player, String message) {
        player.sendMessage(NoticeChat.line(plugin.messagePrefix(), message));
    }

    private void later(Player player, Runnable action) {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) {
                action.run();
            }
        });
    }

    private static void give(Player player, int slot, ItemStack map) {
        ItemStack existing = player.getInventory().getItem(slot);
        if (existing == null || existing.isEmpty()) {
            player.getInventory().setItem(slot, map.clone());
            return;
        }
        ItemStack stacked = existing.clone();
        stacked.setAmount(existing.getAmount() + map.getAmount());
        player.getInventory().setItem(slot, stacked);
    }

    private static int room(Player player, ItemStack map) {
        PlayerInventory inventory = player.getInventory();
        for (int slot = 0; slot < 36; slot++) {
            ItemStack existing = inventory.getItem(slot);
            if (existing != null
                    && !existing.isEmpty()
                    && existing.isSimilar(map)
                    && existing.getAmount() + map.getAmount() <= existing.getMaxStackSize()) {
                return slot;
            }
        }
        int empty = inventory.firstEmpty();
        if (empty >= 0 && empty < 36) {
            return empty;
        }
        return -1;
    }

    private static void setSource(Player player, boolean cursor, int slot, ItemStack stack) {
        if (cursor) {
            player.setItemOnCursor(stack == null || stack.isEmpty() ? ItemStack.empty() : stack);
            return;
        }
        player.getInventory().setItem(slot, stack == null || stack.isEmpty() ? null : stack);
    }

    private static ItemStack decreased(ItemStack stack) {
        if (stack.getAmount() <= 1) {
            return ItemStack.empty();
        }
        ItemStack left = stack.clone();
        left.setAmount(stack.getAmount() - 1);
        return left;
    }

    private static void put(Player player, Located located, ItemStack updated) {
        if (located.cursor) {
            player.setItemOnCursor(updated);
            return;
        }
        player.getInventory().setItem(located.slot, updated);
    }

    private static Located find(Player player, UUID atlasId) {
        if (atlasId.equals(AtlasItem.id(player.getItemOnCursor()))) {
            return new Located(player.getItemOnCursor(), true, -1);
        }
        PlayerInventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (atlasId.equals(AtlasItem.id(stack))) {
                return new Located(stack, false, slot);
            }
        }
        return null;
    }

    private static AtlasClicks.Offer offer(ItemStack stack) {
        int amount = stack == null || stack.isEmpty() ? 0 : stack.getAmount();
        if (stack == null || stack.getType() != Material.FILLED_MAP) {
            return new AtlasClicks.Offer(false, null, 0, 0, "map", amount);
        }
        if (!(stack.getItemMeta() instanceof MapMeta meta) || !meta.hasMapView()) {
            return new AtlasClicks.Offer(false, null, 0, 0, "map", amount);
        }
        MapView view = meta.getMapView();
        if (view == null || MapScale.of(view.getScale()) != MapGrid.REQUIRED_SCALE) {
            return new AtlasClicks.Offer(false, null, 0, 0, "map", amount);
        }
        World world = view.getWorld();
        if (world == null) {
            return new AtlasClicks.Offer(true, null, 0, 0, "map", amount);
        }
        MapGrid.Tile tile = MapGrid.tile(MapGrid.REQUIRED_SCALE, view.getCenterX(), view.getCenterZ());
        return new AtlasClicks.Offer(true, world.getUID().toString(), tile.x(), tile.z(), "map", amount);
    }

    private static boolean mapItem(ItemStack stack) {
        return stack != null && (stack.getType() == Material.FILLED_MAP || stack.getType() == Material.MAP);
    }

    private static boolean same(ItemStack current, ItemStack expected) {
        boolean currentEmpty = current == null || current.isEmpty();
        boolean expectedEmpty = expected == null || expected.isEmpty();
        if (currentEmpty || expectedEmpty) {
            return currentEmpty && expectedEmpty;
        }
        return current.getAmount() == expected.getAmount() && current.isSimilar(expected);
    }

    private static ItemStack clone(ItemStack stack) {
        return stack == null ? null : stack.clone();
    }

    private record Located(ItemStack stack, boolean cursor, int slot) {
    }
}
