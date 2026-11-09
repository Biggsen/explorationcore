package dev.explorationcore.detect;

import org.bukkit.Material;

import java.util.EnumSet;
import java.util.Set;

public final class SoulCandles {
    private static final Set<Material> CANDLES = candles();

    private SoulCandles() {
    }

    public static boolean isCandle(Material material) {
        return CANDLES.contains(material);
    }

    public static PlacedCandle light(PlacedCandle placed) {
        if (placed == null || !isCandle(placed.material()) || placed.lit()) {
            return placed;
        }
        return new PlacedCandle(placed.material(), placed.count(), true);
    }

    private static Set<Material> candles() {
        Set<Material> candles = EnumSet.noneOf(Material.class);
        for (Material material : Material.values()) {
            String name = material.name();
            if ("CANDLE".equals(name) || name.endsWith("_CANDLE")) {
                candles.add(material);
            }
        }
        return Set.copyOf(candles);
    }
}
