package fr.elias.oessentials.economy.lifecycle;

import dev.oreo.modulith.core.ModuleApi;
import fr.elias.oessentials.economy.internal.persistence.database.PlayerEconomyDatabase;
import fr.elias.oessentials.economy.internal.EconomyBootstrap;
import fr.elias.oessentials.economy.internal.offline.OfflinePlayerCache;
import net.milkbowl.vault.economy.Economy;

@ModuleApi("services")
public interface EconomyServices {
    EconomyBootstrap getEcoBootstrap();
    PlayerEconomyDatabase getDatabase();
    OfflinePlayerCache getOfflinePlayerCache();
    /** Vault's economy provider, or {@code null}; untyped because Vault is optional. */
    Object vaultEconomyProvider();

    /** Vault's economy, or {@code null}. Only call when Vault is installed. */
    default Economy getVaultEconomy() {
        return (Economy) vaultEconomyProvider();
    }

    /** The public OEssentials API for this module, or {@code null} while it is not available. */
    default fr.elias.oessentials.api.IEconomyAPI getEconomyApi() {
        var eco = getEcoBootstrap();
        if (eco == null) return null;
        var svc = eco.api();
        return svc == null ? null : new fr.elias.oessentials.economy.internal.api.EconomyAPIImpl(svc);
    }

    /** Names of players seen on the network, for tab completion (empty while the cache is off). */
    default java.util.List<String> offlinePlayerNames() {
        var cache = getOfflinePlayerCache();
        return cache == null ? java.util.List.of() : cache.getNames();
    }

    /** The UUID of a player seen on the network, or {@code null}. */
    default java.util.UUID offlinePlayerId(String name) {
        var cache = getOfflinePlayerCache();
        return cache == null ? null : cache.getId(name);
    }

    /** Balance in the economy database (0 when the economy has no database). */
    default double storedBalance(java.util.UUID playerId) {
        var db = getDatabase();
        return db == null ? 0.0 : db.getBalance(playerId);
    }
}
