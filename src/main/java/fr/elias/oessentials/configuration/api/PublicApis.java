package fr.elias.oessentials.configuration.api;

import dev.oreo.modulith.core.ModuleApi;

import java.util.function.Supplier;

/**
 * Where modules publish their part of the public {@code fr.elias.oessentials.api} (IAfkAPI,
 * IKitsAPI, ...). The integrations module builds {@code OEssentialsAPI} from it without
 * depending on every module, so an optional module that fails only makes its own API null.
 */
@ModuleApi("services")
public interface PublicApis {
    /** Called while the providing module starts; the supplier may return {@code null}. */
    <T> void provide(Class<T> api, Supplier<? extends T> provider);

    void withdraw(Class<?> api);

    /** The current implementation, or {@code null} when its module is not running. */
    <T> T get(Class<T> api);
}
