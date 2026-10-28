package fr.elias.oessentials.inventory.internal.orders;

import fr.elias.oessentials.inventory.api.MarketOrders;
import fr.elias.oessentials.inventory.api.OrderFillView;
import fr.elias.oessentials.inventory.api.OrderView;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Publishes {@link OrdersModule} as the {@link MarketOrders} contract. */
public final class MarketOrdersAdapter implements MarketOrders {
    private final OrdersModule orders;

    public MarketOrdersAdapter(OrdersModule orders) {
        this.orders = orders;
    }

    @Override public boolean enabled() { return orders.enabled(); }
    @Override public List<? extends OrderView> activeOrders() { return orders.getService().getActiveOrders(); }
    @Override public List<? extends OrderView> activeOrdersByPlayer(UUID requester) { return orders.getService().getActiveOrdersByPlayer(requester); }
    @Override public CompletableFuture<Boolean> cancelOrder(Player requester, String orderId) { return orders.getService().cancelOrder(requester, orderId); }
    @Override public CompletableFuture<? extends OrderFillView> fillOrder(Player filler, String orderId, int quantity) { return orders.getService().fillOrder(filler, orderId, quantity); }
}
