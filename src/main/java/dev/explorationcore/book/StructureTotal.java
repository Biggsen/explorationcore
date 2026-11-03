package dev.explorationcore.book;

public record StructureTotal(String key, String name, String shortLabel, int total) {
    public StructureTotal(String key, String name, int total) {
        this(key, name, null, total);
    }

    public String label() {
        if (shortLabel == null || shortLabel.isBlank()) {
            return name;
        }
        return shortLabel;
    }
}
