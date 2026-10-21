package dev.explorationcore.book;

import dev.explorationcore.db.PlayerTally;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Writes the browser preview from {@link ExplorationBook#prepare}.
 * The page strings are already wrapped, so the page only paints them.
 */
final class BookPreviewDump {
    // BookViewScreen places these relative to the top-left of textures/gui/book.png.
    private static final int IMAGE_WIDTH = 192;
    private static final int IMAGE_HEIGHT = 192;
    private static final int TEXT_X = 36;
    private static final int TEXT_Y = 30;
    private static final int LINE_HEIGHT = 9;
    private static final int GLYPH_SIZE = 8;
    private static final int PAGE_INDICATOR_Y = 16;
    private static final int PAGE_INDICATOR_RIGHT = 44;
    private static final int BUTTON_WIDTH = 23;
    private static final int BUTTON_HEIGHT = 13;

    private static final String ASCII_ENTRY = "assets/minecraft/textures/font/ascii.png";
    private static final String BOOK_ENTRY = "assets/minecraft/textures/gui/book.png";
    private static final String FORWARD_ENTRY = "assets/minecraft/textures/gui/sprites/widget/page_forward.png";
    private static final String FORWARD_HOVER_ENTRY = "assets/minecraft/textures/gui/sprites/widget/page_forward_highlighted.png";
    private static final String BACK_ENTRY = "assets/minecraft/textures/gui/sprites/widget/page_backward.png";
    private static final String BACK_HOVER_ENTRY = "assets/minecraft/textures/gui/sprites/widget/page_backward_highlighted.png";

    private BookPreviewDump() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            throw new IllegalArgumentException("Usage: BookPreviewDump <book.js> <assets-dir> [client.jar]");
        }
        Path bookJs = Path.of(args[0]);
        Path assetsDir = Path.of(args[1]);
        Path jar = args.length >= 3 ? Path.of(args[2]) : findClientJar();
        extractTextures(jar, assetsDir);
        String serverName = serverName();
        String playerName = System.getProperty("book.player", "vanism");
        PreparedBook book = realBook(serverName, playerName);
        Path parent = bookJs.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(bookJs, render(book, serverName));
        System.out.println("Wrote " + bookJs.toAbsolutePath());
    }

    static void writePreview(Path bookJs) throws IOException {
        Path parent = bookJs.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(bookJs, render(sample(), "Lowothra"));
    }

    private static PreparedBook realBook(String serverName, String requestedPlayer) throws Exception {
        Path totalsFile = Path.of("dev/book-preview/data/totals.yml");
        Path databaseFile = Path.of("dev/book-preview/data/exploration.db");
        ExplorationTotals totals = TotalsFile.load(totalsFile);
        PlayerRef player = player(databaseFile, requestedPlayer);
        List<PlayerTally> tallies = tallies(databaseFile, player.uuid());
        System.out.println("Book for " + player.name() + " on " + serverName
                + ". Overworld regions " + totals.overworldRegions()
                + ". Discoveries " + player.discoveries() + ".");
        return ExplorationBook.prepare(serverName, totals, tallies);
    }

    private static String serverName() throws IOException {
        Path config = Path.of("src/main/resources/config.yml");
        for (String line : Files.readAllLines(config)) {
            String trimmed = line.trim();
            if (trimmed.startsWith("server-name:")) {
                return trimmed.substring("server-name:".length()).trim();
            }
        }
        return "";
    }

    private static PlayerRef player(Path databaseFile, String requestedName) throws Exception {
        try (Connection connection = openReadOnly(databaseFile)) {
            PlayerRef match = findPlayer(connection, requestedName);
            if (match != null) {
                return match;
            }
            PlayerRef busiest = findPlayer(connection, null);
            if (busiest == null) {
                throw new IllegalStateException("exploration.db has no discoveries");
            }
            System.out.println("No player named " + requestedName + ". Using " + busiest.name() + ".");
            return busiest;
        }
    }

    private static PlayerRef findPlayer(Connection connection, String name) throws SQLException {
        String sql = """
                SELECT player_uuid, player_name, COUNT(*)
                FROM discoveries
                """;
        if (name != null) {
            sql += "WHERE player_name = ? COLLATE NOCASE\n";
        }
        sql += """
                GROUP BY player_uuid, player_name
                ORDER BY COUNT(*) DESC
                LIMIT 1
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            if (name != null) {
                statement.setString(1, name);
            }
            try (ResultSet results = statement.executeQuery()) {
                if (!results.next()) {
                    return null;
                }
                return new PlayerRef(results.getString(1), results.getString(2), results.getLong(3));
            }
        }
    }

    private static List<PlayerTally> tallies(Path databaseFile, String playerUuid) throws Exception {
        try (Connection connection = openReadOnly(databaseFile);
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT world, entity_type, structure_type, COUNT(*)
                     FROM discoveries
                     WHERE player_uuid = ?
                     GROUP BY world, entity_type, structure_type
                     """)) {
            statement.setString(1, playerUuid);
            try (ResultSet results = statement.executeQuery()) {
                List<PlayerTally> tallies = new ArrayList<>();
                while (results.next()) {
                    String structureType = results.getString(3);
                    if (structureType != null && structureType.isBlank()) {
                        structureType = null;
                    }
                    tallies.add(new PlayerTally(
                            results.getString(1),
                            results.getString(2),
                            structureType,
                            results.getLong(4)
                    ));
                }
                return tallies;
            }
        }
    }

    private static Connection openReadOnly(Path databaseFile) throws Exception {
        Class.forName("org.sqlite.JDBC");
        String path = databaseFile.toAbsolutePath().toString().replace('\\', '/');
        String url = "jdbc:sqlite:file:/" + path + "?mode=ro";
        return DriverManager.getConnection(url);
    }

    private record PlayerRef(String uuid, String name, long discoveries) {
    }

    static PreparedBook sample() {
        ExplorationTotals totals = new ExplorationTotals(
                56,
                56,
                30,
                30,
                List.of(
                        new StructureTotal("ancient_city", "Ancient Cities", 12),
                        new StructureTotal("buried_treasure", "Buried Treasures", 53),
                        new StructureTotal("desert_well", "Desert Wells", 1),
                        new StructureTotal("jungle_temple", "Jungle Temples", 11),
                        new StructureTotal("ocean_ruin", "Ocean Ruins", 169),
                        new StructureTotal("pillager_outpost", "Pillager Outposts", 8),
                        new StructureTotal("shipwreck", "Shipwrecks", 126),
                        new StructureTotal("trail_ruins", "Trail Ruins", 19)
                ),
                18,
                8
        );
        return ExplorationBook.prepare(
                "Lowothra",
                totals,
                List.of(
                        new PlayerTally("overworld", "region", null, 40),
                        new PlayerTally("overworld", "village", null, 3),
                        new PlayerTally("overworld", "heart", null, 2),
                        new PlayerTally("overworld", "nerve", null, 1),
                        new PlayerTally("overworld", "structure", "ancient_city", 1),
                        new PlayerTally("overworld", "structure", "buried_treasure", 4),
                        new PlayerTally("overworld", "structure", "ocean_ruin", 20),
                        new PlayerTally("overworld", "structure", "shipwreck", 12),
                        new PlayerTally("overworld", "structure", "mineshaft", 4),
                        new PlayerTally("nether", "region", null, 2),
                        new PlayerTally("nether", "heart", null, 1)
                )
        );
    }

    private static String render(PreparedBook book, String serverName) {
        StringBuilder out = new StringBuilder();
        out.append("window.EXPLORATION_BOOK = {\n");
        stringField(out, "title", ExplorationBook.TITLE);
        stringField(out, "author", serverName);
        stringField(out, "pageIndicator", "Page %s of %s");
        numberField(out, "pageWidth", BookText.PAGE_WIDTH);
        numberField(out, "linesPerPage", BookText.LINES_PER_PAGE);
        numberField(out, "lineHeight", LINE_HEIGHT);
        numberField(out, "glyphSize", GLYPH_SIZE);
        numberField(out, "imageWidth", IMAGE_WIDTH);
        numberField(out, "imageHeight", IMAGE_HEIGHT);
        numberField(out, "textX", TEXT_X);
        numberField(out, "textY", TEXT_Y);
        numberField(out, "pageIndicatorY", PAGE_INDICATOR_Y);
        numberField(out, "pageIndicatorRight", PAGE_INDICATOR_RIGHT);
        numberField(out, "unknownAdvance", BookText.unknownAdvance());
        out.append("  \"advances\": [");
        int[] advances = BookText.asciiAdvances();
        for (int index = 0; index < advances.length; index++) {
            if (index > 0) {
                out.append(", ");
            }
            out.append(advances[index]);
        }
        out.append("],\n");
        out.append("  \"previousButton\": { \"x\": 43, \"y\": 154, \"w\": ")
                .append(BUTTON_WIDTH).append(", \"h\": ").append(BUTTON_HEIGHT)
                .append(", \"u\": 0, \"v\": 205 },\n");
        out.append("  \"nextButton\": { \"x\": 116, \"y\": 154, \"w\": ")
                .append(BUTTON_WIDTH).append(", \"h\": ").append(BUTTON_HEIGHT)
                .append(", \"u\": 0, \"v\": 192 },\n");
        numberField(out, "buttonHoverU", 23);
        out.append("  \"pages\": [\n");
        List<String> pages = book.pages();
        for (int pageIndex = 0; pageIndex < pages.size(); pageIndex++) {
            List<DisplayedLine> lines = BookText.layout(pages.get(pageIndex), pageIndex == 0);
            out.append("    [\n");
            for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
                out.append("      ").append(jsonLine(lines.get(lineIndex)));
                if (lineIndex + 1 < lines.size()) {
                    out.append(',');
                }
                out.append('\n');
            }
            out.append("    ]");
            if (pageIndex + 1 < pages.size()) {
                out.append(',');
            }
            out.append('\n');
        }
        out.append("  ],\n");
        out.append("  \"notices\": [\n");
        List<String> notices = book.notices();
        for (int index = 0; index < notices.size(); index++) {
            out.append("    ").append(jsonString(notices.get(index)));
            if (index + 1 < notices.size()) {
                out.append(',');
            }
            out.append('\n');
        }
        out.append("  ]\n};\n");
        return out.toString();
    }

    private static void stringField(StringBuilder out, String name, String value) {
        out.append("  \"").append(name).append("\": ").append(jsonString(value)).append(",\n");
    }

    private static void numberField(StringBuilder out, String name, int value) {
        out.append("  \"").append(name).append("\": ").append(value).append(",\n");
    }

    private static String jsonLine(DisplayedLine line) {
        int color = line.bold() ? ExplorationBook.SERVER_NAME_COLOR : ExplorationBook.BODY_COLOR;
        StringBuilder out = new StringBuilder("{ \"text\": ").append(jsonString(line.text()));
        if (line.bold()) {
            out.append(", \"bold\": true");
        }
        out.append(", \"color\": \"").append(String.format("%06X", color)).append("\" }");
        return out.toString();
    }

    private static String jsonString(String value) {
        StringBuilder out = new StringBuilder("\"");
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (character < 0x20) {
                        out.append(String.format("\\u%04x", (int) character));
                    } else {
                        out.append(character);
                    }
                }
            }
        }
        out.append('"');
        return out.toString();
    }

    private static void extractTextures(Path jar, Path assetsDir) throws IOException {
        if (jar == null || !Files.isRegularFile(jar)) {
            boolean missing = !Files.isRegularFile(assetsDir.resolve("ascii.png"))
                    || !Files.isRegularFile(assetsDir.resolve("book.png"));
            if (missing) {
                System.out.println("No Minecraft client jar found. The preview will draw a stand-in page.");
                System.out.println("Pass -PminecraftJar=path\\to\\client.jar to copy ascii.png and book.png.");
            }
            return;
        }
        Files.createDirectories(assetsDir);
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            copyEntry(zip, ASCII_ENTRY, assetsDir.resolve("ascii.png"));
            copyEntry(zip, BOOK_ENTRY, assetsDir.resolve("book.png"));
            copyEntry(zip, FORWARD_ENTRY, assetsDir.resolve("page_forward.png"));
            copyEntry(zip, FORWARD_HOVER_ENTRY, assetsDir.resolve("page_forward_highlighted.png"));
            copyEntry(zip, BACK_ENTRY, assetsDir.resolve("page_backward.png"));
            copyEntry(zip, BACK_HOVER_ENTRY, assetsDir.resolve("page_backward_highlighted.png"));
        }
    }

    private static void copyEntry(ZipFile zip, String entryName, Path target) throws IOException {
        ZipEntry entry = zip.getEntry(entryName);
        if (entry == null) {
            System.out.println("Client jar has no " + entryName);
            return;
        }
        try (InputStream input = zip.getInputStream(entry)) {
            Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
        }
        System.out.println("Wrote " + target.toAbsolutePath());
    }

    private static Path findClientJar() throws IOException {
        String configured = System.getenv("EXPLORATION_MINECRAFT_JAR");
        if (configured != null && !configured.isBlank()) {
            return Path.of(configured);
        }
        String appData = System.getenv("APPDATA");
        if (appData == null) {
            return null;
        }
        Path versions = Path.of(appData, ".minecraft", "versions");
        if (!Files.isDirectory(versions)) {
            return null;
        }
        try (Stream<Path> jars = Files.walk(versions, 2)) {
            return jars
                    .filter(path -> path.getFileName().toString().endsWith(".jar"))
                    .max(Comparator.comparing(path -> {
                        try {
                            return Files.getLastModifiedTime(path);
                        } catch (IOException exception) {
                            return null;
                        }
                    }, Comparator.nullsFirst(Comparator.naturalOrder())))
                    .orElse(null);
        }
    }
}
