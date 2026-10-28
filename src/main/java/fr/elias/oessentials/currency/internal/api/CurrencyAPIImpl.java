package fr.elias.oessentials.currency.internal.api;

import fr.elias.oessentials.api.ICurrencyAPI;
import fr.elias.oessentials.api.events.CurrencyTransactionEvent;
import fr.elias.oessentials.api.events.CurrencyTransferEvent;
import fr.elias.oessentials.currency.api.Currency;
import fr.elias.oessentials.currency.internal.CurrencyService;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Internal implementation of {@link ICurrencyAPI}. Not part of the public API surface.
 */
public class CurrencyAPIImpl implements ICurrencyAPI {

    private final CurrencyService service;
    private final Currency currency;

    public CurrencyAPIImpl(CurrencyService service, Currency currency) {
        this.service = service;
        this.currency = currency;
    }

    @Override
    public @NotNull String getId() { return currency.getId(); }

    @Override
    public @NotNull String getName() { return currency.getName(); }

    @Override
    public @NotNull String getSymbol() { return currency.getSymbol(); }

    @Override
    public @NotNull String getDisplayName() { return currency.getDisplayName(); }

    @Override
    public double getDefaultBalance() { return currency.getDefaultBalance(); }

    @Override
    public boolean isTradeable() { return currency.isTradeable(); }

    @Override
    public boolean isAllowNegative() { return currency.isAllowNegative(); }

    @Override
    public @NotNull String format(double amount) { return currency.format(amount); }

    // -------------------------------------------------------------------------

    @Override
    public @NotNull CompletableFuture<Double> getBalance(@NotNull UUID playerId) {
        return service.getBalance(playerId, currency.getId());
    }

    @Override
    public @NotNull CompletableFuture<Boolean> has(@NotNull UUID playerId, double amount) {
        return service.getBalance(playerId, currency.getId())
                .thenApply(balance -> balance >= amount);
    }

    @Override
    public @NotNull CompletableFuture<Boolean> deposit(@NotNull UUID playerId, double amount) {
        CurrencyTransactionEvent event = new CurrencyTransactionEvent(
                playerId, currency.getId(), CurrencyTransactionEvent.Type.DEPOSIT, amount);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return CompletableFuture.completedFuture(false);

        return service.deposit(playerId, currency.getId(), amount);
    }

    @Override
    public @NotNull CompletableFuture<Boolean> withdraw(@NotNull UUID playerId, double amount) {
        CurrencyTransactionEvent event = new CurrencyTransactionEvent(
                playerId, currency.getId(), CurrencyTransactionEvent.Type.WITHDRAW, amount);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return CompletableFuture.completedFuture(false);

        return service.withdraw(playerId, currency.getId(), amount);
    }

    @Override
    public @NotNull CompletableFuture<Void> setBalance(@NotNull UUID playerId, double amount) {
        CurrencyTransactionEvent event = new CurrencyTransactionEvent(
                playerId, currency.getId(), CurrencyTransactionEvent.Type.SET, amount);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return CompletableFuture.completedFuture(null);

        return service.setBalance(playerId, currency.getId(), amount);
    }

    @Override
    public @NotNull CompletableFuture<Boolean> transfer(@NotNull UUID from, @NotNull UUID to, double amount) {
        CurrencyTransferEvent event = new CurrencyTransferEvent(from, to, currency.getId(), amount);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return CompletableFuture.completedFuture(false);

        return service.transfer(from, to, currency.getId(), amount);
    }
}
