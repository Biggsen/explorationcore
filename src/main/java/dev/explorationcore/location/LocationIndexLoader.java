package dev.explorationcore.location;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Reads {@code plugins/ExplorationCore/location/index.yml} and reparses it only when
 * {@code lastModified} changes. A missing file or a failed parse is an empty index.
 */
public final class LocationIndexLoader {
    private static final Logger LOG = Logger.getLogger(LocationIndexLoader.class.getName());

    private final Path file;
    private LocationIndex cached = LocationIndex.empty();
    private long cachedModified = Long.MIN_VALUE;

    public LocationIndexLoader(Path file) {
        this.file = file;
    }

    public synchronized LocationIndex current() {
        try {
            if (!Files.isRegularFile(file)) {
                cached = LocationIndex.empty();
                cachedModified = Long.MIN_VALUE;
                return cached;
            }
            long modified = Files.getLastModifiedTime(file).toMillis();
            if (modified == cachedModified) {
                return cached;
            }
            try (InputStream in = Files.newInputStream(file)) {
                cached = parse(in);
            }
            cachedModified = modified;
            return cached;
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "Could not read location index " + file, ex);
            cached = LocationIndex.empty();
            cachedModified = lastModifiedOrUnset();
            return cached;
        }
    }

    public static LocationIndex parse(String yaml) {
        return parse(new StringReader(yaml));
    }

    public static LocationIndex parse(InputStream in) {
        Object loaded = new Yaml().load(in);
        return fromLoaded(loaded);
    }

    public static LocationIndex parse(Reader reader) {
        Object loaded = new Yaml().load(reader);
        return fromLoaded(loaded);
    }

    private static LocationIndex fromLoaded(Object loaded) {
        if (loaded == null) {
            return LocationIndex.empty();
        }
        if (!(loaded instanceof Map<?, ?> root)) {
            throw new IllegalArgumentException("location index root must be a map");
        }
        Map<String, Map<String, LocationIndex.Entry>> worlds = new LinkedHashMap<>();
        for (Map.Entry<?, ?> worldEntry : root.entrySet()) {
            if (!(worldEntry.getKey() instanceof String worldName)) {
                continue;
            }
            if (!(worldEntry.getValue() instanceof Map<?, ?> regions)) {
                continue;
            }
            Map<String, LocationIndex.Entry> parsedRegions = new LinkedHashMap<>();
            for (Map.Entry<?, ?> regionEntry : regions.entrySet()) {
                if (!(regionEntry.getKey() instanceof String regionId)) {
                    continue;
                }
                if (!(regionEntry.getValue() instanceof Map<?, ?> fields)) {
                    continue;
                }
                if (!(fields.get("kind") instanceof String kind) || !(fields.get("name") instanceof String name)) {
                    continue;
                }
                String color = fields.get("color") instanceof String value && !value.isEmpty() ? value : null;
                parsedRegions.put(regionId, new LocationIndex.Entry(kind, name, color));
            }
            worlds.put(worldName, Map.copyOf(parsedRegions));
        }
        return new LocationIndex(Map.copyOf(worlds));
    }

    private long lastModifiedOrUnset() {
        try {
            if (Files.isRegularFile(file)) {
                return Files.getLastModifiedTime(file).toMillis();
            }
        } catch (IOException ignored) {
            // The next request will try again.
        }
        return Long.MIN_VALUE;
    }
}
