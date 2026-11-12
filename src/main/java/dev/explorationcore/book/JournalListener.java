package dev.explorationcore.book;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;

public final class JournalListener implements Listener {
    private final Consumer<Player> openJournal;
    private final Consumer<Player> openGuides;

    public JournalListener(Consumer<Player> openJournal, Consumer<Player> openGuides) {
        this.openJournal = openJournal;
        this.openGuides = openGuides;
    }

    @EventHandler
    public void openHeldBook(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (!openHeld(event.getPlayer(), event.getItem(), event.getHand())) {
            return;
        }
        event.setUseItemInHand(Event.Result.DENY);
        event.setUseInteractedBlock(Event.Result.DENY);
    }

    @EventHandler
    public void openHeldBookOnEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        EquipmentSlot hand = event.getHand();
        ItemStack item = hand == EquipmentSlot.OFF_HAND
                ? player.getInventory().getItemInOffHand()
                : player.getInventory().getItemInMainHand();
        if (!openHeld(player, item, hand)) {
            return;
        }
        event.setCancelled(true);
    }

    private boolean openHeld(Player player, ItemStack item, EquipmentSlot hand) {
        boolean journal = ExplorationJournal.isJournal(item);
        boolean guides = GuidesItem.isGuide(item);
        if (!journal && !guides) {
            return false;
        }
        player.updateInventory();
        if (hand == EquipmentSlot.OFF_HAND && mainHandMatches(player, journal)) {
            return true;
        }
        if (journal) {
            openJournal.accept(player);
        } else {
            openGuides.accept(player);
        }
        return true;
    }

    private static boolean mainHandMatches(Player player, boolean journal) {
        ItemStack main = player.getInventory().getItemInMainHand();
        return journal ? ExplorationJournal.isJournal(main) : GuidesItem.isGuide(main);
    }
}
