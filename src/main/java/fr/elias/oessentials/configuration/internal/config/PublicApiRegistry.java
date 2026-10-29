package fr.elias.oessentials.configuration.internal.config;

import fr.elias.oessentials.configuration.api.PublicApis;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public final class PublicApiRegistry implements PublicApis {
    private final Map<Class<?>, Supplier<?>> providers = new ConcurrentHashMap<>();

    @Override
    public <T> void provide(Class<T> api, Supplier<? extends T> provider) {
        if (providers.putIfAbsent(api, provider) != null) {
            throw new IllegalStateException(api.getName() + " is already provided");
        }
    }

    @Override
    public void withdraw(Class<?> api) {
        providers.remove(api);
    }

    @Override
    public <T> T get(Class<T> api) {
        Supplier<?> provider = providers.get(api);
        return provider == null ? null : api.cast(provider.get());
    }
}
