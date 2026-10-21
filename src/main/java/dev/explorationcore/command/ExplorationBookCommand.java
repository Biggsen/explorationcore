package dev.explorationcore.command;

import dev.explorationcore.ExplorationCorePlugin;
import dev.explorationcore.book.BookPages;
import dev.explorationcore.book.ExplorationBook;
import dev.explorationcore.book.ExplorationTotals;
import dev.explorationcore.book.PreparedBook;
import dev.explorationcore.book.TotalsException;
import dev.explorationcore.book.TotalsFile;
import dev.explorationcore.db.DiscoveryRepository;
import dev.explorationcore.db.PlayerTally;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;

public final class ExplorationBookCommand implements CommandExecutor {
    private static final String PERMISSION = "explorationcore.exploration";
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
        List<PlayerTally> tallies = repository.talliesFor(player.getUniqueId().toString());
        PreparedBook book = ExplorationBook.prepare(plugin.serverName(), totals, tallies);
        for (String notice : book.notices()) {
            plugin.getLogger().warning("Exploration book for " + player.getName() + ": " + notice);
        }
        player.openBook(Book.book(
                Component.text(ExplorationBook.TITLE),
                Component.text(plugin.serverName()),
                BookPages.components(book)
        ));
    }
}
