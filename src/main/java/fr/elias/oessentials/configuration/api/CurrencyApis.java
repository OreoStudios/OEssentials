package fr.elias.oessentials.configuration.api;

import fr.elias.oessentials.api.ICurrencyAPI;
import dev.oreo.modulith.core.ModuleApi;

import java.util.List;

/** The custom currencies of the public API; provided through {@link PublicApis} by currency-system. */
@ModuleApi("services")
public interface CurrencyApis {
    /** {@code null} when no such currency exists. */
    ICurrencyAPI currency(String currencyId);

    List<ICurrencyAPI> currencies();
}
