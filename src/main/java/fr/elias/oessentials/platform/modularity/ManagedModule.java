package fr.elias.oessentials.platform.modularity;

import dev.oreo.modulith.core.MinecraftModule;
import dev.oreo.modulith.core.ModuleApi;
import dev.oreo.modulith.core.ModuleContext;
import fr.elias.oessentials.OEssentials;
import org.bukkit.plugin.java.JavaPlugin;
import fr.elias.oessentials.platform.scheduling.ModuleTaskScope;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.RegisteredListener;

/** Common lifecycle wiring; feature creation, configuration and cleanup live in each module. */
public abstract class ManagedModule implements MinecraftModule {
    protected OEssentials plugin;
    private ModuleContext context;
    private ModuleEnvironment environment;
    private dev.oreo.modulith.paper.PaperEventRoutes eventRoutes;

    final void configure(ModuleEnvironment environment) {
        if (this.context != null || this.environment != null) {
            throw new IllegalStateException("Module instance has already been configured");
        }
        this.environment = java.util.Objects.requireNonNull(environment);
    }
    private volatile boolean active;
    private ModuleTaskScope tasks;

    @Override
    public final void enable(ModuleContext context) {
        this.context = context;
        this.plugin = (OEssentials) context.platform(JavaPlugin.class);
        if (environment == null) environment = ModuleEnvironment.of(plugin);
        if (environment.plugin() != plugin) throw new IllegalStateException("Module belongs to another plugin");
        this.active = true;
        this.tasks = new ModuleTaskScope(plugin);
        Class<?> api = java.util.Arrays.stream(getClass().getInterfaces())
                .filter(type -> type.isAnnotationPresent(ModuleApi.class))
                .findFirst().orElseThrow(() -> new IllegalStateException("Module must expose a named service API"));
        publish(api);
        // Registered before start so resources from partially initialized modules are released too.
        context.lifecycle().onClose(() -> active = false);
        context.lifecycle().onClose(tasks::close);
        var existingListeners = new java.util.HashSet<>(HandlerList.getRegisteredListeners(plugin));
        try {
            tasks.run(this::start);
            if (!plugin.isEnabled()) throw new IllegalStateException("Plugin disabled during module startup");
        } catch (RuntimeException failure) {
            active = false;
            tasks.close();
            throw failure;
        } catch (LinkageError failure) {
            active = false;
            tasks.close();
            // The runtime rolls back RuntimeExceptions; optional plugin linkage can fail with Error.
            throw new IllegalStateException("Missing or incompatible dependency for " + context.moduleId(), failure);
        } finally {
            // Covers listeners registered by existing feature constructors as well as partial startup.
            var added = new java.util.ArrayList<>(HandlerList.getRegisteredListeners(plugin));
            added.removeAll(existingListeners);
            context.lifecycle().onClose(() -> {
                for (RegisteredListener listener : added) HandlerList.unregisterAll(listener.getListener());
            });
        }
    }

    private <T> void publish(Class<T> api) {
        T service = api.cast(this);
        context.services().publish(api, service);
        var key = new dev.oreo.modulith.core.ServiceKey<>(context.instanceIdentity().value(), api);
        environment.registry().bind(key, service);
        context.lifecycle().onClose(() -> environment.registry().unbind(key, service));
    }

    protected abstract void start();

    /** Opt in only for objects with @ModuleListener methods. Reflection on ordinary
     * service modules can otherwise load unavailable optional plugin API types. */
    protected final void listenForModuleEvents(Object listener) {
        context.listen(listener);
    }

    @Override
    public final void disable() {
        active = false;
        if (tasks != null) tasks.close();
    }

    protected final boolean isActive() { return active && plugin.isEnabled(); }

    protected final <T> T services(Class<T> api) { return context.services().require(api); }

    /** A routed listener may need this scheduler: never block it with publish(). */
    protected final java.util.concurrent.CompletionStage<dev.oreo.modulith.core.EventDispatchResult>
            publishEvent(Object event) {
        return context.events().publishAsync(event);
    }

