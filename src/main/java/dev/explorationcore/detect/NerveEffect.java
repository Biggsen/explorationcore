package dev.explorationcore.detect;

import org.bukkit.Material;

import java.util.Set;

public record NerveEffect(
        double horizontalRadius,
        double verticalRadius,
        Set<Material> ores,
        Material drop
) {
}
