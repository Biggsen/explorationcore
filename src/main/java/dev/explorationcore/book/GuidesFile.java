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

public final class GuidesFile {
    private GuidesFile() {
    }

    public static List<GuideChapter> load(Path file) throws IOException, GuidesException {
        if (!Files.isRegularFile(file)) {
            throw new GuidesException("guides.yml is missing");
        }
        Object loaded;
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            loaded = new Yaml().load(reader);
        } catch (RuntimeException exception) {
            throw new GuidesException("guides.yml is malformed");
        }
        Map<String, Object> root = map(loaded, "guides.yml");
        if (!root.containsKey("chapters")) {
            throw new GuidesException("chapters is missing");
        }
        Object chapters = root.get("chapters");
        if (!(chapters instanceof List<?> list)) {
            throw new GuidesException("chapters must be a list");
        }
        List<GuideChapter> loadedChapters = new ArrayList<>();
        for (int index = 0; index < list.size(); index++) {
            String path = "chapters[" + index + "]";
            Map<String, Object> body = map(list.get(index), path);
            loadedChapters.add(new GuideChapter(readTitle(body, path), readText(body, path)));
        }
        return List.copyOf(loadedChapters);
    }

    private static String readTitle(Map<String, Object> body, String path) throws GuidesException {
        String title = readString(body, path, "title");
        if (title.isBlank()) {
            throw new GuidesException(path + ".title must be a non-blank string");
        }
        return title.trim();
    }

    private static String readText(Map<String, Object> body, String path) throws GuidesException {
        return readString(body, path, "text");
    }

    private static String readString(Map<String, Object> body, String path, String field) throws GuidesException {
        if (!body.containsKey(field) || body.get(field) == null) {
            throw new GuidesException(path + "." + field + " is missing");
        }
        Object value = body.get(field);
        if (!(value instanceof String text)) {
            throw new GuidesException(path + "." + field + " must be a string");
        }
        return text;
    }

    private static Map<String, Object> map(Object value, String path) throws GuidesException {
        if (!(value instanceof Map<?, ?> raw)) {
            throw new GuidesException(path + " must be a map");
        }
        Map<String, Object> map = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : raw.entrySet()) {
            if (!(entry.getKey() instanceof String key) || key.isBlank()) {
                throw new GuidesException(path + " has a blank key");
            }
            map.put(key, entry.getValue());
        }
        return map;
    }
}
