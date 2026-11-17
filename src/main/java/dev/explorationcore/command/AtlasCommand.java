package dev.explorationcore.command;

import dev.explorationcore.atlas.AtlasItem;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class AtlasCommand implements CommandExecutor, TabCompleter {
    private static final String PERMISSION = "explorationcore.exploration";
    private static final String SELF_ONLY = "This command gives you an atlas.";
    private static final String NAME_THE_PLAYER = "Name the player who should receive the atlas.";

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player player && !player.hasPermission(PERMISSION)) {
            player.sendMessage(Component.text("You do not have permission to use this command."));
            return true;
        }
        JournalGift.Decision decision = JournalGift.decide(sender instanceof Player, args, SELF_ONLY, NAME_THE_PLAYER);
        switch (decision) {
            case JournalGift.Self ignored -> giveTo((Player) sender, AtlasItem.create());
            case JournalGift.Named named -> giveNamed(sender, named.name());
            case JournalGift.Rejected rejected -> sender.sendMessage(Component.text(rejected.message()));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (sender instanceof Player player && !player.hasPermission(PERMISSION)) {
            return List.of();
        }
        if (args.length == 1 && !(sender instanceof Player)) {
            return startingWith(onlineNames(), args[0]);
        }
        return List.of();
    }

    private static void giveNamed(CommandSender sender, String name) {
        Player target = null;
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getName().equalsIgnoreCase(name)) {
                target = online;
                break;
            }
        }
        if (target == null) {
            sender.sendMessage(Component.text("player is not online: " + name));
            return;
        }
        giveTo(target, AtlasItem.create());
    }

    private static void giveTo(Player player, ItemStack stack) {
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(stack);
        for (ItemStack stackLeft : leftover.values()) {
            if (stackLeft != null && stackLeft.getAmount() > 0) {
                player.getWorld().dropItemNaturally(player.getLocation(), stackLeft);
            }
        }
    }

    private static List<String> onlineNames() {
        List<String> names = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            names.add(player.getName());
        }
        return names;
    }

    private static List<String> startingWith(List<String> options, String typed) {
        String prefix = typed.toLowerCase(Locale.ROOT);
        List<String> matches = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                matches.add(option);
            }
        }
        return matches;
    }
}
