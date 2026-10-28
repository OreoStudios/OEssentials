package fr.elias.oessentials.inventory.internal;

import fr.elias.oessentials.inventory.internal.auctionhouse.AuctionHouseModule;
import fr.elias.oessentials.inventory.internal.orders.OrdersModule;
import fr.elias.oessentials.inventory.internal.orders.gui.CreateOrderFlow;
import fr.elias.oessentials.inventory.internal.orders.gui.FillOrderMenu;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

/** Chat lines typed in answer to an auction-house or orders prompt (moved from chat's listeners). */
public final class ChatInputCapture {
    private ChatInputCapture() {}

    /** Whether {@code raw} answered a pending prompt; the caller then hides the chat line. */
    public static boolean consume(AuctionHouseModule ahm, OrdersModule om, Player player, String raw) {
        try {
            if (ahm != null && ahm.enabled() && ahm.isWaitingForPrice(player.getUniqueId())) {
                ahm.consumePriceInput(player, raw);
                return true;
            }
        } catch (Throwable ignored) {}

        try {
            if (om != null && om.enabled()) {
                UUID uid = player.getUniqueId();
                if (CreateOrderFlow.isWaitingForQty(uid)) {
                    CreateOrderFlow.consumeQtyInput(om, player, raw);
                    return true;
                }
                if (CreateOrderFlow.isWaitingForPrice(uid)) {
                    CreateOrderFlow.consumePriceInput(om, player, raw);
                    return true;
                }
                if (FillOrderMenu.isWaitingForFillQty(uid)) {
                    FillOrderMenu.consumeFillQtyInput(om, player, raw);
                    return true;
                }
            }
        } catch (Throwable t) {
            Bukkit.getLogger().severe("[Orders] Chat input error: " + t);
            t.printStackTrace();
        }
        return false;
    }
}
