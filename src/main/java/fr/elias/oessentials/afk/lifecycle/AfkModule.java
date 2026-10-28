package fr.elias.oessentials.afk.lifecycle;

import dev.oreo.modulith.core.ModuleListener;
import dev.oreo.modulith.core.PluginModule;
import fr.elias.oessentials.afk.internal.rabbit.namespace.AfkPacketNamespace;
import fr.elias.oessentials.messaging.lifecycle.MessagingReady;
import fr.elias.oessentials.messaging.lifecycle.MessagingServices;
import fr.elias.oessentials.platform.modularity.ManagedModule;
import fr.elias.oessentials.afk.internal.AfkListener;
import fr.elias.oessentials.afk.internal.AfkPoolService;
import fr.elias.oessentials.afk.internal.AfkService;

@PluginModule(value = "afk", dependencies = {"configuration::services", "messaging::events", "messaging::services"}, after = {"proxy-messaging"})
public final class AfkModule extends ManagedModule implements AfkServices {
    private AfkService afkService;
    private AfkPoolService afkPoolService;

    @Override
    protected void start() {
        cleanup("afkService", () -> { if (afkService != null) afkService.shutdown(); });
        cleanup("afkPoolService", () -> { if (afkPoolService != null) afkPoolService.cleanupAll(); });
        services(MessagingServices.class).registerNamespace(new AfkPacketNamespace());
        initAfk();
        listenForModuleEvents(this);
        registerCommands();
    }

    @ModuleListener
    public void onMessagingReady(MessagingReady event) {
        if (!isActive()) return;
        try { if (afkPoolService != null) afkPoolService.tryHookCrossServerNow(); }
        catch (Throwable t) { plugin.getLogger().warning("[AfkPool] Failed to hook cross-server handlers: " + t.getMessage()); }

        try { if (afkService != null) afkService.tryHookCrossServerNow(); }
        catch (Throwable t) { plugin.getLogger().warning("[AfkService] Failed to hook cross-server handlers: " + t.getMessage()); }
    }

    @Override public AfkService getAfkService() { return afkService; }
    @Override public AfkPoolService getAfkPoolService() { return afkPoolService; }

    private void initAfk() {
        this.afkService = new AfkService(plugin);
        plugin.getServer().getPluginManager().registerEvents(new AfkListener(plugin, afkService), plugin);

        if (afkService.getAfkConfig().poolEnabled()) {
            try {
                this.afkPoolService = new AfkPoolService(plugin, afkService);
                afkService.setPoolService(afkPoolService);
                plugin.getLogger().info("[AfkPool] Enabled.");
            } catch (Throwable t) {
                plugin.getLogger().warning("[AfkPool] Failed to initialize: " + t.getMessage());
                this.afkPoolService = null;
            }
        } else {
            plugin.getLogger().info("[AfkPool] Disabled by afk/config.yml");
            this.afkPoolService = null;
        }
    }

    /** Commands this module owns (registered by the commands module until 7.1.6). */
    private void registerCommands() {
        var commands = services(fr.elias.oessentials.configuration.lifecycle.ConfigurationServices.class).getCommands();
        commands.register(new fr.elias.oessentials.afk.internal.AfkCommand(afkService));
    }
}
