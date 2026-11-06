package dev.explorationcore.book;

import java.util.List;

public record ExplorationTotals(
        int overworldRegions,
        int waterRegions,
        int villages,
        int hearts,
        int nerves,
        List<StructureTotal> structures,
        int netherRegions,
        int netherHearts
) {
    public ExplorationTotals {
        structures = List.copyOf(structures);
    }
}
