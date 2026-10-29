package fr.elias.oessentials.configuration.api;

import dev.oreo.modulith.core.ModuleApi;
import org.bukkit.OfflinePlayer;

/** %oreo_...% placeholders answered by the module that owns the data. */
@ModuleApi("services")
public interface Placeholders {
    @ModuleApi("services")
    @FunctionalInterface
    interface Resolver {
        /**
         * @param id    the lower-cased identifier without {@code oreo_}
         * @param idRaw the identifier as typed
         * @return the value, or {@code null} when this resolver does not handle {@code id}
         */
        String resolve(OfflinePlayer player, String id, String idRaw);
    }

    void register(String owner, Resolver resolver);

    void unregister(String owner);

    /** The first non-null answer of the registered resolvers, or {@code null}. */
    String resolve(OfflinePlayer player, String id, String idRaw);
}
