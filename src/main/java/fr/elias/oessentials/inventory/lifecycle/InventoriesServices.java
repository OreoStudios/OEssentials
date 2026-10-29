package fr.elias.oessentials.inventory.lifecycle;

import dev.oreo.modulith.core.ModuleApi;
import fr.elias.oessentials.inventory.internal.auctionhouse.AuctionHouseModule;
import fr.elias.oessentials.inventory.internal.sellgui.manager.SellGuiManager;
import fr.minuskube.inv.InventoryManager;

@ModuleApi("services")
public interface InventoriesServices {
    AuctionHouseModule getAuctionHouse();
    fr.elias.oessentials.inventory.internal.orders.OrdersModule getOrdersModule();
    InventoryManager getInvManager();
    SellGuiManager getSellGuiManager();

    /** The public OEssentials API for this module, or {@code null} while it is not available. */
    default fr.elias.oessentials.api.ISellGuiAPI getSellGuiApi() {
        var s = getSellGuiManager();
        return s == null ? null : new fr.elias.oessentials.inventory.internal.api.SellGuiAPIImpl(s);
    }

    /** The public OEssentials API for this module, or {@code null} while it is not available. */
    default fr.elias.oessentials.api.IOrdersAPI getOrdersApi() {
        var module = getOrdersModule();
        if (module == null) return null;
        var svc = module.getService();
        return svc == null ? null : new fr.elias.oessentials.inventory.internal.api.OrdersAPIImpl(svc);
    }

    /** The public OEssentials API for this module, or {@code null} while it is not available. */
    default fr.elias.oessentials.api.IAuctionHouseAPI getAuctionHouseApi() {
        var module = getAuctionHouse();
        return module == null ? null : new fr.elias.oessentials.inventory.internal.api.AuctionHouseAPIImpl(module);
    }

    /** Market orders, or {@code null} when the orders feature is not loaded. */
    default fr.elias.oessentials.inventory.api.MarketOrders getMarketOrders() {
        var module = getOrdersModule();
        return module == null ? null : new fr.elias.oessentials.inventory.internal.orders.MarketOrdersAdapter(module);
    }

    /** Whether {@code raw} answered an auction-house or orders chat prompt of {@code player}. */
    default boolean consumeChatInput(org.bukkit.entity.Player player, String raw) {
        return fr.elias.oessentials.inventory.internal.ChatInputCapture.consume(getAuctionHouse(), getOrdersModule(), player, raw);
    }
}
