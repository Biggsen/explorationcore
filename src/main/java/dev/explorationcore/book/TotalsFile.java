package dev.explorationcore.book;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TotalsFile {
    private TotalsFile() {
    }

    public static ExplorationTotals load(Path file) throws IOException, TotalsException {
        if (!Files.isRegularFile(file)) {
            throw new TotalsException("totals.yml is missing");
        }
        Object loaded;
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            loaded = new Yaml().load(reader);
        } catch (RuntimeException exception) {
            throw new TotalsException("totals.yml is malformed");
        }
        Map<String, Object> root = map(loaded, "totals.yml");
        Map<String, Object> overworld = map(root.get("overworld"), "overworld");
        Map<String, Object> nether = map(root.get("nether"), "nether");
        return new ExplorationTotals(
                readTotal(overworld, "overworld.regions"),
                readTotal(overworld, "overworld.villages"),
                readTotal(overworld, "overworld.hearts"),
                readTotal(overworld, "overworld.nerves"),
                readStructures(overworld.get("structures")),
                readTotal(nether, "nether.regions"),
                readTotal(nether, "nether.hearts")
        );
    }

    private static List<StructureTotal> readStructures(Object value) throws TotalsException {
        Map<String, Object> structures = map(value, "overworld.structures");
        List<StructureTotal> totals = new ArrayList<>();
        for (Map.Entry<String, Object> entry : structures.entrySet()) {
            String key = entry.getKey();
            String path = "overworld.structures." + key;
            Map<String, Object> body = map(entry.getValue(), path);
            if (!body.containsKey("name")) {
                throw new TotalsException(path + ".name is missing");
            }
            Object nameValue = body.get("name");
            if (!(nameValue instanceof String name) || name.isBlank()) {
                throw new TotalsException(path + ".name must be a non-blank string");
            }
            int total = readInteger(body.get("total"), path + ".total");
            if (total < 1) {
                throw new TotalsException(path + ".total must be at least 1");
            }
            totals.add(new StructureTotal(key, name, total));
        }
        return totals;
    }

    private static int readTotal(Map<String, Object> parent, String path) throws TotalsException {
        String field = path.substring(path.lastIndexOf('.') + 1);
        Object section = parent.get(field);
        if (section == null) {
            throw new TotalsException(path + " is missing");
        }
        Map<String, Object> body = map(section, path);
        if (!body.containsKey("total")) {
            throw new TotalsException(path + ".total is missing");
        }
        return readInteger(body.get("total"), path + ".total");
    }

    private static int readInteger(Object value, String path) throws TotalsException {
        if (!(value instanceof Integer || value instanceof Long || value instanceof Short || value instanceof Byte)) {
            throw new TotalsException(path + " must be an integer of zero or more");
        }
        long total = ((Number) value).longValue();
        if (total < 0 || total > Integer.MAX_VALUE) {
            throw new TotalsException(path + " must be an integer of zero or more");
        }
        return (int) total;
    }

    private static Map<String, Object> map(Object value, String path) throws TotalsException {
        if (!(value instanceof Map<?, ?> raw)) {
            throw new TotalsException(path + " must be a map");
        }
        Map<String, Object> map = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : raw.entrySet()) {
            if (!(entry.getKey() instanceof String key) || key.isBlank()) {
                throw new TotalsException(path + " has a blank key");
            }
            map.put(key, entry.getValue());
        }
        return map;
    }
}
