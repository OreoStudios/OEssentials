package fr.elias.oessentials.platform.modularity;

import dev.oreo.modulith.core.ModuleRuntime;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Runtime reports include named instances, unlike the compile-time module index. */
public final class ModuleReports {
    private ModuleReports() {}

    public static void write(ModuleRuntime runtime, Path directory) throws IOException {
        Files.createDirectories(directory);
        Files.writeString(directory.resolve("architecture.json"), runtime.architectureJson(), StandardCharsets.UTF_8);
        Files.writeString(directory.resolve("architecture.md"), runtime.architectureMarkdown(), StandardCharsets.UTF_8);
        Files.writeString(directory.resolve("modules.mmd"), runtime.graphMermaid(), StandardCharsets.UTF_8);
    }
}
