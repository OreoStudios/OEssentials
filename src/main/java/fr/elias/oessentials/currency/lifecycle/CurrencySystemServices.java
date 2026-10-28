package fr.elias.oessentials.currency.lifecycle;

import dev.oreo.modulith.core.ModuleApi;
import fr.elias.oessentials.currency.internal.CurrencyConfig;
import fr.elias.oessentials.currency.internal.CurrencyService;

@ModuleApi("services")
public interface CurrencySystemServices {
    CurrencyService getCurrencyService();
    fr.elias.oessentials.currency.internal.placeholders.CurrencyPlaceholderExpansion getCurrencyPlaceholders();
    CurrencyConfig getCurrencyConfig();

    /** The public API of one currency, or {@code null} when it does not exist. */
    default fr.elias.oessentials.api.ICurrencyAPI getCurrencyApi(String currencyId) {
        var svc = getCurrencyService();
        if (svc == null) return null;
        var currency = svc.getCurrency(currencyId.toLowerCase(java.util.Locale.ROOT).trim());
        return currency == null ? null : new fr.elias.oessentials.currency.internal.api.CurrencyAPIImpl(svc, currency);
    }

    default java.util.List<fr.elias.oessentials.api.ICurrencyAPI> getCurrencyApis() {
        var svc = getCurrencyService();
        if (svc == null) return java.util.List.of();
        return svc.getAllCurrencies().stream()
                .map(c -> (fr.elias.oessentials.api.ICurrencyAPI) new fr.elias.oessentials.currency.internal.api.CurrencyAPIImpl(svc, c))
                .collect(java.util.stream.Collectors.toList());
    }

    /** Currencies and balances, or {@code null} while the currency system is not running. */
    default fr.elias.oessentials.currency.api.Currencies getCurrencies() {
        return getCurrencyService();
    }
}
