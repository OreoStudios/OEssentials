package fr.elias.oessentials.platform.modularity;

import dev.oreo.modulith.core.ModuleApi;
import dev.oreo.modulith.core.ModuleDescriptor;
import dev.oreo.modulith.core.ModuleRuntime;
import dev.oreo.modulith.test.ModuleAssertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class OreoModuleArchitectureTest {
    @Test
    void explicitCatalogIncludesEveryCompileTimeDiscoveredModule() throws Exception {
        try (var stream = getClass().getResourceAsStream("/META-INF/minecraft-modulith/modules.idx")) {
            assertNotNull(stream, "MinecraftModulith annotation processor must run during compilation");
            var indexed = new java.io.BufferedReader(new java.io.InputStreamReader(stream, java.nio.charset.StandardCharsets.UTF_8))
                    .lines().filter(line -> !line.isBlank()).map(line -> line.split("\\|")[1])
                    .collect(java.util.stream.Collectors.toSet());
            assertEquals(OreoModules.TYPES.stream().map(Class::getName).collect(java.util.stream.Collectors.toSet()), indexed);
        }
    }

    @Test
    void completePluginGraphHasNoMissingDependenciesOrCycles() {
        ModuleAssertions.assertValidArchitecture(OreoModules.TYPES);
        var runtime = ModuleRuntime.builder().modules(OreoModules.TYPES).build();
        assertEquals(56, runtime.modules().size());
        List<String> order = runtime.modules().stream().map(ModuleDescriptor::id).toList();
        assertEquals("configuration", order.getFirst());
        assertEquals("integrations", order.getLast());
        for (String consumer : List.of("economy", "chat", "inventory-sync", "player-vaults", "tab")) {
            assertTrue(order.indexOf("storage") < order.indexOf(consumer), consumer + " must stop before storage");
        }
        // Messaging is the transport: modules register packets with it, so it starts before them
        // (PacketManager stops dispatching as soon as the plugin starts disabling).
        for (String user : List.of("storage", "economy", "player-services", "trade", "inventory-sync", "mod-gui")) {
            assertTrue(order.indexOf("messaging") < order.indexOf(user), user + " must start after messaging");
        }
        assertEquals(List.of("configuration"), runtime.modules().get(order.indexOf("messaging")).dependencies().stream()
                .map(dependency -> dependency.split("::")[0]).toList(), "messaging depends only on configuration");
        ModuleAssertions.assertMermaidContains(runtime, "messaging");
    }

    @Test
    void eachFeatureHasItsOwnPackageAndOnePublicNamedServiceApi() {
        var packages = new HashSet<String>();
        for (var module : OreoModules.TYPES) {
            assertTrue(packages.add(module.getPackageName()), "Two module boundaries share " + module.getPackageName());
            var apis = Arrays.stream(module.getInterfaces()).filter(type -> type.isAnnotationPresent(ModuleApi.class)).toList();
            assertEquals(1, apis.size(), module.getName());
            assertEquals("services", apis.getFirst().getAnnotation(ModuleApi.class).value());
            assertTrue(java.lang.reflect.Modifier.isPublic(apis.getFirst().getModifiers()));
            assertDoesNotThrow(() -> module.getDeclaredConstructor());
        }
    }
}
