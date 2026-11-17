package dev.explorationcore.atlas;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

public final class AtlasHolder implements InventoryHolder {
    private final UUID atlasId;
    private Inventory inventory;

    public AtlasHolder(UUID atlasId) {
        this.atlasId = atlasId;
    }

    public UUID atlasId() {
        return atlasId;
    }

    public void attach(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
