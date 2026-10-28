package dev.explorationcore;

import dev.explorationcore.command.ExplorationBookCommand;
import dev.explorationcore.command.ExplorationCommand;
import dev.explorationcore.counter.ExplorationCounterExpansion;
import dev.explorationcore.counter.TallyCache;
import dev.explorationcore.db.DatabaseFiles;
import dev.explorationcore.db.DiscoveryRepository;
import dev.explorationcore.location.ExplorationLocationExpansion;
import dev.explorationcore.reward.StructureRewards;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandSendEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.logging.Level;

public final class ExplorationCorePlugin extends JavaPlugin implements Listener {
    private String serverName = "";
    private boolean debug;
    private StructureRewards structureRewards = StructureRewards.none();
    private DiscoveryRepository repository;
    private final TallyCache tallyCache = new TallyCache();
    private ExplorationLocationExpansion locationExpansion;
    private ExplorationCounterExpansion counterExpansion;

    @Override
    public void onEnable() {
        if (!getDataFolder().exists() && !getDataFolder().mkdirs()) {
            getLogger().severe("Could not create plugin data folder " + getDataFolder().getAbsolutePath());
        }
        saveDefaultConfig();

        String configuredName = getConfig().getString("server-name", "");
        if (configuredName == null || configuredName.isBlank()) {
            serverName = "";
        } else {
            serverName = configuredName.trim();
        }
        debug = getConfig().getBoolean("debug", false);
        try {
            structureRewards = StructureRewards.load(getConfig(), getLogger());
        } catch (RuntimeException exception) {
            structureRewards = StructureRewards.none();
            getLogger().log(Level.SEVERE, "rewards is malformed. Structure XP rewards are disabled.", exception);
        }

        Path databaseFile = DatabaseFiles.resolve(
                getDataFolder().toPath(),
                getConfig().getString("database.file", "exploration.db")
        );
        try {
            repository = DiscoveryRepository.open(databaseFile);
        } catch (SQLException | IOException exception) {
            repository = null;
            getLogger().log(Level.SEVERE, "Failed to open SQLite database at " + databaseFile, exception);
        }

        PluginCommand command = getCommand("explorationcore");
        if (command == null) {
            getLogger().severe("Command explorationcore is missing from plugin.yml");
        } else {
            ExplorationCommand executor = new ExplorationCommand(this);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }
        PluginCommand bookCommand = getCommand("exploration");
        if (bookCommand == null) {
            getLogger().severe("Command exploration is missing from plugin.yml");
        } else {
            bookCommand.setExecutor(new ExplorationBookCommand(this));
        }
        getServer().getPluginManager().registerEvents(this, this);
        registerPlaceholders();

        if (repository != null) {
            getLogger().info("SQLite database ready");
        }
        if (serverName.isBlank()) {
            getLogger().severe("server-name is missing or blank. Record commands will be refused until it is set.");
        } else {
            getLogger().info("Server identity: " + serverName);
        }
    }

    @Override
    public void onDisable() {
        ExplorationLocationExpansion expansion = locationExpansion;
        locationExpansion = null;
        if (expansion != null) {
            expansion.unregister();
        }
        ExplorationCounterExpansion counters = counterExpansion;
        counterExpansion = null;
        if (counters != null) {
            counters.unregister();
        }

        DiscoveryRepository openRepository = repository;
        repository = null;
        if (openRepository == null) {
            return;
        }
        try {
            openRepository.close();
        } catch (SQLException exception) {
            getLogger().log(Level.SEVERE, "Failed to close SQLite database", exception);
        }
    }

    @EventHandler
    public void hideCommandFromPlayers(PlayerCommandSendEvent event) {
        if (!event.getPlayer().hasPermission("explorationcore.status")
                && !event.getPlayer().hasPermission("explorationcore.record")) {
            event.getCommands().remove("explorationcore");
        }
        if (!event.getPlayer().hasPermission("explorationcore.exploration")) {
            event.getCommands().remove("exploration");
        }
    }

    public String serverName() {
        return serverName;
    }

    public boolean debug() {
        return debug;
    }

    public StructureRewards structureRewards() {
        return structureRewards;
    }

    public DiscoveryRepository repository() {
        return repository;
    }

    private void registerPlaceholders() {
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") == null) {
            getLogger().info("PlaceholderAPI is absent. Placeholders are unavailable.");
            return;
        }
        registerLocationExpansion();
        registerCounterExpansion();
    }

    private void registerLocationExpansion() {
        ExplorationLocationExpansion expansion = new ExplorationLocationExpansion(
                getDataFolder().toPath().resolve("location").resolve("index.yml"),
                getDescription().getVersion()
        );
        if (expansion.register()) {
            locationExpansion = expansion;
            getLogger().info("Location placeholders registered");
        } else {
            getLogger().warning("Location placeholders were not registered. WorldGuard is required.");
        }
    }

    private void registerCounterExpansion() {
        ExplorationCounterExpansion expansion = new ExplorationCounterExpansion(
                getDescription().getVersion(),
                getLogger(),
                repository,
                tallyCache
        );
        if (expansion.register()) {
            counterExpansion = expansion;
            getLogger().info("Discovery counters registered");
        } else {
            getLogger().warning("Discovery counters were not registered.");
        }
    }
}