    protected final <T> T services(dev.oreo.modulith.core.ServiceKey<T> key) {
        return context.services().require(key);
    }

    protected final dev.oreo.modulith.core.ModuleInstanceId instanceIdentity() {
        return context.instanceIdentity();
    }

    private dev.oreo.modulith.paper.PaperEventRoutes eventRoutes() {
        if (eventRoutes == null) {
            eventRoutes = context.platform(dev.oreo.modulith.paper.PaperPlatform.class).eventRoutes(context);
        }
        return eventRoutes;
    }

    private <E> java.util.function.Consumer<E> owned(java.util.function.Consumer<? super E> listener) {
        return event -> {
            if (isActive()) tasks.run(() -> listener.accept(event));
        };
    }

    /** Global coordination only; entity and world work needs an owning route. */
    protected final <E> void listenForGlobalEvent(Class<E> type, String id,
            java.util.function.Consumer<? super E> listener) {
        eventRoutes().global(type, id, owned(listener));
    }

    /** Capture the location on its owning thread before publishing the event. */
    protected final <E> void listenForRegionEvent(Class<E> type, String id,
            java.util.function.Function<? super E, org.bukkit.Location> target,
            java.util.function.Consumer<? super E> listener) {
        eventRoutes().region(type, id, target, owned(listener));
    }

    protected final <E> void listenForEntityEvent(Class<E> type, String id,
            java.util.function.Function<? super E, org.bukkit.entity.Entity> target,
            java.util.function.Consumer<? super E> listener) {
        eventRoutes().entity(type, id, target, owned(listener));
    }

    /** Background work must not read or mutate entity/region-owned state. */
    protected final <E> void listenForAsyncEvent(Class<E> type, String id,
            java.util.function.Consumer<? super E> listener) {
        eventRoutes().async(type, id, owned(listener));
    }

    /** Answers %oreo_...% placeholders for this module until it stops. */
    protected final void registerPlaceholders(fr.elias.oessentials.configuration.api.Placeholders.Resolver resolver) {
        var placeholders = services(fr.elias.oessentials.configuration.lifecycle.ConfigurationServices.class).getPlaceholders();
        String owner = context.moduleId();
        placeholders.register(owner, resolver);
        context.lifecycle().onClose(() -> placeholders.unregister(owner));
    }

    /** Declares that this module reloads {@code fileName} (e.g. when the web panel pushes it). */
    protected final void registerConfigFile(String fileName, Runnable reload) {
        var files = services(fr.elias.oessentials.configuration.lifecycle.ConfigurationServices.class).getConfigFiles();
        files.register(fileName, reload);
        context.lifecycle().onClose(() -> files.unregister(fileName));
    }

    /** Adds a step to /oereload until the module stops; lower {@code order} runs first. */
    protected final void registerReloadStep(int order, String name, fr.elias.oessentials.configuration.api.ReloadSteps.Step step) {
        var steps = services(fr.elias.oessentials.configuration.lifecycle.ConfigurationServices.class).getReloadSteps();
        steps.register(order, name, step);
        context.lifecycle().onClose(() -> steps.unregister(name));
    }

    /** Publishes this module's part of the public OEssentials API until the module stops. */
    protected final <T> void providePublicApi(Class<T> api, java.util.function.Supplier<? extends T> provider) {
        var apis = services(fr.elias.oessentials.configuration.lifecycle.ConfigurationServices.class).getPublicApis();
        apis.provide(api, provider);
        context.lifecycle().onClose(() -> apis.withdraw(api));
    }

    protected final void cleanup(String resource, Cleanup action) {
        context.lifecycle().onClose(() -> {
            try {
                action.close();
            } catch (Exception failure) {
                throw new IllegalStateException("Could not close " + context.moduleId() + "/" + resource, failure);
            }
        });
    }

    @FunctionalInterface
    protected interface Cleanup { void close() throws Exception; }
}
