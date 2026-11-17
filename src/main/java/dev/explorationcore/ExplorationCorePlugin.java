package dev.explorationcore;

import dev.explorationcore.atlas.AtlasListener;
import dev.explorationcore.book.AnvilRenameListener;
import dev.explorationcore.book.AvailableDiscoveries;
import dev.explorationcore.book.JournalListener;
import dev.explorationcore.cartography.AmpMapListener;
import dev.explorationcore.cartography.AmpMarkerRestore;
import dev.explorationcore.cartography.AmpMarkerRestoreListener;
import dev.explorationcore.cartography.CartographyTestCommand;
import dev.explorationcore.command.AtlasCommand;
import dev.explorationcore.command.ExplorationBookCommand;
import dev.explorationcore.command.ExplorationCommand;
import dev.explorationcore.counter.DiscoveryCounters;
import dev.explorationcore.counter.ExplorationCounterExpansion;
import dev.explorationcore.counter.TallyCache;
import dev.explorationcore.db.DatabaseFiles;
import dev.explorationcore.db.DiscoveryRepository;
import dev.explorationcore.detect.DetectionCatalogue;
import dev.explorationcore.detect.DetectionSettings;
import dev.explorationcore.detect.DetectionSites;
import dev.explorationcore.detect.EnvironmentalDetectionListener;
import dev.explorationcore.location.ExplorationLocationExpansion;
import dev.explorationcore.location.LocationIndexLoader;
import dev.explorationcore.milestone.Milestones;
import dev.explorationcore.reward.DiscoveryCrates;
import dev.explorationcore.reward.RegionBooks;
import dev.explorationcore.reward.StructureRewards;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandSendEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.logging.Level;

public final class ExplorationCorePlugin extends JavaPlugin implements Listener {
    private String serverName = "";
    private String messagePrefix = "§6⌖ §r";
    private boolean debug;
    private StructureRewards structureRewards = StructureRewards.none();
    private DiscoveryCrates discoveryCrates = DiscoveryCrates.none();
    private RegionBooks regionBooks = RegionBooks.none();
    private Milestones milestones = Milestones.none();
    private DiscoveryCounters counters = DiscoveryCounters.none();
    private DiscoveryRepository repository;
    private AmpMarkerRestore ampMarkerRestore;
    private final TallyCache tallyCache = new TallyCache();
    private ExplorationLocationExpansion locationExpansion;
    private ExplorationCounterExpansion counterExpansion;

    @Override
    public void onEnable() {
        if (!getDataFolder().exists() && !getDataFolder().mkdirs()) {
            getLogger().severe("Could not create plugin data folder " + getDataFolder().getAbsolutePath());
        }
        saveDefaultConfig();
        saveGuidesFile();

        String configuredName = getConfig().getString("server-name", "");
        if (configuredName == null || configuredName.isBlank()) {
            serverName = "";
        } else {
            serverName = configuredName.trim();
        }
        debug = getConfig().getBoolean("debug", false);
        String configuredPrefix = getConfig().getString("messages.prefix", "§6⌖ §r");
        if (configuredPrefix == null) {
            messagePrefix = "§6⌖ §r";
        } else {
            messagePrefix = configuredPrefix.trim();
        }
        try {
            structureRewards = StructureRewards.load(getConfig(), getLogger());
        } catch (RuntimeException exception) {
            structureRewards = StructureRewards.none();
            getLogger().log(Level.SEVERE, "rewards is malformed. Structure XP rewards are disabled.", exception);
        }
        try {
            discoveryCrates = DiscoveryCrates.load(getConfig(), getLogger());
        } catch (RuntimeException exception) {
            discoveryCrates = DiscoveryCrates.none();
            getLogger().log(
                    Level.SEVERE,
                    "discovery-crates is malformed. Discovery crate rewards are disabled.",
                    exception
            );
        }
        try {
            regionBooks = RegionBooks.load(getConfig(), getLogger());
        } catch (RuntimeException exception) {
            regionBooks = RegionBooks.none();
            getLogger().log(
                    Level.SEVERE,
                    "region-books is malformed. Region book permissions are disabled.",
                    exception
            );
        }
        counters = loadStructureCounters();
        milestones = loadMilestones();

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
        ExplorationBookCommand bookExecutor = new ExplorationBookCommand(this);
        PluginCommand bookCommand = getCommand("exploration");
        if (bookCommand == null) {
            getLogger().severe("Command exploration is missing from plugin.yml");
        } else {
            bookCommand.setExecutor(bookExecutor);
            bookCommand.setTabCompleter(bookExecutor);
        }
        PluginCommand guidesCommand = getCommand("guides");
        if (guidesCommand == null) {
            getLogger().severe("Command guides is missing from plugin.yml");
        } else {
            guidesCommand.setExecutor(bookExecutor);
            guidesCommand.setTabCompleter(bookExecutor);
        }
        PluginCommand atlasCommand = getCommand("atlas");
        if (atlasCommand == null) {
            getLogger().severe("Command atlas is missing from plugin.yml");
        } else {
            AtlasCommand atlasExecutor = new AtlasCommand();
            atlasCommand.setExecutor(atlasExecutor);
            atlasCommand.setTabCompleter(atlasExecutor);
        }
        PluginCommand cartographyCommand = getCommand("cartographytest");
        if (cartographyCommand == null) {
            getLogger().severe("Command cartographytest is missing from plugin.yml");
        } else {
            cartographyCommand.setExecutor(new CartographyTestCommand(this));
        }
        ampMarkerRestore = new AmpMarkerRestore(repository == null ? null : repository.ampMarkers());
        getServer().getPluginManager().registerEvents(new AmpMarkerRestoreListener(ampMarkerRestore), this);
        ampMarkerRestore.scanLoaded(getServer());
        if (getServer().getPluginManager().getPlugin("WorldGuard") != null) {
            getServer().getPluginManager().registerEvents(
                    new AmpMapListener(
                            this,
                            new LocationIndexLoader(getDataFolder().toPath().resolve("location").resolve("index.yml")),
                            repository == null ? null : repository.ampMarkers(),
                            ampMarkerRestore
                    ),
                    this
            );
        }
        getServer().getPluginManager().registerEvents(
                new JournalListener(bookExecutor::openJournal, bookExecutor::openGuides),
                this
        );
        getServer().getPluginManager().registerEvents(new AnvilRenameListener(), this);
        getServer().getPluginManager().registerEvents(new AtlasListener(this), this);
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
        registerDetection();
    }

