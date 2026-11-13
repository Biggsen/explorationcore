package dev.explorationcore.command;

import dev.explorationcore.ExplorationCorePlugin;
import dev.explorationcore.book.BookPages;
import dev.explorationcore.book.ExplorationBook;
import dev.explorationcore.book.ExplorationJournal;
import dev.explorationcore.book.ExplorationTotals;
import dev.explorationcore.book.GuideBook;
import dev.explorationcore.book.GuideChapter;
import dev.explorationcore.book.GuidesException;
import dev.explorationcore.book.GuidesFile;
import dev.explorationcore.book.GuidesItem;
import dev.explorationcore.book.JournalFixtures;
import dev.explorationcore.book.PreparedBook;
import dev.explorationcore.book.TotalsException;
import dev.explorationcore.book.TotalsFile;
import dev.explorationcore.db.DiscoveryEntry;
import dev.explorationcore.db.DiscoveryRepository;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;
import java.util.logging.Level;

public final class ExplorationBookCommand implements CommandExecutor, TabCompleter {
    private static final String PERMISSION = "explorationcore.exploration";
    private static final String PREVIEW_PERMISSION = "explorationcore.bookpreview";
    private static final String UNAVAILABLE = "Exploration book is unavailable.";
    private static final String GUIDES_UNAVAILABLE = "Guides are unavailable.";
    private static final String GUIDE_SELF_ONLY = "This command gives you a guide book.";
    private static final String GUIDE_NAME_THE_PLAYER = "Name the player who should receive the guide book.";

    private final ExplorationCorePlugin plugin;

