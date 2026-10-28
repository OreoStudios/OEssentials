package fr.elias.oessentials.platform.modularity;

import dev.oreo.modulith.core.PluginModule;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class FeaturePackageLayoutTest {
    private static final String BASE = "fr.elias.oessentials";
    private static final Path ROOT = Path.of("src/main/java/fr/elias/oessentials");
    /** {@code api} holds a feature's published contracts; the other layers are its implementation. */
    private static final Set<String> FEATURE_LAYERS =
            Set.of("api", "domain", "application", "infra", "internal", "lifecycle");

    @Test
    void everyLifecycleBelongsToOneDocumentedFeaturePackage() throws Exception {
        var featurePackages = new HashSet<String>();
        for (var lifecycle : OreoModules.TYPES) {
            assertTrue(lifecycle.getPackageName().endsWith(".lifecycle"), lifecycle.getName());
            String feature = lifecycle.getPackageName().replaceFirst("\\.lifecycle$", "");
            assertTrue(featurePackages.add(feature), feature);
            assertEquals(1, feature.substring(BASE.length() + 1).split("\\.").length);
            var descriptor = Class.forName(feature + ".package-info").getPackage().getAnnotation(FeaturePackage.class);
            assertNotNull(descriptor, feature + " must declare @FeaturePackage in package-info.java");
            assertEquals(lifecycle, descriptor.lifecycle());
            assertEquals(lifecycle.getAnnotation(PluginModule.class).value(), descriptor.id());
        }
        try (var directories = Files.list(ROOT)) {
            var declared = directories.filter(Files::isDirectory)
                    .filter(path -> Files.exists(path.resolve("package-info.java")))
                    .filter(path -> !Set.of("api", "platform", "shared").contains(path.getFileName().toString()))
                    .map(path -> BASE + "." + path.getFileName()).collect(java.util.stream.Collectors.toSet());
            assertEquals(featurePackages, declared, "No orphan package descriptors or lifecycle modules");
        }
    }

    @Test
    void productionSourcesUseFeatureOrInfrastructurePackages() throws Exception {
        var allowed = new HashSet<>(Set.of("api", "platform", "shared"));
        OreoModules.TYPES.forEach(type -> allowed.add(type.getPackageName().substring(BASE.length() + 1).split("\\.")[0]));
        try (var files = Files.walk(ROOT)) {
            for (var source : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                Path relative = ROOT.relativize(source);
                if (relative.getNameCount() == 1) {
                    assertEquals("OEssentials.java", relative.toString(), "Only the plugin entry point belongs at the root");
                    continue;
                }
                assertTrue(allowed.contains(relative.getName(0).toString()), "Unowned source: " + relative);
                if (!Set.of("api", "platform", "shared").contains(relative.getName(0).toString()) && relative.getNameCount() > 2) {
                    assertTrue(FEATURE_LAYERS.contains(relative.getName(1).toString()), "Unexpected feature layout: " + relative);
                }
            }
        }
    }
}