    private void saveGuidesFile() {
        if (new File(getDataFolder(), "guides.yml").isFile()) {
            return;
        }
        saveResource("guides.yml", false);
    }

    private void registerDetection() {
        DetectionSettings settings;
        try {
            settings = DetectionSettings.load(getConfig(), getLogger());
        } catch (RuntimeException exception) {
            getLogger().log(Level.SEVERE, "detection is malformed. Environmental detection is disabled.", exception);
            return;
        }
        if (!settings.enabled()) {
            return;
        }
        Path file = getDataFolder().toPath().resolve("detection-sites.yml");
        if (!Files.isRegularFile(file)) {
            getLogger().info("detection-sites.yml is missing. Environmental detection is disabled.");
            return;
        }
        DetectionCatalogue catalogue;
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            catalogue = DetectionSites.parse(reader);
        } catch (IOException | RuntimeException exception) {
            getLogger().log(
                    Level.SEVERE,
                    "detection-sites.yml is malformed. Environmental detection is disabled.",
                    exception
            );
            return;
        }
        getLogger().info(
                "Detection sites loaded: "
                        + counted(catalogue.hearts(), "heart", "hearts")
                        + ", "
                        + counted(catalogue.nerves(), "nerve", "nerves")
                        + ", "
                        + counted(catalogue.souls(), "soul", "souls")
        );
        if (catalogue.isEmpty()) {
            return;
        }
        getServer().getPluginManager().registerEvents(new EnvironmentalDetectionListener(settings, catalogue), this);
    }

    private static String counted(int count, String singular, String plural) {
        return count + " " + (count == 1 ? singular : plural);
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
        ampMarkerRestore = null;
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
            event.getCommands().remove("atlas");
        }
        if (!event.getPlayer().hasPermission(CartographyTestCommand.PERMISSION)) {
            event.getCommands().remove("cartographytest");
        }
    }

    public String serverName() {
        return serverName;
    }

    public boolean debug() {
        return debug;
    }

    public String messagePrefix() {
        return messagePrefix;
    }

    public StructureRewards structureRewards() {
        return structureRewards;
    }

    public DiscoveryCrates discoveryCrates() {
        return discoveryCrates;
    }

    public RegionBooks regionBooks() {
        return regionBooks;
    }

    public Milestones milestones() {
        return milestones;
    }

    public DiscoveryCounters counters() {
        return counters;
    }

    private Milestones loadMilestones() {
        File file = new File(getDataFolder(), "milestones.yml");
        if (!file.exists()) {
            try {
                saveResource("milestones.yml", false);
            } catch (IllegalArgumentException exception) {
                getLogger().log(
                        Level.SEVERE,
                        "milestones.yml is missing from the plugin jar. Milestone rewards are disabled.",
                        exception
                );
                return Milestones.none();
            }
        }
        if (!file.exists()) {
            getLogger().severe("milestones.yml is missing. Milestone rewards are disabled.");
            return Milestones.none();
        }
        YamlConfiguration yaml = new YamlConfiguration();
        try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            yaml.load(reader);
        } catch (IOException | InvalidConfigurationException exception) {
            getLogger().log(Level.SEVERE, "milestones.yml is malformed. Milestone rewards are disabled.", exception);
            return Milestones.none();
        }
        return Milestones.load(yaml, getLogger(), structureRewards.configuredCommand(), counters);
    }

    private DiscoveryCounters loadStructureCounters() {
        File file = new File(getDataFolder(), "structure-counters.yml");
        if (!file.exists()) {
            try {
                saveResource("structure-counters.yml", false);
            } catch (IllegalArgumentException exception) {
                getLogger().log(
                        Level.SEVERE,
                        "structure-counters.yml is missing from the plugin jar.",
                        exception
                );
                return DiscoveryCounters.none();
            }
        }
        DiscoveryCounters loaded = DiscoveryCounters.load(file.toPath(), getLogger());
        if (loaded.accepted()) {
            getLogger().info("Structure counters loaded: " + loaded.familyCount());
        }
        return loaded;
    }

    public DiscoveryRepository repository() {
        return repository;
    }

    public AmpMarkerRestore ampMarkerRestore() {
        return ampMarkerRestore;
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
                tallyCache,
                counters,
                new AvailableDiscoveries(getDataFolder().toPath().resolve("totals.yml"), getLogger())
        );
        if (expansion.register()) {
            counterExpansion = expansion;
            getLogger().info("Discovery counters registered");
        } else {
            getLogger().warning("Discovery counters were not registered.");
        }
    }
}
