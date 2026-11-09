package dev.explorationcore.detect;

import org.bukkit.Material;

public record PlacedCandle(Material material, int count, boolean lit) {
}
