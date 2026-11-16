package dev.explorationcore.cartography;

/**
 * Vanilla map tiles. A map is not centred on the player. Its centre is snapped to
 * {@code grid * size + size / 2 - 64}, where {@code size} is {@code 128 << scale}.
 */
public final class MapGrid {
    public static final int REQUIRED_SCALE = 4;

    private MapGrid() {
    }

    public static int size(int scale) {
        return 128 << scale;
    }

    public static boolean contains(int scale, int centerX, int centerZ, int blockX, int blockZ) {
        int half = size(scale) / 2;
        return blockX >= centerX - half
                && blockX <= centerX + half - 1
                && blockZ >= centerZ - half
                && blockZ <= centerZ + half - 1;
    }

    public static Tile tile(int scale, int centerX, int centerZ) {
        int size = size(scale);
        int x = Math.floorDiv(centerX + 64 - size / 2, size);
        int z = Math.floorDiv(centerZ + 64 - size / 2, size);
        boolean aligned = centerX == canonical(x, size) && centerZ == canonical(z, size);
        return new Tile(x, z, aligned);
    }

    static int canonical(int grid, int size) {
        return grid * size + size / 2 - 64;
    }

    public record Tile(int x, int z, boolean aligned) {
    }
}
