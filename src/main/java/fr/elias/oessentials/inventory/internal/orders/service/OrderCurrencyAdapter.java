package fr.elias.oessentials.inventory.internal.orders.service;

import fr.elias.oessentials.OEssentials;
import fr.elias.oessentials.currency.api.Currencies;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Unified currency adapter that wraps either Vault or OEssentials custom currencies.
 * currencyId == null → use Vault economy.
 */
public final class OrderCurrencyAdapter {

    private final OEssentials plugin;

    public OrderCurrencyAdapter(OEssentials plugin) {
        this.plugin = plugin;
    }


    public CompletableFuture<Double> getBalance(UUID playerId, String currencyId) {
        if (currencyId == null) {
            return CompletableFuture.supplyAsync(() -> {
                Economy eco = vaultEco();
                if (eco == null) return 0.0;
                return eco.getBalance(Bukkit.getOfflinePlayer(playerId));
            });
        }
        Currencies cs = plugin.getModuleRegistry().read(fr.elias.oessentials.currency.lifecycle.CurrencySystemServices.class, fr.elias.oessentials.currency.lifecycle.CurrencySystemServices::getCurrencies);
        if (cs == null) return CompletableFuture.completedFuture(0.0);
        return cs.getBalance(playerId, currencyId);
    }


    /**
     * Synchronously withdraws from a player (must be called off-thread or in a CF chain).
     * Returns true on success.
     */
    public CompletableFuture<Boolean> withdraw(UUID playerId, String currencyId, double amount) {
        if (amount <= 0) return CompletableFuture.completedFuture(true);

        if (currencyId == null) {
            return CompletableFuture.supplyAsync(() -> {
                Economy eco = vaultEco();
                if (eco == null) return false;
                OfflinePlayer op = Bukkit.getOfflinePlayer(playerId);
                if (!eco.has(op, amount)) return false;
                return eco.withdrawPlayer(op, amount).transactionSuccess();
            });
        }
        Currencies cs = plugin.getModuleRegistry().read(fr.elias.oessentials.currency.lifecycle.CurrencySystemServices.class, fr.elias.oessentials.currency.lifecycle.CurrencySystemServices::getCurrencies);
        if (cs == null) return CompletableFuture.completedFuture(false);
        return cs.withdraw(playerId, currencyId, amount);
    }


    public CompletableFuture<Boolean> deposit(UUID playerId, String currencyId, double amount) {
        if (amount <= 0) return CompletableFuture.completedFuture(true);

        if (currencyId == null) {
            return CompletableFuture.supplyAsync(() -> {
                Economy eco = vaultEco();
                if (eco == null) return false;
                return eco.depositPlayer(Bukkit.getOfflinePlayer(playerId), amount).transactionSuccess();
            });
        }
        Currencies cs = plugin.getModuleRegistry().read(fr.elias.oessentials.currency.lifecycle.CurrencySystemServices.class, fr.elias.oessentials.currency.lifecycle.CurrencySystemServices::getCurrencies);
        if (cs == null) return CompletableFuture.completedFuture(false);
        return cs.deposit(playerId, currencyId, amount);
    }


    public String format(String currencyId, double amount) {
        if (currencyId == null) {
            Economy eco = vaultEco();
            return eco != null ? eco.format(amount) : String.format("$%.2f", amount);
        }
        Currencies cs = plugin.getModuleRegistry().read(fr.elias.oessentials.currency.lifecycle.CurrencySystemServices.class, fr.elias.oessentials.currency.lifecycle.CurrencySystemServices::getCurrencies);
        return cs != null ? cs.formatBalance(currencyId, amount) : String.format("%.2f %s", amount, currencyId);
    }

    public String currencyDisplayName(String currencyId) {
        if (currencyId == null) {
            Economy eco = vaultEco();
            return eco != null ? eco.currencyNamePlural() : "Money";
        }
        Currencies cs = plugin.getModuleRegistry().read(fr.elias.oessentials.currency.lifecycle.CurrencySystemServices.class, fr.elias.oessentials.currency.lifecycle.CurrencySystemServices::getCurrencies);
        if (cs == null) return currencyId;
        var cur = cs.getCurrency(currencyId);
        return cur != null ? cur.getDisplayName() : currencyId;
    }


    private Economy vaultEco() {
        try {
            var rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
            return rsp != null ? rsp.getProvider() : null;
        } catch (Throwable t) {
            return null;
        }
    }
}
