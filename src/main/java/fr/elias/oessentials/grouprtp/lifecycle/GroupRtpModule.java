package fr.elias.oessentials.grouprtp.lifecycle;

import dev.oreo.modulith.core.ModuleListener;
import dev.oreo.modulith.core.PluginModule;
import fr.elias.oessentials.platform.modularity.ManagedModule;
import fr.elias.oessentials.messaging.lifecycle.MessagingReady;
import fr.elias.oessentials.messaging.lifecycle.MessagingServices;

@PluginModule(value = "group-rtp", dependencies = {"messaging::events", "messaging::services"}, after = {"portals"}, optional = true)
public final class GroupRtpModule extends ManagedModule implements GroupRtpServices {
    private fr.elias.oessentials.grouprtp.internal.GroupRtpModule groupRtpModule;

    @Override
    protected void start() {
        services(MessagingServices.class).registerNamespace(new fr.elias.oessentials.grouprtp.internal.rabbit.GroupRtpPacketNamespace());
        initGroupRtp();
        listenForModuleEvents(this);
    }

    @Override public fr.elias.oessentials.grouprtp.internal.GroupRtpModule getGroupRtpModule() { return groupRtpModule; }

    private void initGroupRtp() {
        try {
            this.groupRtpModule = new fr.elias.oessentials.grouprtp.internal.GroupRtpModule(plugin);
            this.groupRtpModule.init();
        } catch (Throwable t) {
            plugin.getLogger().severe("[GroupRTP] Failed to initialize: " + t.getMessage());
        }
    }

    @ModuleListener
    public void onMessagingReady(MessagingReady event) {
        if (!isActive() || groupRtpModule == null) return;
        new fr.elias.oessentials.grouprtp.internal.rabbit.GroupRtpCrossServerBroker(plugin, event.packets());
        plugin.getLogger().info("[GroupRTP] Cross-server broker ready.");
    }

}
