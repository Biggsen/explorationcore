package dev.explorationcore.detect;

import java.util.List;
import java.util.Optional;

public final class DetectionQuery {
    private DetectionQuery() {
    }

    public static Optional<DetectionHit> nearest(
            List<DetectionSite> sites,
            String type,
            String worldName,
            String overworldFolder,
            int x,
            int y,
            int z,
            double horizontalRadius,
            double verticalRadius
    ) {
        if (worldName == null || overworldFolder == null || !worldName.equals(overworldFolder)) {
            return Optional.empty();
        }
        DetectionHit nearest = null;
        if (sites == null) {
            return Optional.empty();
        }
        for (DetectionSite site : sites) {
            if (site == null || !type.equals(site.type())) {
                continue;
            }
            double dx = (double) x - site.x();
            double dy = (double) y - site.y();
            double dz = (double) z - site.z();
            double distance = DetectionChance.distance(dx, dy, dz, horizontalRadius, verticalRadius);
            if (distance >= 1.0) {
                continue;
            }
            if (nearest == null || distance < nearest.distance()) {
                nearest = new DetectionHit(site, distance);
            }
        }
        return Optional.ofNullable(nearest);
    }
}
