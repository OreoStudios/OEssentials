package fr.elias.oessentials.proxy.lifecycle;

import dev.oreo.modulith.core.PluginModule;
import fr.elias.oessentials.configuration.lifecycle.ConfigurationServices;
import fr.elias.oessentials.platform.modularity.ManagedModule;
import fr.elias.oessentials.proxy.internal.transport.ProxyMessenger;

@PluginModule(value = "proxy-messaging", dependencies = {"configuration::services"}, after = {"kits"})
public final class ProxyMessagingModule extends ManagedModule implements ProxyMessagingServices {
    private ProxyMessenger proxyMessenger;
    private fr.elias.oessentials.commandcontrol.internal.aliases.AliasService aliasService;

    @Override
    protected void start() {
        cleanup("aliasService", () -> { if (aliasService != null) aliasService.shutdown(); });
        initProxyMessaging();
        registerCommands();
        providePublicApi(fr.elias.oessentials.api.IAliasesAPI.class, this::getAliasesApi);
    }

    @Override public ProxyMessenger getProxyMessenger() { return proxyMessenger; }
    @Override public fr.elias.oessentials.commandcontrol.internal.aliases.AliasService getAliasService() { return aliasService; }

    private void initProxyMessaging() {
        this.aliasService = new fr.elias.oessentials.commandcontrol.internal.aliases.AliasService(plugin);
        this.aliasService.load();
        this.aliasService.applyRuntimeRegistration();

        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, "BungeeCord");
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, "bungeecord:main");
        this.proxyMessenger = new ProxyMessenger(plugin);
        plugin.getLogger().info("[BOOT] Registered proxy plugin messaging channels.");

        var configuration = services(ConfigurationServices.class);
        final ProxyMessenger messenger = this.proxyMessenger;
        configuration.setServerConnector(messenger::sendToServer);
        cleanup("serverConnector", () -> configuration.setServerConnector(null));
    }

    /** Commands this module owns (registered by the commands module until 7.1.6). */
    private void registerCommands() {
        var commands = services(fr.elias.oessentials.configuration.lifecycle.ConfigurationServices.class).getCommands();
        commands.register(new fr.elias.oessentials.proxy.internal.commands.core.playercommands.ServerProxyCommand(proxyMessenger));
        commands.rewireTab("oeserver", new fr.elias.oessentials.proxy.internal.commands.core.playercommands.ServerProxyCommand(proxyMessenger));
    }
}
