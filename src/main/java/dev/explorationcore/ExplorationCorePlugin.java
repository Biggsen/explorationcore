package dev.explorationcore;

import dev.explorationcore.command.ExplorationCommand;
import dev.explorationcore.db.DatabaseFiles;
import dev.explorationcore.db.DiscoveryRepository;
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
    private DiscoveryRepository repository;

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
        getServer().getPluginManager().registerEvents(this, this);

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
        if (event.getPlayer().hasPermission("explorationcore.status")
                || event.getPlayer().hasPermission("explorationcore.record")) {
            return;
        }
        event.getCommands().remove("explorationcore");
    }

    public String serverName() {
        return serverName;
    }

    public boolean debug() {
        return debug;
    }

    public DiscoveryRepository repository() {
        return repository;
    }
}
