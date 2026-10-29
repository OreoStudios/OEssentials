package fr.elias.oessentials.inventory.api;

import dev.oreo.modulith.core.ModuleApi;

import java.util.UUID;

/** Read-only view of a market order. */
@ModuleApi("services")
public interface OrderView {
    String getId();
    UUID getRequesterUuid();
    String getRequesterName();
    String getDisplayItemName();
    /** The serialized requested item. */
    String getItemData();
    int getTotalQty();
    int getRemainingQty();
    double getUnitPrice();
    double getEscrowTotal();
    double getEscrowRemaining();
    String getCurrencyId();
    String getStatusName();
    long getCreatedAt();
}
