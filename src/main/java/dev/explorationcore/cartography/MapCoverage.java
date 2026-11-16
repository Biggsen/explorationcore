package dev.explorationcore.cartography;

import java.util.Locale;
import java.util.OptionalInt;

/**
 * Counts explored pixels in a vanilla map colour buffer. Palette ids above 127 are
 * negative Java bytes, and only 0 means the pixel was never sampled.
 */
public final class MapCoverage {
    public static final int PIXELS = 128 * 128;
    public static final double REQUIRED_RATIO = 0.98;

    private MapCoverage() {
    }

    public static OptionalInt explored(byte[] colors) {
        if (colors == null || colors.length != PIXELS) {
            return OptionalInt.empty();
        }
        int count = 0;
        for (byte color : colors) {
            if ((color & 0xFF) != 0) {
                count++;
            }
        }
        return OptionalInt.of(count);
    }

    /**
     * Compares the raw ratio. 16056 / 16384 prints as 98.0% and is still short of 98%.
     */
    public static boolean meetsRequired(int explored) {
        return explored / (double) PIXELS >= REQUIRED_RATIO;
    }

    public static String percent(int explored) {
        return String.format(Locale.ROOT, "%.1f%%", explored * 100.0 / PIXELS);
    }
}
