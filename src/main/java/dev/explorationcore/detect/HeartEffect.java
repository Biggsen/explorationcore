package dev.explorationcore.detect;

import org.bukkit.Material;

public record HeartEffect(
        double horizontalRadius,
        double verticalRadius,
        Material place,
        Material become
) {
}
