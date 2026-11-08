package fr.elias.oessentials.platform.modularity;

import com.google.gson.JsonParser;
import dev.oreo.modulith.core.*;
import fr.elias.oessentials.OEssentials;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class Modulith060IntegrationTest {
    private OEssentials plugin;
    private OreoModuleRegistry registry;
    private ModuleEnvironment environment;

    @BeforeEach
    void setup() {
        plugin = mock(OEssentials.class);
        registry = new OreoModuleRegistry();
        when(plugin.getModuleRegistry()).thenReturn(registry);
        when(plugin.isEnabled()).thenReturn(true);
        environment = ModuleEnvironment.of(plugin);
    }

    @Test
    void namedFactoriesIsolateApisAndCleanBothCompatibilityBindings() {
        ServiceKey<ProbeApi> east = new ServiceKey<>("probe.east", ProbeApi.class);
        ServiceKey<ProbeApi> west = new ServiceKey<>("probe.west", ProbeApi.class);
        try (var runtime = ModuleRuntime.builder().platformService(JavaPlugin.class, plugin)
                .bind(east.providerId(), Probe.class, environment, (env, ctx) -> env.create(Probe::new, ctx))
                .bind(west.providerId(), Probe.class, environment, (env, ctx) -> env.create(Probe::new, ctx))
                .bind("probe.consumer", Consumer.class, environment, (env, ctx) -> env.create(Consumer::new, ctx))
                .dependsOn("probe.consumer", "probe.east::services", "probe.west::services")
                .start()) {
            assertEquals("probe.east", registry.require(east).instance());
            assertEquals("probe.west", registry.require(west).instance());
            assertNotSame(registry.require(east), registry.require(west));
            assertThrows(IllegalStateException.class, () -> registry.require(ProbeApi.class));
            assertThrows(IllegalStateException.class, () -> registry.read(ProbeApi.class, ProbeApi::instance));
            assertEquals("probe", runtime.catalog().find("probe.east").orElseThrow().templateId());
            assertEquals("probe.east/probe.west", registry.require(ConsumerApi.class).providers());
        }
        assertNull(registry.read(east, ProbeApi::instance));
        assertNull(registry.read(west, ProbeApi::instance));
        assertNull(registry.read(ConsumerApi.class, ConsumerApi::providers));
    }

    @Test
    void publishingDoesNotBlockTheSchedulerWhileAStagedHandlerIsPending() {
        try (var runtime = ModuleRuntime.builder().platformService(JavaPlugin.class, plugin)
                .functor(Probe.class, environment, (env, ctx) -> env.create(Probe::new, ctx)).start()) {
            CompletableFuture<Void> handler = new CompletableFuture<>();
            try (var subscription = runtime.events().subscribeStage(String.class, "pending-handler",
                    EventDelivery.SYNC, ignored -> handler)) {
                var completion = assertTimeoutPreemptively(Duration.ofSeconds(2),
                        () -> registry.require(ProbeApi.class).emit("ready")).toCompletableFuture();
                assertFalse(completion.isDone(), "Scheduling is not handler completion");
                handler.complete(null);
                assertTrue(completion.join().successful());
            }
        }
    }

    @Test
    void factoryRejectsAnEnvironmentFromAnotherPlugin() {
        OEssentials other = mock(OEssentials.class);
        assertThrows(ModulithException.class, () -> ModuleRuntime.builder()
                .platformService(JavaPlugin.class, other)
                .functor(Probe.class, environment, (env, ctx) -> env.create(Probe::new, ctx)).start());
        assertNull(registry.read(ProbeApi.class, ProbeApi::instance));
    }

    @Test
    void reportsDescribeRuntimeInstanceIdentities(@TempDir Path directory) throws Exception {
        try (var runtime = ModuleRuntime.builder()
                .bind("probe.east", Probe.class, environment, (env, ctx) -> env.create(Probe::new, ctx))
                .bind("probe.west", Probe.class, environment, (env, ctx) -> env.create(Probe::new, ctx)).build()) {
            ModuleReports.write(runtime, directory);
            var report = JsonParser.parseString(Files.readString(directory.resolve("architecture.json"))).getAsJsonObject();
            assertEquals(1, report.get("schemaVersion").getAsInt());
            var modules = report.getAsJsonArray("modules");
            assertEquals(2, modules.size());
            assertEquals("probe.east", modules.get(0).getAsJsonObject().get("id").getAsString());
            assertEquals("probe", modules.get(0).getAsJsonObject().get("templateId").getAsString());
            assertTrue(Files.readString(directory.resolve("architecture.md")).contains("probe.west"));
            assertTrue(Files.readString(directory.resolve("modules.mmd")).contains("probe.east"));
        }
    }

    @ModuleApi("services")
    public interface ProbeApi {
        String instance();
        CompletionStage<EventDispatchResult> emit(Object event);
    }

    @PluginModule("probe")
    public static final class Probe extends ManagedModule implements ProbeApi {
        @Override protected void start() {}
        @Override public String instance() { return instanceIdentity().value(); }
        @Override public CompletionStage<EventDispatchResult> emit(Object event) { return publishEvent(event); }
    }

    @ModuleApi("services")
    public interface ConsumerApi { String providers(); }

    @PluginModule(value = "probe-consumer", uses = "probe::services")
    public static final class Consumer extends ManagedModule implements ConsumerApi {
        private String providers;
        @Override protected void start() {
            providers = services(new ServiceKey<>("probe.east", ProbeApi.class)).instance() + "/"
                    + services(new ServiceKey<>("probe.west", ProbeApi.class)).instance();
        }
        @Override public String providers() { return providers; }
    }
}
