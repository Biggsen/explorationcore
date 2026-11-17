package dev.explorationcore.atlas;

public final class AtlasBoard {
    private final String world;
    private final AtlasLayout.Origin origin;
    private final String[] cells;

    private AtlasBoard(String world, AtlasLayout.Origin origin, String[] cells) {
        this.world = world;
        this.origin = origin;
        this.cells = cells;
    }

    public static AtlasBoard empty() {
        return new AtlasBoard(null, null, new String[AtlasLayout.CELLS]);
    }

    public static AtlasBoard of(String world, AtlasLayout.Origin origin, String[] cells) {
        String[] copy = new String[AtlasLayout.CELLS];
        if (cells != null) {
            System.arraycopy(cells, 0, copy, 0, Math.min(cells.length, copy.length));
        }
        for (String cell : copy) {
            if (cell != null) {
                return new AtlasBoard(world, origin, copy);
            }
        }
        return empty();
    }

    public String world() {
        return world;
    }

    public AtlasLayout.Origin origin() {
        return origin;
    }

    public String cell(int index) {
        return cells[index];
    }

    AtlasBoard place(int index, String token, String world, AtlasLayout.Origin origin) {
        String[] next = cells.clone();
        next[index] = token;
        return new AtlasBoard(world, origin, next);
    }

    AtlasBoard clear(int index) {
        String[] next = cells.clone();
        next[index] = null;
        return of(world, origin, next);
    }
}
