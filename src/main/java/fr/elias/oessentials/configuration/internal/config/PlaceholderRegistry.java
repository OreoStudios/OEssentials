package fr.elias.oessentials.configuration.internal.config;

import fr.elias.oessentials.configuration.api.Placeholders;
import org.bukkit.OfflinePlayer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class PlaceholderRegistry implements Placeholders {
    private final Map<String, Resolver> resolvers = new ConcurrentHashMap<>();

    @Override
    public void register(String owner, Resolver resolver) {
        if (resolvers.putIfAbsent(owner, resolver) != null) {
            throw new IllegalStateException(owner + " already registered placeholders");
        }
    }

    @Override
    public void unregister(String owner) {
        resolvers.remove(owner);
    }

    @Override
    public String resolve(OfflinePlayer player, String id, String idRaw) {
        for (Resolver resolver : resolvers.values()) {
            String value = resolver.resolve(player, id, idRaw);
            if (value != null) return value;
        }
        return null;
    }
}
