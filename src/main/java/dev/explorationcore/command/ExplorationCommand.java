package dev.explorationcore.command;

import dev.explorationcore.ExplorationCorePlugin;
import dev.explorationcore.db.DiscoveryRepository;
import dev.explorationcore.db.PlayerTally;
import dev.explorationcore.feedback.DiscoveryNotice;
import dev.explorationcore.feedback.DiscoveryNotices;
import dev.explorationcore.feedback.DiscoveryPresenter;
import dev.explorationcore.feedback.NoticeChat;
import dev.explorationcore.milestone.Milestone;
import dev.explorationcore.milestone.MilestoneEvaluator;
import dev.explorationcore.record.AttemptResult;
import dev.explorationcore.record.OnlinePlayer;
import dev.explorationcore.record.RecordAttempt;
import dev.explorationcore.record.Timestamps;
import dev.explorationcore.reward.CrateGrant;
import dev.explorationcore.reward.DiscoveryCrates;
import dev.explorationcore.reward.RegionBooks;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.RemoteConsoleCommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
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
                showDiscovery(recorded, DiscoveryNotices.notice(recorded).orElse(null));
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

    private void showDiscovery(AttemptResult.Recorded recorded, DiscoveryNotice notice) {
        if (Bukkit.isPrimaryThread()) {
            deliverDiscovery(recorded, notice);
            return;
        }
        Bukkit.getScheduler().runTask(plugin, () -> deliverDiscovery(recorded, notice));
    }

    private void deliverDiscovery(AttemptResult.Recorded recorded, DiscoveryNotice notice) {
        grantRegionBook(recorded);
        Player player = Bukkit.getPlayer(recorded.playerUuid());
        if (player == null) {
            plugin.getLogger().warning(
                    "Discovery recorded but player left before feedback: " + discoveryContext(recorded)
            );
        } else {
            if (notice != null) {
                try {
                    DiscoveryPresenter.show(player, notice, plugin.messagePrefix());
                } catch (RuntimeException exception) {
                    plugin.getLogger().log(
                            Level.SEVERE,
                            "Discovery presentation failed: " + discoveryContext(recorded),
                            exception
                    );
                }
            }
            if ("structure".equals(recorded.entityType()) && recorded.structureType() != null) {
                grantExperience(player, recorded);
                grantClaimblocks(player, recorded);
            }
            if (recorded.skipCrate()) {
                if (plugin.debug()) {
                    plugin.getLogger().info("Crate grant skipped: " + discoveryContext(recorded));
                }
            } else {
                grantDiscoveryCrate(player, recorded);
            }
        }
        grantMilestones(recorded, player);
    }

    private void grantMilestones(AttemptResult.Recorded recorded, Player player) {
        if (plugin.milestones().isEmpty()) {
            return;
        }
        DiscoveryRepository repository = plugin.repository();
        if (repository == null) {
            plugin.getLogger().severe(
                    "Milestone check failed because the database is closed: " + discoveryContext(recorded)
            );
            return;
        }
        List<PlayerTally> tallies;
        try {
            tallies = repository.talliesFor(recorded.playerUuid().toString());
        } catch (SQLException exception) {
            plugin.getLogger().log(
                    Level.SEVERE,
                    "Milestone check failed: " + discoveryContext(recorded),
                    exception
            );
            return;
        }
        List<Milestone> crossed = MilestoneEvaluator.crossed(
                plugin.milestones(),
                plugin.counters(),
                recorded.world(),
                recorded.entityType(),
                recorded.structureType(),
                tallies
        );
        int shown = 0;
        for (int index = 0; index < crossed.size(); index++) {
            if (player == null || !player.isOnline()) {
                for (int rest = index; rest < crossed.size(); rest++) {
                    plugin.getLogger().severe(
                            "Milestone not granted because the player was offline and needs manual recovery: player="
                                    + recorded.playerUuid()
                                    + " name=" + recorded.playerName()
                                    + " " + crossed.get(rest).recoveryText()
                    );
                }
                return;
            }
            Milestone milestone = crossed.get(index);
            boolean claimed;
            try {
                claimed = repository.claimMilestone(
                        recorded.playerUuid().toString(),
                        milestone.id(),
                        Timestamps.format(Instant.now())
                );
            } catch (SQLException exception) {
                plugin.getLogger().log(
                        Level.SEVERE,
                        "Milestone grant failed and needs manual recovery: player="
                                + recorded.playerUuid()
                                + " name=" + player.getName()
                                + " " + milestone.recoveryText(),
                        exception
                );
                for (int rest = index + 1; rest < crossed.size(); rest++) {
                    plugin.getLogger().severe(
                            "Milestone not granted because an earlier grant failed and needs manual recovery: player="
                                    + recorded.playerUuid()
                                    + " name=" + player.getName()
                                    + " " + crossed.get(rest).recoveryText()
                    );
                }
                return;
            }
            if (!claimed) {
                if (plugin.debug()) {
                    plugin.getLogger().info(
                            "Milestone already granted: player=" + recorded.playerUuid()
                                    + " milestone=" + milestone.id()
                    );
                }
                continue;
            }
            List<String> confirmations = deliverMilestone(player, recorded, milestone);
            shown++;
            scheduleMilestone(
                    recorded.playerUuid(),
                    player.getName(),
                    milestone,
                    confirmations,
                    DiscoveryPresenter.durationTicks() * shown
            );
        }
    }

    private List<String> deliverMilestone(
            Player player,
            AttemptResult.Recorded recorded,
            Milestone milestone
    ) {
        List<String> confirmations = new ArrayList<>();
        if (milestone.xp() > 0) {
            String reward = "xp amount=" + milestone.xp();
            if (!player.isOnline()) {
                logRewardFailed(recorded, player.getName(), milestone, reward + " reason=player offline", null);
            } else {
                try {
                    player.giveExp(milestone.xp(), false);
                    confirmations.add(DiscoveryNotices.experience(milestone.xp()));
                } catch (RuntimeException exception) {
                    logRewardFailed(recorded, player.getName(), milestone, reward, exception);
                }
            }
        }
        for (Milestone.ItemReward item : milestone.items()) {
            String reward = "item " + item.name() + item.detail() + " amount=" + item.amount();
            if (!player.isOnline()) {
                logRewardFailed(recorded, player.getName(), milestone, reward + " reason=player offline", null);
            } else {
                try {
                    giveItem(player, item);
                    confirmations.add(DiscoveryNotices.item(item.amount(), item.label()));
                } catch (RuntimeException exception) {
                    logRewardFailed(recorded, player.getName(), milestone, reward, exception);
                }
            }
        }
        if (milestone.claimblocks() > 0) {
            deliverClaimblocks(player, recorded, milestone, confirmations);
        }
        if (milestone.promotionCommand() != null) {
            deliverPromotion(player, recorded, milestone);
        }
        return confirmations;
    }

    private void deliverClaimblocks(
            Player player,
            AttemptResult.Recorded recorded,
            Milestone milestone,
            List<String> confirmations
    ) {
        String reward = "claimblocks amount=" + milestone.claimblocks();
        if (!player.isOnline()) {
            logRewardFailed(recorded, recorded.playerName(), milestone, reward + " reason=player offline", null);
            return;
        }
        if (!singleToken(player.getName())) {
            logRewardFailed(recorded, player.getName(), milestone, reward + " command unavailable", null);
            return;
        }
        Optional<String> command = plugin.structureRewards().formatClaimblocks(
                player.getName(),
                milestone.claimblocks()
        );
        if (command.isEmpty()) {
            logRewardFailed(recorded, player.getName(), milestone, reward + " command unavailable", null);
            return;
        }
        if (dispatch(player, recorded, milestone, command.get(), reward)) {
            confirmations.add(DiscoveryNotices.claimblocks(milestone.claimblocks()));
        }
    }

    private void deliverPromotion(Player player, AttemptResult.Recorded recorded, Milestone milestone) {
        if (!player.isOnline()) {
            logRewardFailed(
                    recorded,
                    recorded.playerName(),
                    milestone,
                    "promotion command=\"" + milestone.promotionCommand() + "\" reason=player offline",
                    null
            );
            return;
        }
        if (!singleToken(player.getName())) {
            logRewardFailed(
                    recorded,
                    player.getName(),
                    milestone,
                    "promotion command=\"" + milestone.promotionCommand() + "\" command unavailable",
                    null
            );
            return;
        }
        String command = milestone.promotionCommand().replace("{player}", player.getName());
        dispatch(player, recorded, milestone, command, "promotion");
    }

    private boolean dispatch(
            Player player,
            AttemptResult.Recorded recorded,
            Milestone milestone,
            String command,
            String reward
    ) {
        if (!player.isOnline()) {
            logRewardFailed(recorded, player.getName(), milestone, reward + " reason=player offline", null);
            return false;
        }
        boolean accepted;
        try {
            accepted = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        } catch (RuntimeException exception) {
            logRewardFailed(recorded, player.getName(), milestone, reward + " command=\"" + command + "\"", exception);
            return false;
        }
        if (!accepted) {
            logRewardFailed(
                    recorded,
                    player.getName(),
                    milestone,
                    reward + " command=\"" + command + "\"",
                    null
            );
            return false;
        }
        return true;
    }

    private void giveItem(Player player, Milestone.ItemReward item) {
        List<ItemStack> stacks = item.stacks();
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(stacks.toArray(ItemStack[]::new));
        for (ItemStack stack : leftover.values()) {
            if (stack != null && stack.getAmount() > 0) {
                player.getWorld().dropItemNaturally(player.getLocation(), stack);
            }
        }
    }

    private void scheduleMilestone(
            UUID playerUuid,
            String playerName,
            Milestone milestone,
            List<String> confirmations,
            long delayTicks
    ) {
        DiscoveryNotice notice = DiscoveryNotices.milestone(milestone.displayName(), milestone.message());
        List<String> lines = List.copyOf(confirmations);
        String broadcast = milestone.broadcast();
        String milestoneId = milestone.id();
        Bukkit.getScheduler().runTaskLater(
                plugin,
                () -> showMilestone(playerUuid, playerName, milestoneId, notice, lines, broadcast),
                delayTicks
        );
    }

    private void showMilestone(
            UUID playerUuid,
            String playerName,
            String milestoneId,
            DiscoveryNotice notice,
            List<String> confirmations,
            String broadcast
    ) {
        Player player = Bukkit.getPlayer(playerUuid);
        if (player != null && player.isOnline()) {
            try {
                DiscoveryPresenter.show(player, notice, plugin.messagePrefix());
            } catch (RuntimeException exception) {
                plugin.getLogger().log(
                        Level.SEVERE,
                        "Milestone notice failed: player=" + playerUuid + " milestone=" + milestoneId,
                        exception
                );
            }
            for (String line : confirmations) {
                try {
                    player.sendMessage(NoticeChat.line(plugin.messagePrefix(), line));
                } catch (RuntimeException exception) {
                    plugin.getLogger().log(
                            Level.SEVERE,
                            "Milestone confirmation failed: player=" + playerUuid
                                    + " milestone=" + milestoneId
                                    + " message=" + line,
                            exception
                    );
                }
            }
        } else {
            plugin.getLogger().warning(
                    "Milestone notice skipped because the player left: player=" + playerUuid
                            + " milestone=" + milestoneId
            );
        }
        if (broadcast == null) {
            return;
        }
        Component announcement = LegacyComponentSerializer.legacySection().deserialize(
                broadcast.replace("{player}", playerName)
        );
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            try {
                viewer.sendMessage(announcement);
            } catch (RuntimeException exception) {
                plugin.getLogger().log(
                        Level.SEVERE,
                        "Milestone broadcast failed: milestone=" + milestoneId + " player=" + viewer.getName(),
                        exception
                );
            }
        }
    }

    private void logRewardFailed(
            AttemptResult.Recorded recorded,
            String name,
            Milestone milestone,
            String reward,
            Exception exception
    ) {
        String message = "Milestone reward failed and needs manual recovery: player="
                + recorded.playerUuid()
                + " name=" + name
                + " milestone=" + milestone.id()
                + " reward=" + reward;
        if (exception == null) {
            plugin.getLogger().severe(message);
        } else {
            plugin.getLogger().log(Level.SEVERE, message, exception);
        }
    }

    private static boolean singleToken(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        for (int index = 0; index < value.length(); index++) {
            if (Character.isWhitespace(value.charAt(index))) {
                return false;
            }
        }
        return true;
    }

    private void grantRegionBook(AttemptResult.Recorded recorded) {
        RegionBooks books = plugin.regionBooks();
        Optional<String> command = books.command(
                recorded.world(),
                recorded.entityType(),
                recorded.entityId(),
                recorded.playerName()
        );
        if (command.isEmpty()) {
            return;
        }
        String line = command.get();
        boolean accepted;
        try {
            accepted = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), line);
        } catch (RuntimeException exception) {
            plugin.getLogger().log(
                    Level.SEVERE,
                    "Book permission failed: " + discoveryContext(recorded)
                            + " name=" + recorded.playerName()
                            + " command=\"" + line + "\"",
                    exception
            );
            return;
        }
        if (!accepted) {
            plugin.getLogger().severe(
                    "Book permission failed: " + discoveryContext(recorded)
                            + " name=" + recorded.playerName()
                            + " command=\"" + line + "\""
            );
        }
    }

    private void grantDiscoveryCrate(Player player, AttemptResult.Recorded recorded) {
        DiscoveryCrates crates = plugin.discoveryCrates();
        Optional<String> crate = crates.crate(recorded.world(), recorded.entityType());
        if (crate.isEmpty()) {
            return;
        }
        String crateName = crate.get();
        Optional<String> command = crates.command(recorded.world(), recorded.entityType(), player.getName());
        if (command.isEmpty()) {
            plugin.getLogger().severe(
                    "Crate grant failed: " + discoveryContext(recorded)
                            + " crate=" + crateName
                            + " command unavailable"
            );
            return;
        }
        boolean dispatched;
        try {
            dispatched = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.get());
        } catch (RuntimeException exception) {
            plugin.getLogger().log(
                    Level.SEVERE,
                    "Crate grant failed: " + discoveryContext(recorded) + " crate=" + crateName,
                    exception
            );
            return;
        }
        CrateGrant grant = CrateGrant.fromDispatch(dispatched);
        if (!grant.accepted()) {
            plugin.getLogger().severe(
                    "Crate grant failed: " + discoveryContext(recorded) + " crate=" + crateName
            );
            return;
        }
        grant.playerMessage().ifPresent(message -> {
            try {
                player.sendMessage(NoticeChat.line(plugin.messagePrefix(), message));
            } catch (RuntimeException exception) {
                plugin.getLogger().log(
                        Level.SEVERE,
                        "Crate message failed: " + discoveryContext(recorded) + " crate=" + crateName,
                        exception
                );
            }
        });
    }

    private void grantExperience(Player player, AttemptResult.Recorded recorded) {
        OptionalInt xp = plugin.structureRewards().xp(recorded.structureType());
        if (xp.isEmpty()) {
            return;
        }
        int amount = xp.getAsInt();
        if (!player.isOnline()) {
            plugin.getLogger().severe(
                    "XP grant failed: " + discoveryContext(recorded) + " xp=" + amount + " player left"
            );
            return;
        }
        try {
            player.giveExp(amount, false);
        } catch (RuntimeException exception) {
            plugin.getLogger().log(
                    Level.SEVERE,
                    "XP grant failed: " + discoveryContext(recorded) + " xp=" + amount,
                    exception
            );
            return;
        }
        try {
            player.sendMessage(NoticeChat.line(plugin.messagePrefix(), DiscoveryNotices.experience(amount)));
        } catch (RuntimeException exception) {
            plugin.getLogger().log(
                    Level.SEVERE,
                    "XP message failed: " + discoveryContext(recorded) + " xp=" + amount,
                    exception
            );
        }
    }

    private void grantClaimblocks(Player player, AttemptResult.Recorded recorded) {
        OptionalInt claimblocks = plugin.structureRewards().claimblocks(recorded.structureType());
        if (claimblocks.isEmpty()) {
            return;
        }
        int amount = claimblocks.getAsInt();
        Optional<String> command = plugin.structureRewards().claimblocksCommand(
                recorded.structureType(),
                player.getName()
        );
        if (command.isEmpty()) {
            plugin.getLogger().severe(
                    "Claimblock grant failed: " + discoveryContext(recorded)
                            + " claimblocks=" + amount
                            + " command unavailable"
            );
            return;
        }
        boolean accepted;
        try {
            accepted = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.get());
        } catch (RuntimeException exception) {
            plugin.getLogger().log(
                    Level.SEVERE,
                    "Claimblock grant failed: " + discoveryContext(recorded) + " claimblocks=" + amount,
                    exception
            );
            return;
        }
        if (!accepted) {
            plugin.getLogger().severe(
                    "Claimblock grant failed: " + discoveryContext(recorded) + " claimblocks=" + amount
            );
            return;
        }
        try {
            player.sendMessage(NoticeChat.line(plugin.messagePrefix(), DiscoveryNotices.claimblocks(amount)));
        } catch (RuntimeException exception) {
            plugin.getLogger().log(
                    Level.SEVERE,
                    "Claimblock message failed: " + discoveryContext(recorded) + " claimblocks=" + amount,
                    exception
            );
        }
    }

    private static String discoveryContext(AttemptResult.Recorded recorded) {
        return "player=" + recorded.playerUuid()
                + " world=" + recorded.world()
                + " entity_type=" + recorded.entityType()
                + " structure_type=" + recorded.structureType()
                + " entity_id=" + recorded.entityId();
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
