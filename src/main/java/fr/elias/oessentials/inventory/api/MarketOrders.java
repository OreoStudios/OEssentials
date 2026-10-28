package fr.elias.oessentials.inventory.api;

import dev.oreo.modulith.core.ModuleApi;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** The /market buy orders, for the web panel and other modules. */
@ModuleApi("services")
public interface MarketOrders {
    boolean enabled();

    List<? extends OrderView> activeOrders();

    List<? extends OrderView> activeOrdersByPlayer(UUID requester);

    CompletableFuture<Boolean> cancelOrder(Player requester, String orderId);

    /** Takes the items from the filler's inventory (main thread) and pays them asynchronously. */
    CompletableFuture<? extends OrderFillView> fillOrder(Player filler, String orderId, int quantity);
}
