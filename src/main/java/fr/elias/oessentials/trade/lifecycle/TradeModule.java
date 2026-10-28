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

@PluginModule(value = "trade", dependencies = {"configuration::services", "messaging::events", "messaging::services", "storage::services"}, after = {"notes"})
public final class TradeModule extends ManagedModule implements TradeServices {
    private TradeConfig tradeConfig;
    private TradeService tradeService;
    private TradeCrossServerBroker tradeBroker;
    private volatile fr.elias.oessentials.trade.internal.cross.CrossTradeService crossTrades;

    @Override
    protected void start() {
        cleanup("tradeService", () -> { if (tradeService != null) tradeService.cancelAll(); });
        initTrade();
        initCrossServerPackets();
        listenForModuleEvents(this);
        providePublicApi(fr.elias.oessentials.api.ITradeAPI.class, this::getTradeApi);
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
        messaging.subscribe(fr.elias.oessentials.trade.internal.cross.CrossTradeSignalPacket.class, (channel, pkt) -> {
            var cross = crossTrades;
            if (cross != null) cross.onSignal(pkt);
        });
    }

    @ModuleListener
    public void onMessagingReady(MessagingReady event) {
        if (!isActive()) return;
        // The old packet-based cross-server broker stays off: it never ran before 7.1.6 and lost or
        // duplicated items when tested. Cross-server trades use CrossTradeService instead, whose
        // trade record (and the offered items) lives in MongoDB.
        if (tradeService == null || !services(ConfigurationServices.class).getFeatureSettings().tradeCrossServerEnabled()) {
            plugin.getLogger().info("[TRADE] Cross-server trading disabled.");
            return;
        }
        var client = services(fr.elias.oessentials.storage.lifecycle.StorageServices.class).getHomesMongoClient();
        if (client == null) {
            plugin.getLogger().warning("[TRADE] Cross-server trading needs essentials.storage: mongodb; trades stay on one server.");
            return;
        }
        var cfg = plugin.getConfig();
        var collection = client.getDatabase(cfg.getString("storage.mongo.database", "oreo"))
                .getCollection(cfg.getString("storage.mongo.collectionPrefix", "oreo_") + "trades");
        var service = new fr.elias.oessentials.trade.internal.cross.CrossTradeService(plugin,
                new fr.elias.oessentials.trade.internal.cross.CrossTradeStore(collection), event.packets(),
                id -> tradeService.getTradeIdByPlayer(id) != null);
        plugin.getServer().getPluginManager().registerEvents(service, plugin);
        this.crossTrades = service;
        cleanup("crossTrades", () -> { service.shutdown(); crossTrades = null; });
        plugin.getLogger().info("[TRADE] Cross-server trading ready (MongoDB trade records).");
    }

    @Override public TradeConfig getTradeConfig() { return tradeConfig; }
    @Override public TradeService getTradeService() { return tradeService; }
    @Override public TradeCrossServerBroker getTradeBroker() { return tradeBroker; }

    private void initTrade() {
        this.tradeConfig = new TradeConfig(plugin);

        if (this.tradeConfig.enabled && services(ConfigurationServices.class).getFeatureSettings().tradeEnabled()) {
            this.tradeService = new TradeService(plugin, this.tradeConfig);
            services(ConfigurationServices.class).getCommands().registerLegacy("trade", new TradeCommand(plugin, this.tradeService, () -> crossTrades));
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
