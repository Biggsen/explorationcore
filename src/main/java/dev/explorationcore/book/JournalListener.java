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
        ItemStack item = event.getItem();
        boolean journal = ExplorationJournal.isJournal(item);
        boolean guides = GuidesItem.isGuide(item);
        if (!journal && !guides) {
            return;
        }
        event.setUseItemInHand(Event.Result.DENY);
        Player player = event.getPlayer();
        player.updateInventory();
        if (action != Action.RIGHT_CLICK_AIR) {
            return;
        }
        if (event.getHand() == EquipmentSlot.OFF_HAND && mainHandMatches(player, journal)) {
            return;
        }
        if (journal) {
            openJournal.accept(player);
        } else {
            openGuides.accept(player);
        }
    }

    private static boolean mainHandMatches(Player player, boolean journal) {
        ItemStack main = player.getInventory().getItemInMainHand();
        return journal ? ExplorationJournal.isJournal(main) : GuidesItem.isGuide(main);
    }
}
