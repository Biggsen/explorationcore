package dev.explorationcore.location;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * PlaceholderAPI identifier {@code exploration}. Answers
 * {@code %exploration_current_region%}, {@code %exploration_current_village%}, and
 * {@code %exploration_current_structure%}.
 */
public final class ExplorationLocationExpansion extends PlaceholderExpansion {
    private final LocationIndexLoader loader;
    private final String version;

    public ExplorationLocationExpansion(Path indexFile, String version) {
        this.loader = new LocationIndexLoader(indexFile);
        this.version = version;
    }

    @Override
    public String getIdentifier() {
        return "exploration";
    }

    @Override
    public String getAuthor() {
        return "exploration";
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String getRequiredPlugin() {
        return "WorldGuard";
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (!LocationResolver.isKnown(params)) {
            return null;
        }
        LocationIndex index = loader.current();
        if (player == null || !player.isOnline()) {
            return LocationResolver.UNRESOLVED;
        }
        Player online = player.getPlayer();
        if (online == null) {
            return LocationResolver.UNRESOLVED;
        }
        return LocationResolver.resolve(index, online.getWorld().getName(), regionIds(online), params);
    }

    private static List<String> regionIds(Player player) {
        ApplicableRegionSet set =
                WorldGuard.getInstance()
                        .getPlatform()
                        .getRegionContainer()
                        .createQuery()
                        .getApplicableRegions(BukkitAdapter.adapt(player.getLocation()));
        List<ProtectedRegion> regions = new ArrayList<>();
        for (ProtectedRegion region : set.getRegions()) {
            if (region != null && !ProtectedRegion.GLOBAL_REGION.equals(region.getId())) {
                regions.add(region);
            }
        }
        regions.sort(Comparator.comparingInt(ProtectedRegion::getPriority).reversed());
        List<String> ids = new ArrayList<>(regions.size());
        for (ProtectedRegion region : regions) {
            ids.add(region.getId());
        }
        return ids;
    }
}
