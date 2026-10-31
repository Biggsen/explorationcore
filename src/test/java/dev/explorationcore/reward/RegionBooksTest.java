package dev.explorationcore.reward;

import dev.explorationcore.record.AttemptResult;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegionBooksTest {
    private static final String COMMAND = "lp user {player} permission set bookgui.book.{region} true";
    private static final Logger LOGGER = quietLogger();

    @Test
    void shippedConfigKeepsDeliveryOff() throws IOException {
        RegionBooks books = loadResource("config.yml");

        assertFalse(books.enabled());
        assertEquals(COMMAND, books.configuredCommand());
        assertEquals(Optional.empty(), books.command("overworld", "region", "auronia", "Steve"));
    }

    @Test
    void enabledConfigSubstitutesTheRegionAndTheRecordedName() {
        RegionBooks books = enabled();

        assertTrue(books.enabled());
        assertEquals(
                Optional.of("lp user Steve permission set bookgui.book.auronia true"),
                books.command("overworld", "region", "Auronia", "Steve")
        );
        assertEquals(
                Optional.of("lp user Steve permission set bookgui.book.ash_barrow true"),
                books.command("overworld", "region", "ash_barrow", "Steve")
        );
    }

    @Test
    void netherRegionsAndOtherKindsPayNothing() {
        RegionBooks books = enabled();

        assertEquals(Optional.empty(), books.command("nether", "region", "ashbarrow", "Steve"));
        assertEquals(Optional.empty(), books.command("overworld", "village", "auronia", "Steve"));
        assertEquals(Optional.empty(), books.command("overworld", "heart", "auronia", "Steve"));
        assertEquals(Optional.empty(), books.command("overworld", "nerve", "auronia", "Steve"));
        assertEquals(Optional.empty(), books.command("overworld", "structure", "auronia", "Steve"));
    }

    @Test
    void recordedNameStillProducesTheCommandAfterDisconnect() {
        RegionBooks books = enabled();
        AttemptResult.Recorded recorded = new AttemptResult.Recorded(
                UUID.fromString("6f1b1c2a-7d4e-4f8a-9b0c-1d2e3f4a5b6c"),
                "Steve",
                "overworld",
                "region",
                "auronia",
                "Auronia",
                null,
                false
        );

        assertEquals(
                Optional.of("lp user Steve permission set bookgui.book.auronia true"),
                books.command(recorded.world(), recorded.entityType(), recorded.entityId(), recorded.playerName())
        );
    }

    @Test
    void unsafeNameOrRegionPaysNothing() {
        RegionBooks books = enabled();

        assertEquals(Optional.empty(), books.command("overworld", "region", "auronia", "Steve Two"));
        assertEquals(Optional.empty(), books.command("overworld", "region", "auronia", " "));
        assertEquals(Optional.empty(), books.command("overworld", "region", "auronia", ""));
        assertEquals(Optional.empty(), books.command("overworld", "region", "auronia", null));
        assertEquals(Optional.empty(), books.command("overworld", "region", "auro nia", "Steve"));
        assertEquals(Optional.empty(), books.command("overworld", "region", "auronia;op", "Steve"));
        assertEquals(Optional.empty(), books.command("overworld", "region", "book.auronia", "Steve"));
        assertEquals(Optional.empty(), books.command("overworld", "region", "", "Steve"));
        assertEquals(Optional.empty(), books.command("overworld", "region", null, "Steve"));
    }

    @Test
    void disabledConfigPaysNothing() {
        RegionBooks books = load("""
                region-books:
                  enabled: false
                  command: "lp user {player} permission set bookgui.book.{region} true"
                """);

        assertFalse(books.enabled());
        assertEquals(COMMAND, books.configuredCommand());
        assertEquals(Optional.empty(), books.command("overworld", "region", "auronia", "Steve"));
    }

    @Test
    void missingSectionPaysNothing() {
        RegionBooks books = load("server-name: Lowothra\n");

        assertFalse(books.enabled());
        assertEquals("", books.configuredCommand());
        assertEquals(Optional.empty(), books.command("overworld", "region", "auronia", "Steve"));
    }

    @Test
    void malformedCommandDisablesGrants() {
        RegionBooks missing = load("""
                region-books:
                  enabled: true
                """);
        RegionBooks blank = load("""
                region-books:
                  enabled: true
                  command: "  "
                """);
        RegionBooks slash = load("""
                region-books:
                  enabled: true
                  command: "/lp user {player} permission set bookgui.book.{region} true"
                """);
        RegionBooks playerOnly = load("""
                region-books:
                  enabled: true
                  command: "lp user {player} permission set bookgui.book.auronia true"
                """);
        RegionBooks regionOnly = load("""
                region-books:
                  enabled: true
                  command: "lp permission set bookgui.book.{region} true"
                """);
        RegionBooks shape = load("""
                region-books: "lp user {player} permission set bookgui.book.{region} true"
                """);

        assertFalse(missing.enabled());
        assertEquals("", missing.configuredCommand());
        assertEquals(Optional.empty(), missing.command("overworld", "region", "auronia", "Steve"));
        assertFalse(blank.enabled());
        assertEquals("", blank.configuredCommand());
        assertFalse(slash.enabled());
        assertEquals("", slash.configuredCommand());
        assertFalse(playerOnly.enabled());
        assertEquals(Optional.empty(), playerOnly.command("overworld", "region", "auronia", "Steve"));
        assertFalse(regionOnly.enabled());
        assertEquals(Optional.empty(), regionOnly.command("overworld", "region", "auronia", "Steve"));
        assertFalse(shape.enabled());
        assertEquals(Optional.empty(), shape.command("overworld", "region", "auronia", "Steve"));
    }

    private static RegionBooks enabled() {
        return load("""
                region-books:
                  enabled: true
                  command: "lp user {player} permission set bookgui.book.{region} true"
                """);
    }

    private static RegionBooks loadResource(String name) throws IOException {
        try (InputStream in = RegionBooksTest.class.getClassLoader().getResourceAsStream(name)) {
            if (in == null) {
                throw new IOException("missing resource " + name);
            }
            return load(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    private static RegionBooks load(String yaml) {
        YamlConfiguration configuration = new YamlConfiguration();
        try {
            configuration.loadFromString(yaml);
        } catch (InvalidConfigurationException exception) {
            throw new AssertionError(exception);
        }
        return RegionBooks.load(configuration, LOGGER);
    }

    private static Logger quietLogger() {
        Logger logger = Logger.getLogger("region-books-test");
        logger.setUseParentHandlers(false);
        return logger;
    }
}
