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
    Economy getVaultEconomy();

    /** The public OEssentials API for this module, or {@code null} while it is not available. */
    default fr.elias.oessentials.api.IEconomyAPI getEconomyApi() {
        var eco = getEcoBootstrap();
        if (eco == null) return null;
        var svc = eco.api();
        return svc == null ? null : new fr.elias.oessentials.economy.internal.api.EconomyAPIImpl(svc);
    }
}
