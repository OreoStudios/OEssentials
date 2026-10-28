package fr.elias.oessentials.trade.lifecycle;

import dev.oreo.modulith.core.ModuleListener;
import dev.oreo.modulith.core.PluginModule;
import fr.elias.oessentials.messaging.lifecycle.MessagingReady;
import fr.elias.oessentials.messaging.lifecycle.MessagingServices;
import fr.elias.oessentials.trade.internal.rabbit.TradeCrossServerBroker;
import fr.elias.oessentials.trade.internal.rabbit.handler.*;
import fr.elias.oessentials.trade.internal.rabbit.namespace.TradePacketNamespace;
import fr.elias.oessentials.trade.internal.rabbit.packet.*;
import fr.elias.oessentials.platform.modularity.BootstrapSupport;
import fr.elias.oessentials.platform.modularity.ManagedModule;
import fr.elias.oessentials.configuration.lifecycle.ConfigurationServices;
import fr.elias.oessentials.trade.internal.command.TradeCommand;
import fr.elias.oessentials.trade.internal.config.TradeConfig;
import fr.elias.oessentials.trade.internal.service.TradeService;

@PluginModule(value = "trade", dependencies = {"configuration::services", "messaging::events", "messaging::services"}, after = {"notes"})
public final class TradeModule extends ManagedModule implements TradeServices {
    private TradeConfig tradeConfig;
    private TradeService tradeService;
    private TradeCrossServerBroker tradeBroker;

    @Override
    protected void start() {
        cleanup("tradeService", () -> { if (tradeService != null) tradeService.cancelAll(); });
        initTrade();
        initCrossServerPackets();
        listenForModuleEvents(this);
    }

    private void initCrossServerPackets() {
        var messaging = services(MessagingServices.class);
        messaging.registerNamespace(new TradePacketNamespace());
        messaging.subscribe(TradeStartPacket.class,   new TradeStartPacketHandler(plugin));
        messaging.subscribe(TradeInvitePacket.class,  new TradeInvitePacketHandler(plugin));
        messaging.subscribe(TradeStatePacket.class,   new TradeStatePacketHandler(plugin));
        messaging.subscribe(TradeConfirmPacket.class, new TradeConfirmPacketHandler(plugin));
        messaging.subscribe(TradeCancelPacket.class,  new TradeCancelPacketHandler(plugin));
        messaging.subscribe(TradeGrantPacket.class,   new TradeGrantPacketHandler(plugin));
        messaging.subscribe(TradeClosePacket.class,   new TradeClosePacketHandler(plugin));
    }

    @ModuleListener
    public void onMessagingReady(MessagingReady event) {
        if (!isActive()) return;
        if (tradeService != null && services(ConfigurationServices.class).getFeatureSettings().tradeCrossServerEnabled()) {
            this.tradeBroker = new TradeCrossServerBroker(plugin, event.packets(), services(ConfigurationServices.class).getServerConfig().serverName(), tradeService);
            plugin.getLogger().info("[TRADE] Cross-server trade broker ready.");
        } else {
            plugin.getLogger().info("[TRADE] Cross-server trade broker disabled.");
        }
    }

    @Override public TradeConfig getTradeConfig() { return tradeConfig; }
    @Override public TradeService getTradeService() { return tradeService; }
    @Override public TradeCrossServerBroker getTradeBroker() { return tradeBroker; }

    private void initTrade() {
        this.tradeConfig = new TradeConfig(plugin);

        if (this.tradeConfig.enabled && services(ConfigurationServices.class).getFeatureSettings().tradeEnabled()) {
            this.tradeService = new TradeService(plugin, this.tradeConfig);
            services(ConfigurationServices.class).getCommands().registerLegacy("trade", new TradeCommand(plugin, this.tradeService));
            plugin.getLogger().info("[Trade] Enabled.");
            org.bukkit.Bukkit.getPluginManager().registerEvents(new fr.elias.oessentials.trade.internal.ui.TradeGuiGuardListener(plugin), plugin);
            plugin.getServer().getPluginManager().registerEvents(new fr.elias.oessentials.trade.internal.ui.TradeInventoryCloseListener(plugin), plugin);
        } else {
            this.tradeService = null;
            BootstrapSupport.unregisterCommandHard(plugin, "trade");
            plugin.getLogger().info("[Trade] Disabled (trade.yml or settings.yml).");
        }
    }
}
