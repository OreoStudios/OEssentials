package fr.elias.oessentials.modgui.lifecycle;

import dev.oreo.modulith.core.PluginModule;
import fr.elias.oessentials.configuration.lifecycle.ConfigurationServices;
import fr.elias.oessentials.platform.modularity.ManagedModule;
import fr.elias.oessentials.modgui.internal.world.WorldTweaksListener;

@PluginModule(value = "mod-gui", dependencies = {"configuration::services", "messaging::events", "messaging::services", "storage::services"}, after = {"inventories"})
public final class ModGuiModule extends ManagedModule implements ModGuiServices {
    private fr.elias.oessentials.modgui.internal.ModGuiService modGuiService;
    private fr.elias.oessentials.modgui.internal.cross.ModBridge modBridge;

    @Override
    protected void start() {
        services(fr.elias.oessentials.messaging.lifecycle.MessagingServices.class)
                .registerNamespace(new fr.elias.oessentials.modgui.internal.cross.rabbit.CrossInvPacketNamespace());
        initModGui();
        listenForModuleEvents(this);
        providePublicApi(fr.elias.oessentials.api.ICrossServerAPI.class, this::getCrossServerApi);
    }

    /** Cross-server kick, freeze, vanish and other moderation actions. */
    @dev.oreo.modulith.core.ModuleListener
    public void onMessagingReady(fr.elias.oessentials.messaging.lifecycle.MessagingReady event) {
        if (!isActive()) return;
        this.modBridge = new fr.elias.oessentials.modgui.internal.cross.ModBridge(plugin, event.packets(),
                services(ConfigurationServices.class).getServerConfig().serverName());
        plugin.getLogger().info("[MOD-BRIDGE] Cross-server moderation bridge ready.");
    }

    @Override public fr.elias.oessentials.modgui.internal.ModGuiService getModGuiService() { return modGuiService; }
    @Override public fr.elias.oessentials.modgui.internal.cross.ModBridge getModBridge() { return modBridge; }

    private void initModGui() {
        try {
            this.modGuiService = new fr.elias.oessentials.modgui.internal.ModGuiService(plugin);
            plugin.getLogger().info("[ModGUI] Server management GUI ready (/modgui).");
            new WorldTweaksListener(plugin);
        } catch (Throwable t) {
            plugin.getLogger().warning("[ModGUI] Failed to init: " + t.getMessage());
            this.modGuiService = null;
        }
    }
}
