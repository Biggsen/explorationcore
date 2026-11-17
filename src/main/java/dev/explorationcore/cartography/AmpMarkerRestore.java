package dev.explorationcore.cartography;

import dev.explorationcore.db.AmpMarkerStore;
import org.bukkit.Chunk;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapView;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class AmpMarkerRestore {
    private final AmpMarkerStore store;
    private final Set<Integer> restored = ConcurrentHashMap.newKeySet();

    public AmpMarkerRestore(AmpMarkerStore store) {
        this.store = store;
    }

    public void ensure(MapView view) {
        if (store == null || view == null || restored.contains(view.getId())) {
            return;
        }
        List<AmpMarkerStore.Marker> rows;
        try {
            rows = store.forMap(view.getId());
        } catch (SQLException exception) {
            return;
        }
        boolean applied = true;
        for (AmpMarkerStore.Marker row : rows) {
            SharedMapMarkers.Outcome outcome = SharedMapMarkers.mark(
                    view,
                    new AmpSelection.Amp(row.ampId(), row.kind(), row.name()),
                    row.x(),
                    row.y(),
                    row.z(),
                    view.getWorld()
            );
            if (outcome == SharedMapMarkers.Outcome.UNAVAILABLE) {
                applied = false;
            }
        }
        if (applied) {
            restored.add(view.getId());
        }
    }

    public void forget(int mapId) {
        restored.remove(mapId);
    }

    public void scanLoaded(Server server) {
        if (store == null || server == null) {
            return;
        }
        for (World world : server.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                ensureChunk(chunk);
            }
        }
    }

    public void ensureChunk(Chunk chunk) {
        if (chunk == null) {
            return;
        }
        for (Entity entity : chunk.getEntities()) {
            if (entity instanceof ItemFrame frame) {
                ensureItem(frame.getItem());
            }
        }
    }

    public void ensureItem(ItemStack stack) {
        ensure(mapView(stack));
    }

    public int storedCount(int mapId) throws SQLException {
        if (store == null) {
            throw new SQLException("SQLite database is closed");
        }
        return store.forMap(mapId).size();
    }

    public boolean available() {
        return store != null;
    }

    private static MapView mapView(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta() || !(stack.getItemMeta() instanceof MapMeta meta) || !meta.hasMapView()) {
            return null;
        }
        return meta.getMapView();
    }
}
