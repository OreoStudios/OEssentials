package fr.elias.oessentials.storage.api;

import dev.oreo.modulith.core.ModuleApi;

import java.util.UUID;

/**
 * Where the CMI, EssentialsX and zEssentials importers write player balances.
 *
 * <p>The economy module starts after storage, so it registers its database through
 * {@link fr.elias.oessentials.storage.lifecycle.StorageServices#setBalanceImportTarget}
 * instead of storage depending on economy.</p>
 */
@ModuleApi("services")
public interface BalanceImportTarget {
    double getBalance(UUID playerId);

    void setBalance(UUID playerId, String playerName, double balance);
}
