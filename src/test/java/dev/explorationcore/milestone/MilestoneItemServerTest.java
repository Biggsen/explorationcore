package dev.explorationcore.milestone;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MilestoneItemServerTest {
    private static final String PAPER_URL = "https://fill-data.papermc.io/v1/objects/"
            + "5ffef465eeeb5f2a3c23a24419d97c51afd7dbb4923ff42df9a3f58bba1ccfba/paper-1.21.11-132.jar";
    private static final String PAPER_SHA256 = "5ffef465eeeb5f2a3c23a24419d97c51afd7dbb4923ff42df9a3f58bba1ccfba";

    @Test
    @Timeout(value = 5, unit = TimeUnit.MINUTES)
    void registryRejectsBadBooksAndItemsCarryTheirMeta() throws Exception {
        Path root = Path.of("build");
        Path paper = paperJar(root.resolve("paper-server"));
        Path plugin = pluginJar(root.resolve("libs"));
        Path probe = root.resolve("probe").resolve("milestone-item-probe.jar");
        assertTrue(Files.isRegularFile(probe), "missing " + probe.toAbsolutePath());

        Path server = root.resolve("milestone-item-server");
        Path plugins = server.resolve("plugins");
        Path result = root.resolve("milestone-item-result.txt");
        Files.createDirectories(plugins);
        deleteChildren(plugins);
        Files.copy(plugin, plugins.resolve(plugin.getFileName()));
        Files.copy(probe, plugins.resolve(probe.getFileName()));
        Files.writeString(server.resolve("eula.txt"), "eula=true\n");
        Files.writeString(server.resolve("server.properties"), serverProperties(freePort()));
        Files.writeString(server.resolve("milestone-item-result.path"), result.toAbsolutePath().toString());
        Files.deleteIfExists(result);

        Path log = server.resolve("server.log");
        Path java = javaExecutable();
        Process process = new ProcessBuilder(
                java.toString(),
                "-Xms512M",
                "-Xmx512M",
                "-jar",
                paper.toAbsolutePath().toString(),
                "--nogui"
        )
                .directory(server.toFile())
                .redirectErrorStream(true)
                .redirectOutput(ProcessBuilder.Redirect.to(log.toFile()))
                .start();
        try {
            long deadline = System.nanoTime() + Duration.ofMinutes(4).toNanos();
            while (System.nanoTime() < deadline && !Files.isRegularFile(result)) {
                if (!process.isAlive()) {
                    break;
                }
                Thread.sleep(500);
            }
            assertTrue(Files.isRegularFile(result), "server produced no result\n" + tail(log));
            String report = Files.readString(result);
            assertTrue(report.contains("status=ok\n"), report + "\n" + tail(log));
        } finally {
            process.destroy();
            if (!process.waitFor(20, TimeUnit.SECONDS)) {
                process.destroyForcibly();
            }
        }
    }

    private static Path paperJar(Path directory) throws IOException, InterruptedException, NoSuchAlgorithmException {
        Files.createDirectories(directory);
        Path jar = directory.resolve("paper-1.21.11-132.jar");
        if (Files.isRegularFile(jar) && PAPER_SHA256.equals(sha256(jar))) {
            return jar;
        }
        Files.deleteIfExists(jar);
        HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
        HttpRequest request = HttpRequest.newBuilder(URI.create(PAPER_URL)).build();
        HttpResponse<Path> response = client.send(request, HttpResponse.BodyHandlers.ofFile(jar));
        if (response.statusCode() != 200) {
            throw new IOException("paper download status " + response.statusCode());
        }
        String hash = sha256(jar);
        if (!PAPER_SHA256.equals(hash)) {
            throw new IOException("paper jar hash " + hash);
        }
        return jar;
    }

    private static Path pluginJar(Path directory) throws IOException {
        try (DirectoryStream<Path> jars = Files.newDirectoryStream(directory, "ExplorationCore-*.jar")) {
            Path found = null;
            FileTime newest = null;
            for (Path jar : jars) {
                FileTime modified = Files.getLastModifiedTime(jar);
                if (newest == null || modified.compareTo(newest) > 0) {
                    newest = modified;
                    found = jar;
                }
            }
            if (found == null) {
                throw new IOException("missing plugin jar in " + directory);
            }
            return found;
        }
    }

    private static int freePort() throws IOException {
        try (java.net.ServerSocket socket = new java.net.ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private static String serverProperties(int port) {
        return """
                online-mode=false
                server-port=%d
                enable-query=false
                spawn-monsters=false
                spawn-animals=false
                spawn-npcs=false
                generate-structures=false
                allow-nether=false
                level-type=minecraft:flat
                view-distance=2
                simulation-distance=2
                max-players=1
                motd=milestone-item-test
                spawn-protection=0
                sync-chunk-writes=false
                """.formatted(port);
    }

    private static Path javaExecutable() {
        Path windows = Path.of(System.getProperty("java.home"), "bin", "java.exe");
        if (Files.isRegularFile(windows)) {
            return windows;
        }
        return Path.of(System.getProperty("java.home"), "bin", "java");
    }

    private static void deleteChildren(Path directory) throws IOException {
        try (Stream<Path> children = Files.list(directory)) {
            for (Path child : children.toList()) {
                if (Files.isDirectory(child)) {
                    try (Stream<Path> walk = Files.walk(child)) {
                        for (Path path : walk.sorted(Comparator.reverseOrder()).toList()) {
                            Files.deleteIfExists(path);
                        }
                    }
                } else {
                    Files.deleteIfExists(child);
                }
            }
        }
    }

    private static String sha256(Path file) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream input = Files.newInputStream(file)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                digest.update(buffer, 0, read);
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static String tail(Path log) throws IOException {
        if (!Files.isRegularFile(log)) {
            return "(no server log)";
        }
        String text = Files.readString(log);
        int length = text.length();
        return length <= 4000 ? text : text.substring(length - 4000);
    }
}
