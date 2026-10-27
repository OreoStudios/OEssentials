package fr.elias.oessentials.platform.modularity;

import dev.oreo.modulith.core.ApplicationModule;
import dev.oreo.modulith.core.ModuleBoundary;
import dev.oreo.modulith.core.PluginModule;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.TreeSet;

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
    /** Modules whose whole package hierarchy is enforced by the annotation processor. */
    static final Set<String> PROTECTED = Set.of(
            "discord-integration",
            "events",
            "help",
            "motd",
            "mute-system",
            "rules"
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
