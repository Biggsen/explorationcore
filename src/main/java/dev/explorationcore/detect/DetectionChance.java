package dev.explorationcore.detect;

public final class DetectionChance {
    private DetectionChance() {
    }

    public static double distance(
            double dx,
            double dy,
            double dz,
            double horizontalRadius,
            double verticalRadius
    ) {
        return Math.sqrt(
                (dx * dx + dz * dz) / (horizontalRadius * horizontalRadius)
                        + (dy * dy) / (verticalRadius * verticalRadius)
        );
    }

    public static double chance(double distance, double maxProbability, double falloff) {
        if (distance >= 1.0) {
            return 0.0;
        }
        return maxProbability * Math.pow(1.0 - distance, falloff);
    }

    public static boolean succeeds(double chance, double draw) {
        return draw < chance;
    }
}
