package dev.explorationcore.book;

import java.util.List;

public record ExplorationTotals(
        int overworldRegions,
        int waterRegions,
        int villages,
        int hearts,
        int nerves,
        int souls,
        List<StructureTotal> structures,
        int netherRegions,
        int netherHearts
) {
    public ExplorationTotals {
        structures = List.copyOf(structures);
    }

    /**
     * Places that count toward the world-explored percentage.
     * Structure totals have their own percentage and stay out of this sum.
     */
    public int available() {
        return overworldRegions + waterRegions + villages + hearts
                + nerves + souls + netherRegions + netherHearts;
    }
}
