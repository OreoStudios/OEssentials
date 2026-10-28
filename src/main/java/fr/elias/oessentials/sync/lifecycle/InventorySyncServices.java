package fr.elias.oessentials.sync.lifecycle;

import dev.oreo.modulith.core.ModuleApi;

@ModuleApi("services")
public interface InventorySyncServices {
    fr.elias.oessentials.sync.internal.invsee.InvseeService getInvseeService();
    fr.elias.oessentials.sync.internal.invsee.rabbit.InvseeCrossServerBroker getInvseeBroker();
    fr.elias.oessentials.sync.internal.invlook.manager.InvlookManager getInvlookManager();
}
