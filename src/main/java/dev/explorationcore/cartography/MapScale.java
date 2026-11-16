package dev.explorationcore.cartography;

import org.bukkit.map.MapView;

public final class MapScale {
    private MapScale() {
    }

    public static int of(MapView.Scale scale) {
        return switch (scale) {
            case CLOSEST -> 0;
            case CLOSE -> 1;
            case NORMAL -> 2;
            case FAR -> 3;
            case FARTHEST -> 4;
        };
    }
}
