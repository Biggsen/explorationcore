package dev.explorationcore.record;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DisplayNameCodecTest {
    @Test
    void decodesSpecExamples() {
        assertEquals("Sakonotur", DisplayNameCodec.decode("U2Frb25vdHVy"));
        assertEquals("Inner Core", DisplayNameCodec.decode("SW5uZXIgQ29yZQ"));
    }

    @Test
    void roundTripsCharactersThatWouldBreakAConsoleCommand() {
        String displayName = "Say \"hi\"; %player%";
        assertEquals(displayName, DisplayNameCodec.decode(encode(displayName)));
    }

    @Test
    void rejectsPaddedBlankAndGarbageTokens() {
        assertThrows(IllegalArgumentException.class, () -> DisplayNameCodec.decode("SW5uZXIgQ29yZQ=="));
        assertThrows(IllegalArgumentException.class, () -> DisplayNameCodec.decode(encode("   ")));
        assertThrows(IllegalArgumentException.class, () -> DisplayNameCodec.decode("not base64!!!"));
        assertThrows(IllegalArgumentException.class, () -> DisplayNameCodec.decode(""));
    }

    static String encode(String displayName) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(displayName.getBytes(StandardCharsets.UTF_8));
    }
}