    public ExplorationBookCommand(ExplorationCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("guides")) {
            guides(sender, args);
            return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("book")) {
            give(sender, Arrays.copyOfRange(args, 1, args.length));
            return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("guides")) {
            guides(sender, Arrays.copyOfRange(args, 1, args.length));
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only a player can open the exploration book."));
            return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("preview")) {
            preview(player, args);
            return true;
        }
        if (!player.hasPermission(PERMISSION)) {
            player.sendMessage(Component.text("You do not have permission to use this command."));
            return true;
        }
        openJournal(player);
        return true;
    }

    public void openJournal(Player player) {
        try {
            open(player);
        } catch (Exception exception) {
            plugin.getLogger().log(Level.SEVERE, "Exploration book failed for " + player.getName(), exception);
            player.sendMessage(Component.text(UNAVAILABLE));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("guides")) {
            return completeGuides(sender, args);
        }
        if (args.length == 1) {
            List<String> options = new ArrayList<>();
            if (canReceiveJournal(sender)) {
                options.add("book");
                options.add("guides");
            }
            if (sender.hasPermission(PREVIEW_PERMISSION)) {
                options.add("preview");
            }
            return startingWith(options, args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("book") && !(sender instanceof Player)) {
            return startingWith(onlineNames(), args[1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("guides") && canReceiveJournal(sender)) {
            return startingWith(List.of("book"), args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("guides") && args[1].equalsIgnoreCase("book")
                && !(sender instanceof Player)) {
            return startingWith(onlineNames(), args[2]);
        }
        if (sender.hasPermission(PREVIEW_PERMISSION) && args.length == 2 && args[0].equalsIgnoreCase("preview")) {
            return startingWith(JournalFixtures.SCENARIOS, args[1]);
        }
        return List.of();
    }

    private List<String> completeGuides(CommandSender sender, String[] args) {
        if (args.length == 1 && canReceiveJournal(sender)) {
            return startingWith(List.of("book"), args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("book") && !(sender instanceof Player)) {
            return startingWith(onlineNames(), args[1]);
        }
        return List.of();
    }

    private void preview(Player player, String[] args) {
        if (!player.hasPermission(PREVIEW_PERMISSION)) {
            player.sendMessage(Component.text("You do not have permission to use this command."));
            return;
        }
        if (args.length != 2 || JournalFixtures.scenario(args[1]) == null) {
            player.sendMessage(Component.text("Unknown preview. Use empty, partial, or long."));
            return;
        }
        try {
            show(player, JournalFixtures.book(plugin.serverName(), args[1]));
        } catch (Exception exception) {
            plugin.getLogger().log(Level.SEVERE, "Exploration book preview failed for " + player.getName(), exception);
            player.sendMessage(Component.text(UNAVAILABLE));
        }
    }

    private void open(Player player) throws Exception {
        Path totalsFile = plugin.getDataFolder().toPath().resolve("totals.yml");
        ExplorationTotals totals;
        try {
            totals = TotalsFile.load(totalsFile);
        } catch (TotalsException exception) {
            plugin.getLogger().warning("Exploration book failed for " + player.getName() + ": " + exception.getMessage());
            player.sendMessage(Component.text(UNAVAILABLE));
            return;
        }
        DiscoveryRepository repository = plugin.repository();
        if (repository == null) {
            plugin.getLogger().severe("Exploration book failed for " + player.getName() + ": SQLite database is unavailable");
            player.sendMessage(Component.text(UNAVAILABLE));
            return;
        }
        List<DiscoveryEntry> discoveries = repository.discoveriesFor(player.getUniqueId().toString());
        show(player, ExplorationBook.prepare(plugin.serverName(), totals, discoveries));
    }

    private void show(Player player, PreparedBook book) {
        for (String notice : book.notices()) {
            plugin.getLogger().warning("Exploration book for " + player.getName() + ": " + notice);
        }
        player.openBook(Book.book(
                Component.text(ExplorationBook.TITLE),
                Component.text(plugin.serverName()),
                BookPages.components(book)
        ));
    }

    private void guides(CommandSender sender, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("book")) {
            giveItem(
                    sender,
                    Arrays.copyOfRange(args, 1, args.length),
                    GuidesItem::create,
                    GUIDE_SELF_ONLY,
                    GUIDE_NAME_THE_PLAYER
            );
            return;
        }
        if (args.length > 0) {
            sender.sendMessage(Component.text("Use /guides book."));
            return;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only a player can open the guides."));
            return;
        }
        if (!player.hasPermission(PERMISSION)) {
            player.sendMessage(Component.text("You do not have permission to use this command."));
            return;
        }
        openGuides(player);
    }

    public void openGuides(Player player) {
        try {
            Path file = plugin.getDataFolder().toPath().resolve("guides.yml");
            List<GuideChapter> chapters = GuidesFile.load(file);
            showGuides(player, GuideBook.prepare(plugin.serverName(), chapters));
        } catch (GuidesException exception) {
            plugin.getLogger().warning("Guides failed for " + player.getName() + ": " + exception.getMessage());
            player.sendMessage(Component.text(GUIDES_UNAVAILABLE));
        } catch (Exception exception) {
            plugin.getLogger().log(Level.SEVERE, "Guides failed for " + player.getName(), exception);
            player.sendMessage(Component.text(GUIDES_UNAVAILABLE));
        }
    }

    private void showGuides(Player player, PreparedBook book) {
        player.openBook(Book.book(
                Component.text(GuideBook.TITLE),
                Component.text(plugin.serverName()),
                BookPages.components(book)
        ));
    }

    private void give(CommandSender sender, String[] args) {
        giveItem(sender, args, ExplorationJournal::create, JournalGift.SELF_ONLY, JournalGift.NAME_THE_PLAYER);
    }

    private void giveItem(
            CommandSender sender,
            String[] args,
            Supplier<ItemStack> item,
            String selfOnly,
            String nameThePlayer
    ) {
        if (sender instanceof Player player && !player.hasPermission(PERMISSION)) {
            player.sendMessage(Component.text("You do not have permission to use this command."));
            return;
        }
        JournalGift.Decision decision = JournalGift.decide(sender instanceof Player, args, selfOnly, nameThePlayer);
        switch (decision) {
            case JournalGift.Self ignored -> giveTo((Player) sender, item.get());
            case JournalGift.Named named -> giveNamed(sender, named.name(), item);
            case JournalGift.Rejected rejected -> sender.sendMessage(Component.text(rejected.message()));
        }
    }

    private void giveNamed(CommandSender sender, String name, Supplier<ItemStack> item) {
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
        giveTo(target, item.get());
    }

    private static void giveTo(Player player, ItemStack stack) {
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(stack);
        for (ItemStack stackLeft : leftover.values()) {
            if (stackLeft != null && stackLeft.getAmount() > 0) {
                player.getWorld().dropItemNaturally(player.getLocation(), stackLeft);
            }
        }
    }

    private static boolean canReceiveJournal(CommandSender sender) {
        return !(sender instanceof Player) || sender.hasPermission(PERMISSION);
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
