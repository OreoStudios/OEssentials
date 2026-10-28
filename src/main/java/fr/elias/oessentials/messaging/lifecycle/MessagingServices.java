package fr.elias.oessentials.messaging.lifecycle;

import dev.oreo.modulith.core.ModuleApi;
import fr.elias.oessentials.player.internal.back.BackLocation;
import fr.elias.oessentials.player.internal.back.rabbit.BackBroker;
import fr.elias.oessentials.messaging.internal.cross.ModBridge;
import fr.elias.oessentials.messaging.internal.invsee.InvseeService;
import fr.elias.oessentials.messaging.internal.invsee.rabbit.InvseeCrossServerBroker;
import fr.elias.oessentials.player.internal.tp.rabbit.brokers.TpCrossServerBroker;
import fr.elias.oessentials.player.internal.tp.rabbit.brokers.TpaCrossServerBroker;
import fr.elias.oessentials.trade.internal.rabbit.TradeCrossServerBroker;
import fr.elias.oessentials.messaging.api.PacketBus;
import java.util.Map;
import java.util.UUID;

@ModuleApi("services")
public interface MessagingServices {
    TpCrossServerBroker getTpBroker();
    BackBroker getBackBroker();
    fr.elias.oessentials.messaging.internal.network.NetworkCountReceiver getNetworkCountReceiver();
    TpaCrossServerBroker getTpaBroker();
    ModBridge getModBridge();
    InvseeService getInvseeService();
    fr.elias.oessentials.messaging.internal.invlook.manager.InvlookManager getInvlookManager();
    InvseeCrossServerBroker getInvseeBroker();
    TradeCrossServerBroker getTradeBroker();
    /** Cross-server packets, or {@code null} when RabbitMQ is disabled. */
    PacketBus getPacketBus();
    fr.elias.oessentials.storage.internal.homes.HomeTeleportBroker getHomeTpBroker();
    Map<UUID, BackLocation> getPendingBackTeleports();
}
