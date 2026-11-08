package fr.elias.oessentials.platform.modularity;

import dev.oreo.modulith.core.ServiceKey;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/** Bukkit API compatibility lookup, with the same instance-qualified keys as Modulith. */
public final class OreoModuleRegistry {
    private static final String LEGACY_OWNER = "legacy";
    private final Map<ServiceKey<?>, Object> services = new ConcurrentHashMap<>();

    /** Compatibility overload for callers that do not own a Modulith instance. */
    public <T> void bind(Class<T> api, T service) {
        bind(new ServiceKey<>(LEGACY_OWNER, api), service);
    }

    public <T> void bind(ServiceKey<T> key, T service) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(service, "service");
        if (services.putIfAbsent(key, key.contract().cast(service)) != null) {
            throw new IllegalStateException("Module API already bound: " + key);
        }
    }

    public <T> T require(ServiceKey<T> key) {
        T service = key.contract().cast(services.get(key));
        if (service == null) throw new IllegalStateException("Module API is not running: " + key);
        return service;
    }

    public <T> T require(Class<T> api) {
        T service = unique(api);
        if (service == null) throw new IllegalStateException("Module API is not running: " + api.getName());
        return service;
    }

    /** Legacy getters remain null while a feature is absent; ambiguity must be explicit. */
    public <T, R> R read(Class<T> api, Function<T, R> reader) {
        T service = unique(api);
        return service == null ? null : reader.apply(service);
    }

    public <T, R> R read(ServiceKey<T> key, Function<T, R> reader) {
        T service = key.contract().cast(services.get(key));
        return service == null ? null : reader.apply(service);
    }

    private <T> T unique(Class<T> api) {
        Objects.requireNonNull(api, "api");
        T result = null;
        for (var entry : services.entrySet()) {
            if (!entry.getKey().contract().equals(api)) continue;
            if (result != null) throw new IllegalStateException(
                    "Multiple module instances publish " + api.getName() + "; select a ServiceKey");
            result = api.cast(entry.getValue());
        }
        return result;
    }

    public void unbind(Class<?> api, Object service) {
        unbind(new ServiceKey<>(LEGACY_OWNER, api), service);
    }

    public void unbind(ServiceKey<?> key, Object service) { services.remove(key, service); }
    public void clear() { services.clear(); }
}
