package fr.elias.oessentials.integrations.lifecycle;

import dev.oreo.modulith.core.ModuleApi;
import fr.elias.oessentials.integrations.internal.placeholders.PlaceholderAPIHook;
import fr.elias.oessentials.currency.internal.placeholders.CurrencyPlaceholderExpansion;
import org.bstats.bukkit.Metrics;

@ModuleApi("services")
public interface IntegrationsServices {
    Metrics getMetrics();
    PlaceholderAPIHook getPlaceholderHook();
}
