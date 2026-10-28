package fr.elias.oessentials.inventory.api;

import dev.oreo.modulith.core.ModuleApi;

/** The outcome of filling a market order. */
@ModuleApi("services")
public interface OrderFillView {
    boolean isSuccess();
    String getOutcomeName();
    int getFilledQty();
    double getPaidToSeller();
    OrderView getUpdatedOrder();
}
