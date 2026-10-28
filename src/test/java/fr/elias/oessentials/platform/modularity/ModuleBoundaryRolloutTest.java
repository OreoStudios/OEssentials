package fr.elias.oessentials.platform.modularity;

import dev.oreo.modulith.core.ApplicationModule;
import dev.oreo.modulith.core.ModuleBoundary;
import dev.oreo.modulith.core.PluginModule;
import dev.oreo.modulith.core.ModuleApi;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tracks the progressive rollout of strict MinecraftModulith boundaries.
 *
 * <p>A module is protected when its feature package-info.java declares {@code @ApplicationModule}.
 * The annotation processor then rejects any other module that references its {@code internal}
 * packages or non-{@code @ModuleApi} types. {@link #PROTECTED} only grows: removing a module from
 * it, or dropping its annotation, fails this test. See docs/architecture/README.md.</p>
 */
class ModuleBoundaryRolloutTest {
    private static final Path SOURCES = Path.of("src/main/java");

    /** Modules whose whole package hierarchy is enforced by the annotation processor. */
    static final Set<String> PROTECTED = Set.of(
            "afk",
            "boss-bar",
            "clear-lag",
            "command-toggle",
            "commands",
            "configuration",
            "custom-crafting",
            "custom-worlds",
            "daily",
            "discord-integration",
            "ender-chest",
            "events",
            "freeze",
            "group-rtp",
            "help",
            "holograms",
            "ignore",
            "jails",
            "jump-pads",
            "mail",
            "maintenance",
            "messaging",
            "mobs",
            "motd",
            "mute-system",
            "performance-guards",
            "portals",
            "punishment-logger",
            "rules",
            "scoreboard",
            "sharding",
            "storage",
            "tab",
            "warnings",
            "web-panel"
    );

    @Test
    void protectedModulesDeclareTheirBoundary() {
        var actual = new TreeSet<String>();
        for (var lifecycle : OreoModules.TYPES) {
            String id = lifecycle.getAnnotation(PluginModule.class).value();
            var declared = boundary(lifecycle);
            if (declared != null) {
                actual.add(id);
            }
        }
        assertEquals(new TreeSet<>(PROTECTED), actual,
                "Update PROTECTED when a module is migrated; protected modules must never lose @ApplicationModule");
    }

    @Test
    void boundaryMatchesFeaturePackageAndLifecycle() {
        for (var lifecycle : OreoModules.TYPES) {
            ApplicationModule declared = boundary(lifecycle);
            if (declared == null) {
                continue;
            }
            String id = lifecycle.getAnnotation(PluginModule.class).value();
            String feature = lifecycle.getPackageName().replaceFirst("\\.lifecycle$", "");
            assertEquals(id, declared.id(), lifecycle.getName());
            assertEquals(feature, ModuleBoundary.rootPackage(lifecycle),
                    id + " must own its whole feature package, not only " + lifecycle.getPackageName());
            assertEquals(0, declared.allowedDependencies().length,
                    id + ": declare dependencies on @PluginModule so they stay in one place");
            assertEquals(id, featureId(feature), id + " @FeaturePackage id");
        }
    }

    /** A feature's api package is its published contract: every type is a @ModuleApi that never exposes implementation. */
    @Test
    void featureApiPackagesOnlyPublishContracts() throws Exception {
        var problems = new ArrayList<String>();
        for (var lifecycle : OreoModules.TYPES) {
            String feature = lifecycle.getPackageName().replaceFirst("\\.lifecycle$", "");
            Path api = SOURCES.resolve(feature.replace('.', '/')).resolve("api");
            if (!Files.isDirectory(api)) {
                continue;
            }
            try (Stream<Path> files = Files.list(api)) {
                for (Path file : files.filter(f -> f.toString().endsWith(".java")).toList()) {
                    String name = file.getFileName().toString().replace(".java", "");
                    Class<?> type = Class.forName(feature + ".api." + name);
                    if (!type.isAnnotationPresent(ModuleApi.class)) {
                        problems.add(type.getName() + " is not @ModuleApi");
                    }
                    for (Method method : type.getMethods()) {
                        for (Class<?> used : signature(method)) {
                            if (!used.getPackageName().startsWith(feature + ".")) continue;
                            if (!used.getPackageName().equals(feature + ".api")) {
                                problems.add(type.getName() + "#" + method.getName() + " exposes " + used.getName());
                            }
                        }
                    }
                }
            }
        }
        assertEquals(List.of(), problems);
    }

    private static List<Class<?>> signature(Method method) {
        var types = new ArrayList<Class<?>>(List.of(method.getParameterTypes()));
        types.add(method.getReturnType());
        return types;
    }

    private static ApplicationModule boundary(Class<?> lifecycle) {
        return ModuleBoundary.declaration(lifecycle).map(ModuleBoundary.Declaration::annotation).orElse(null);
    }

    private static String featureId(String feature) {
        try {
            return Class.forName(feature + ".package-info").getAnnotation(FeaturePackage.class).id();
        } catch (ClassNotFoundException e) {
            return fail(feature + " has no package-info.java");
        }
    }
}
