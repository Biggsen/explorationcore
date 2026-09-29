package dev.explorationcore.db;

import java.nio.file.Path;

public final class DatabaseFiles {
    private DatabaseFiles() {
    }

    public static Path resolve(Path dataFolder, String configured) {
        String file = configured == null ? "" : configured.trim();
        if (file.isEmpty()) {
            file = "exploration.db";
        }
        Path path = Path.of(file);
        if (!path.isAbsolute()) {
            path = dataFolder.resolve(path);
        }
        return path.normalize();
    }
}
