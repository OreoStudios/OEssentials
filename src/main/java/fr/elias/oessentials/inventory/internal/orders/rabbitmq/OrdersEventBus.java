package fr.elias.oessentials.inventory.internal.orders.rabbitmq;

import fr.elias.oessentials.OEssentials;
import fr.elias.oessentials.inventory.internal.orders.model.Order;
import fr.elias.oessentials.platform.scheduling.OreScheduler;
import fr.elias.oessentials.inventory.internal.orders.service.OrderService;
import fr.elias.oessentials.inventory.internal.orders.gui.OrdersGuiManager;
import java.util.logging.Logger;

/**
 * Handles publishing OrderSyncPackets to RabbitMQ and receiving them from other servers.
 * On receive: applies to OrderService memory + triggers a debounced GUI refresh.
 *
 * Changes from original: added debug logging on all publish/receive paths.
 */
public final class OrdersEventBus {

    private final OEssentials plugin;
    private final String         serverId;
    private final Logger         log;
    private OrderService     service;        // set after construction to break circular dep
    private OrdersGuiManager guiManager;     // set after GUI is ready

    public OrdersEventBus(OEssentials plugin, String serverId) {
        this.plugin   = plugin;
        this.serverId = serverId;
        this.log      = plugin.getLogger();
    }

    public void setService(OrderService service)         { this.service = service; }
    public void setGuiManager(OrdersGuiManager manager)  { this.guiManager = manager; }


    // ── Publish ───────────────────────────────────────────────────────────────

    public void publishCreated(Order order) {
        log.info("[Orders/EventBus] PUBLISH ORDER_CREATED orderId=" + order.getId()
                + " from server=" + serverId);
        sendPacket(OrderSyncPacket.created(serverId, order));
    }

    public void publishUpdated(Order order) {
        log.info("[Orders/EventBus] PUBLISH ORDER_UPDATED orderId=" + order.getId()
                + " status=" + order.getStatus()
                + " remainingQty=" + order.getRemainingQty()
                + " from server=" + serverId);
        sendPacket(OrderSyncPacket.updated(serverId, order));
    }

    public void publishRemoved(Order order) {
        log.info("[Orders/EventBus] PUBLISH ORDER_REMOVED orderId=" + order.getId()
                + " from server=" + serverId);
        sendPacket(OrderSyncPacket.removed(serverId, order));
    }


    // ── Receive ───────────────────────────────────────────────────────────────

    /**
     * Called by OEssentials when an OrderSyncPacket arrives from another server.
     * Runs on the RabbitMQ consumer thread — schedules main-thread work as needed.
     */
    public void onReceive(OrderSyncPacket pkt) {
        if (pkt == null) return;
        if (serverId.equals(pkt.getServerId())) {
            // Own broadcast echoed back — ignore
            return;
        }

        log.info("[Orders/EventBus] RECEIVED " + pkt.getType()
                + " orderId=" + pkt.getOrderId()
                + " from server=" + pkt.getServerId());

        // Apply to in-memory service on main thread for GUI consistency
        OreScheduler.run(plugin, () -> {
            if (service != null) {
                service.applyIncomingEvent(pkt);
            } else {
                log.warning("[Orders/EventBus] onReceive: service is null, cannot apply event for " + pkt.getOrderId());
            }
            if (guiManager != null) {
                guiManager.scheduleRefreshAll();
            }
        });
    }


    // ── Internal ──────────────────────────────────────────────────────────────

    private void sendPacket(OrderSyncPacket pkt) {
        try {
            var pm = plugin.getPacketBus();
            if (pm != null && pm.isInitialized()) {
                pm.sendPacket(pkt);
            } else {
                log.warning("[Orders/EventBus] sendPacket: PacketManager not available — cross-server update skipped for " + pkt.getOrderId());
            }
        } catch (Throwable t) {
            log.warning("[Orders/EventBus] sendPacket failed for " + pkt.getOrderId() + ": " + t.getMessage());
        }
    }
}