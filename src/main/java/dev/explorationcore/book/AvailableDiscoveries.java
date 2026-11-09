package dev.explorationcore.book;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

/**
 * Cached {@code %explorationcore_total_available%} value. The file is read again
 * only after its size or modification time changes.
 */
public final class AvailableDiscoveries {
    private final Path file;
    private final Logger logger;
    private boolean observed;
    private long modified = Long.MIN_VALUE;
    private long size = Long.MIN_VALUE;
    private String value = "";
    private String failure;

    public AvailableDiscoveries(Path file, Logger logger) {
        this.file = file;
        this.logger = logger;
    }

    public synchronized String value() {
        long nextModified;
        long nextSize;
        try {
            if (!Files.isRegularFile(file)) {
                return retain("", Long.MIN_VALUE, -1L, "totals.yml is missing");
            }
            nextModified = Files.getLastModifiedTime(file).toMillis();
            nextSize = Files.size(file);
        } catch (IOException exception) {
            return retain("", Long.MIN_VALUE, -2L, "totals.yml could not be read");
        }
        if (observed && nextModified == modified && nextSize == size) {
            return value;
        }
        try {
            return retain(Integer.toString(TotalsFile.load(file).available()), nextModified, nextSize, null);
        } catch (IOException | TotalsException exception) {
            String message = exception.getMessage();
            if (message == null || message.isBlank()) {
                message = "totals.yml could not be read";
            }
            return retain("", nextModified, nextSize, message);
        }
    }

    private String retain(String next, long nextModified, long nextSize, String problem) {
        boolean repeated = observed && problem != null && problem.equals(failure);
        observed = true;
        modified = nextModified;
        size = nextSize;
        value = next;
        if (problem == null) {
            failure = null;
            return value;
        }
        failure = problem;
        if (!repeated) {
            logger.warning("total_available is unavailable: " + problem);
        }
        return value;
    }
}
