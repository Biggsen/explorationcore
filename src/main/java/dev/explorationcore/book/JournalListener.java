package dev.explorationcore.book;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;

public final class JournalListener implements Listener {
    private final Consumer<Player> open;

    public JournalListener(Consumer<Player> open) {
        this.open = open;
    }

    @EventHandler
    public void openJournal(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (!ExplorationJournal.isJournal(event.getItem())) {
            return;
        }
        event.setUseItemInHand(Event.Result.DENY);
        Player player = event.getPlayer();
        player.updateInventory();
        if (action != Action.RIGHT_CLICK_AIR) {
            return;
        }
        if (event.getHand() == EquipmentSlot.OFF_HAND && mainHandIsJournal(player)) {
            return;
        }
        open.accept(player);
    }

    private static boolean mainHandIsJournal(Player player) {
        ItemStack main = player.getInventory().getItemInMainHand();
        return ExplorationJournal.isJournal(main);
    }
}
