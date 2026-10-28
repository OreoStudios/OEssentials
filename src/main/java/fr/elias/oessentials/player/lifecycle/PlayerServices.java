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

    /** The public OEssentials API for this module, or {@code null} while it is not available. */
    default fr.elias.oessentials.api.ITeleportAPI getTeleportApi() {
        var s = getTeleportService();
        return s == null ? null : new fr.elias.oessentials.player.internal.api.TeleportAPIImpl(s);
    }

    /** The public OEssentials API for this module, or {@code null} while it is not available. */
    default fr.elias.oessentials.api.IBackAPI getBackApi() {
        var s = getBackService();
        return s == null ? null : new fr.elias.oessentials.player.internal.api.BackAPIImpl(s);
    }

    /** The public OEssentials API for this module, or {@code null} while it is not available. */
    default fr.elias.oessentials.api.IDeathBackAPI getDeathBackApi() {
        var s = getDeathBackService();
        return s == null ? null : new fr.elias.oessentials.player.internal.api.DeathBackAPIImpl(s);
    }

    /** The public OEssentials API for this module, or {@code null} while it is not available. */
    default fr.elias.oessentials.api.IVanishAPI getVanishApi() {
        var s = getVanishService();
        return s == null ? null : new fr.elias.oessentials.player.internal.api.VanishAPIImpl(s);
    }
}
