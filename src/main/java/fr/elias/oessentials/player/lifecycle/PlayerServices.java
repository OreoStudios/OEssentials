package fr.elias.oessentials.player.lifecycle;

import dev.oreo.modulith.core.ModuleApi;
import fr.elias.oessentials.player.internal.back.service.BackService;
import fr.elias.oessentials.player.internal.deathback.DeathBackService;
import fr.elias.oessentials.player.internal.tp.service.TeleportService;
import fr.elias.oessentials.player.internal.services.GodService;
import fr.elias.oessentials.player.internal.services.MessageService;
import fr.elias.oessentials.player.internal.services.VanishService;

@ModuleApi("services")
public interface PlayerServices {
    TeleportService getTeleportService();
    BackService getBackService();
    MessageService getMessageService();
    DeathBackService getDeathBackService();
    GodService getGodService();
    VanishService getVanishService();
    fr.elias.oessentials.player.internal.tp.rabbit.brokers.TpaCrossServerBroker getTpaBroker();
    fr.elias.oessentials.player.internal.tp.rabbit.brokers.TpCrossServerBroker getTpBroker();
    fr.elias.oessentials.player.internal.back.rabbit.BackBroker getBackBroker();
    java.util.Map<java.util.UUID, fr.elias.oessentials.player.internal.back.BackLocation> getPendingBackTeleports();
}
