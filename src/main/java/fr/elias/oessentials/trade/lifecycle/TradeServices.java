package fr.elias.oessentials.trade.lifecycle;

import dev.oreo.modulith.core.ModuleApi;
import fr.elias.oessentials.trade.internal.config.TradeConfig;
import fr.elias.oessentials.trade.internal.service.TradeService;

@ModuleApi("services")
public interface TradeServices {
    TradeConfig getTradeConfig();
    TradeService getTradeService();
    fr.elias.oessentials.trade.internal.rabbit.TradeCrossServerBroker getTradeBroker();

    /** The public OEssentials API for this module, or {@code null} while it is not available. */
    default fr.elias.oessentials.api.ITradeAPI getTradeApi() {
        var s = getTradeService();
        return s == null ? null : new fr.elias.oessentials.trade.internal.api.TradeAPIImpl(s);
    }
}
