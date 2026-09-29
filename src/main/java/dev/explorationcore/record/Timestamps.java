package dev.explorationcore.record;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public final class Timestamps {
    private Timestamps() {
    }

    public static String format(Instant instant) {
        return DateTimeFormatter.ISO_INSTANT.format(instant.truncatedTo(ChronoUnit.SECONDS));
    }
}
