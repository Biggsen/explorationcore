package dev.explorationcore.record;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TimestampsTest {
    @Test
    void formatsUtcSecondsWithAZoneSuffix() {
        Instant instant = Instant.parse("2026-09-26T17:31:42.125Z");
        assertEquals("2026-09-26T17:31:42Z", Timestamps.format(instant));
    }
}
