package dev.explorationcore.command;

import dev.explorationcore.ExplorationCorePlugin;
import dev.explorationcore.record.AttemptResult;
import dev.explorationcore.record.OnlinePlayer;
import dev.explorationcore.record.RecordAttempt;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.RemoteConsoleCommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.logging.Level;

public final class ExplorationCommand implements CommandExecutor, TabCompleter {
    private static final String RECORD_PERMISSION = "explorationcore.record";
    private static final String STATUS_PERMISSION = "explorationcore.status";

    private final ExplorationCorePlugin plugin;

    public ExplorationCommand(ExplorationCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        try {
            if (args.length == 0 || args[0].equalsIgnoreCase("status")) {
                status(sender);
            } else if (args[0].equalsIgnoreCase("record")) {
                record(sender, Arrays.copyOfRange(args, 1, args.length));
            } else if (!sender.hasPermission(STATUS_PERMISSION) && !sender.hasPermission(RECORD_PERMISSION)) {
                sender.sendMessage(Component.text("You do not have permission to use this command."));
            } else {
                sender.sendMessage(Component.text("Unknown subcommand. Use status or record."));
            }
        } catch (Exception exception) {
            plugin.getLogger().log(Level.SEVERE, "ExplorationCore command failed", exception);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) {
            return List.of();
        }
        String prefix = args[0].toLowerCase(Locale.ROOT);
        List<String> options = new ArrayList<>();
        if (sender.hasPermission(STATUS_PERMISSION) && "status".startsWith(prefix)) {
            options.add("status");
        }
        if (canRecord(sender) && "record".startsWith(prefix)) {
            options.add("record");
        }
        return options;
    }

    private void status(CommandSender sender) {
        if (!sender.hasPermission(STATUS_PERMISSION)) {
            sender.sendMessage(Component.text("You do not have permission to use this command."));
            return;
        }
        String server = plugin.serverName().isBlank() ? "(not set)" : plugin.serverName();
        if (plugin.repository() == null) {
            sender.sendMessage(statusText(server, "unavailable", "unavailable"));
            return;
        }
        try {
            long discoveries = plugin.repository().count();
            sender.sendMessage(statusText(server, "connected", Long.toString(discoveries)));
        } catch (Exception exception) {
            plugin.getLogger().log(Level.SEVERE, "Failed to read discovery count", exception);
            sender.sendMessage(statusText(server, "unavailable", "unavailable"));
        }
    }

    private void record(CommandSender sender, String[] args) {
        if (!canRecord(sender)) {
            sender.sendMessage(Component.text("You do not have permission to record discoveries."));
            plugin.getLogger().warning(
                    "Rejected record command from " + sender.getName() + ": missing " + RECORD_PERMISSION
            );
            return;
        }
        AttemptResult result = RecordAttempt.attempt(
                plugin.serverName(),
                args,
                ExplorationCommand::findOnline,
                plugin.repository(),
                Instant.now()
        );
        switch (result) {
            case AttemptResult.Rejected rejected -> {
                plugin.getLogger().warning("Rejected record: " + rejected.reason());
                sender.sendMessage(Component.text(rejected.reason()));
            }
            case AttemptResult.Recorded recorded -> {
                if ("structure".equals(recorded.entityType()) && recorded.structureType() == null) {
                    plugin.getLogger().warning(
                            "Structure recorded without structure_type: player=" + recorded.playerName()
                                    + " world=" + recorded.world()
                                    + " entity_id=" + recorded.entityId()
                    );
                }
                if (plugin.debug()) {
                    plugin.getLogger().info(recorded.logLine());
                }
            }
            case AttemptResult.Duplicate duplicate -> {
                if (plugin.debug()) {
                    plugin.getLogger().info(duplicate.logLine());
                }
            }
            case AttemptResult.Failed failed -> {
                if (failed.cause() == null) {
                    plugin.getLogger().severe(failed.logLine() + " SQLite database is unavailable");
                } else {
                    plugin.getLogger().log(Level.SEVERE, failed.logLine(), failed.cause());
                }
                sender.sendMessage(Component.text("Persistence failed. See the server log."));
            }
        }
    }

    private static boolean canRecord(CommandSender sender) {
        return sender instanceof ConsoleCommandSender
                || sender instanceof RemoteConsoleCommandSender
                || sender.hasPermission(RECORD_PERMISSION);
    }

    private static Optional<OnlinePlayer> findOnline(String name) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getName().equalsIgnoreCase(name)) {
                return Optional.of(new OnlinePlayer(player.getUniqueId(), player.getName()));
            }
        }
        return Optional.empty();
    }

    private static Component statusText(String server, String database, String discoveries) {
        return Component.text(
                "ExplorationCore\n"
                        + "Server: " + server + "\n"
                        + "Database: " + database + "\n"
                        + "Discoveries: " + discoveries
        );
    }
}
