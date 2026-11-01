package dev.explorationcore.command;

import dev.explorationcore.ExplorationCorePlugin;
import dev.explorationcore.book.BookPages;
import dev.explorationcore.book.ExplorationBook;
import dev.explorationcore.book.ExplorationTotals;
import dev.explorationcore.book.JournalFixtures;
import dev.explorationcore.book.PreparedBook;
import dev.explorationcore.book.TotalsException;
import dev.explorationcore.book.TotalsFile;
import dev.explorationcore.db.DiscoveryEntry;
import dev.explorationcore.db.DiscoveryRepository;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;

public final class ExplorationBookCommand implements CommandExecutor, TabCompleter {
    private static final String PERMISSION = "explorationcore.exploration";
    private static final String PREVIEW_PERMISSION = "explorationcore.bookpreview";
    private static final String UNAVAILABLE = "Exploration book is unavailable.";

    private final ExplorationCorePlugin plugin;

    public ExplorationBookCommand(ExplorationCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
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
        try {
            open(player);
        } catch (Exception exception) {
            plugin.getLogger().log(Level.SEVERE, "Exploration book failed for " + player.getName(), exception);
            player.sendMessage(Component.text(UNAVAILABLE));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission(PREVIEW_PERMISSION)) {
            return List.of();
        }
        if (args.length == 1) {
            return startingWith(List.of("preview"), args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("preview")) {
            return startingWith(JournalFixtures.SCENARIOS, args[1]);
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
