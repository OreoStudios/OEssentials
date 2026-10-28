package fr.elias.oessentials.currency.api;

import dev.oreo.modulith.core.ModuleApi;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Custom currencies and balances, for modules that price things in them (shop, auction house, orders). */
@ModuleApi("services")
public interface Currencies {
    /** {@code null} when no such currency exists. */
    Currency getCurrency(String id);

    List<Currency> getAllCurrencies();

    CompletableFuture<Double> getBalance(UUID playerId, String currencyId);

    CompletableFuture<Boolean> deposit(UUID playerId, String currencyId, double amount);

    CompletableFuture<Boolean> withdraw(UUID playerId, String currencyId, double amount);

    String formatBalance(String currencyId, double amount);
}
