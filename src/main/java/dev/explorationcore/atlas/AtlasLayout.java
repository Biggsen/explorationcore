package dev.explorationcore.atlas;

public final class AtlasLayout {
    public static final int SPAN = 4;
    public static final int CELLS = SPAN * SPAN;
    public static final int ROW_WIDTH = 9;
    public static final int GUI_SIZE = ROW_WIDTH * SPAN;

    private AtlasLayout() {
    }

    public static Origin origin(int gridX, int gridZ) {
        return new Origin(aligned(gridX), aligned(gridZ));
    }

    private static int aligned(int grid) {
        return Math.floorDiv(grid + 2, SPAN) * SPAN - 2;
    }

    public static boolean inside(Origin origin, int gridX, int gridZ) {
        return gridX >= origin.x()
                && gridX < origin.x() + SPAN
                && gridZ >= origin.z()
                && gridZ < origin.z() + SPAN;
    }

    public static int column(Origin origin, int gridX) {
        return gridX - origin.x();
    }

    public static int row(Origin origin, int gridZ) {
        return gridZ - origin.z();
    }

    public static int index(Origin origin, int gridX, int gridZ) {
        return column(origin, gridX) + row(origin, gridZ) * SPAN;
    }

    public static int guiSlot(int index) {
        int column = index % SPAN;
        int row = index / SPAN;
        return row * ROW_WIDTH + column;
    }

    public static int guiSlot(Origin origin, int gridX, int gridZ) {
        return guiSlot(index(origin, gridX, gridZ));
    }

    public static boolean mapGuiSlot(int guiSlot) {
        if (guiSlot < 0 || guiSlot >= GUI_SIZE) {
            return false;
        }
        int column = guiSlot % ROW_WIDTH;
        int row = guiSlot / ROW_WIDTH;
        return column < SPAN && row < SPAN;
    }

    public static int index(int guiSlot) {
        int column = guiSlot % ROW_WIDTH;
        int row = guiSlot / ROW_WIDTH;
        return column + row * SPAN;
    }

    public record Origin(int x, int z) {
    }
}
