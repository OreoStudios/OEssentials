package fr.elias.oessentials.messaging.lifecycle;

import dev.oreo.modulith.core.PluginModule;
import fr.elias.oessentials.platform.modularity.ManagedModule;
import fr.elias.oessentials.afk.lifecycle.AfkServices;
import fr.elias.oessentials.configuration.lifecycle.ConfigurationServices;
import fr.elias.oessentials.economy.lifecycle.EconomyServices;
import fr.elias.oessentials.inventory.lifecycle.InventoriesServices;
import fr.elias.oessentials.mail.lifecycle.MailServices;
import fr.elias.oessentials.player.lifecycle.PlayerServices;
import fr.elias.oessentials.proxy.lifecycle.ProxyMessagingServices;
import fr.elias.oessentials.storage.lifecycle.StorageServices;
import fr.elias.oessentials.trade.lifecycle.TradeServices;
import fr.elias.oessentials.afk.internal.rabbit.packets.AfkPoolEnterPacket;
import fr.elias.oessentials.afk.internal.rabbit.packets.AfkPoolExitPacket;
import fr.elias.oessentials.player.internal.back.BackLocation;
import fr.elias.oessentials.player.internal.back.listeners.BackJoinListener;
import fr.elias.oessentials.player.internal.back.rabbit.BackBroker;
import fr.elias.oessentials.player.internal.back.rabbit.packets.BackTeleportPacket;
import fr.elias.oessentials.messaging.internal.cross.ModBridge;
import fr.elias.oessentials.messaging.internal.invsee.InvseeService;
import fr.elias.oessentials.messaging.internal.invsee.rabbit.InvseeCrossServerBroker;
import fr.elias.oessentials.messaging.internal.invsee.rabbit.packets.InvseeEditPacket;
import fr.elias.oessentials.messaging.internal.invsee.rabbit.packets.InvseeOpenRequestPacket;
import fr.elias.oessentials.messaging.internal.invsee.rabbit.packets.InvseeStatePacket;
import fr.elias.oessentials.messaging.internal.oreobotfeatures.rabbit.handlers.PlayerJoinPacketHandler;
import fr.elias.oessentials.messaging.internal.oreobotfeatures.rabbit.handlers.PlayerQuitPacketHandler;
import fr.elias.oessentials.messaging.internal.oreobotfeatures.rabbit.packets.PlayerJoinPacket;
import fr.elias.oessentials.messaging.internal.oreobotfeatures.rabbit.packets.PlayerQuitPacket;
import fr.elias.oessentials.player.internal.tp.rabbit.brokers.CrossServerTeleportBroker;
import fr.elias.oessentials.player.internal.tp.rabbit.brokers.TpCrossServerBroker;
import fr.elias.oessentials.player.internal.tp.rabbit.brokers.TpaCrossServerBroker;
import fr.elias.oessentials.player.internal.tp.rabbit.packets.TpJumpPacket;
import fr.elias.oessentials.player.internal.tp.rabbit.packets.TpaAcceptPacket;
import fr.elias.oessentials.player.internal.tp.rabbit.packets.TpaBringPacket;
import fr.elias.oessentials.player.internal.tp.rabbit.packets.TpaRequestPacket;
import fr.elias.oessentials.player.internal.tp.rabbit.packets.TpaSummonPacket;
import fr.elias.oessentials.trade.internal.rabbit.TradeCrossServerBroker;
import fr.elias.oessentials.trade.internal.rabbit.handler.TradeCancelPacketHandler;
import fr.elias.oessentials.trade.internal.rabbit.handler.TradeClosePacketHandler;
import fr.elias.oessentials.trade.internal.rabbit.handler.TradeConfirmPacketHandler;
import fr.elias.oessentials.trade.internal.rabbit.handler.TradeGrantPacketHandler;
import fr.elias.oessentials.trade.internal.rabbit.handler.TradeInvitePacketHandler;
import fr.elias.oessentials.trade.internal.rabbit.handler.TradeStartPacketHandler;
import fr.elias.oessentials.trade.internal.rabbit.handler.TradeStatePacketHandler;
import fr.elias.oessentials.trade.internal.rabbit.packet.TradeCancelPacket;
import fr.elias.oessentials.trade.internal.rabbit.packet.TradeClosePacket;
import fr.elias.oessentials.trade.internal.rabbit.packet.TradeConfirmPacket;
import fr.elias.oessentials.trade.internal.rabbit.packet.TradeGrantPacket;
import fr.elias.oessentials.trade.internal.rabbit.packet.TradeInvitePacket;
import fr.elias.oessentials.trade.internal.rabbit.packet.TradeStartPacket;
import fr.elias.oessentials.trade.internal.rabbit.packet.TradeStatePacket;
import fr.elias.oessentials.storage.internal.warps.rabbit.packets.PlayerWarpTeleportRequestPacket;
import fr.elias.oessentials.messaging.internal.rabbitmq.PacketChannels;
import fr.elias.oessentials.messaging.internal.rabbitmq.handler.RemoteMessagePacketHandler;
import fr.elias.oessentials.messaging.internal.rabbitmq.packet.PacketManager;
import fr.elias.oessentials.messaging.internal.rabbitmq.sender.RabbitMQSender;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@PluginModule(value = "messaging", dependencies = {"afk::services", "configuration::services", "economy::services", "inventories::services", "mail::services", "player-services::services", "proxy-messaging::services", "storage::services", "trade::services"}, after = {"dialogs"})
public final class MessagingModule extends ManagedModule implements MessagingServices {
    private TpCrossServerBroker tpBroker;
    private BackBroker backBroker;
    private fr.elias.oessentials.messaging.internal.network.NetworkCountReceiver networkCountReceiver;
    private TpaCrossServerBroker tpaBroker;
    private ModBridge modBridge;
    private InvseeService invseeService;
    private InvseeCrossServerBroker invseeBroker;
    private TradeCrossServerBroker tradeBroker;
    private volatile PacketManager packetManager;
    private volatile RabbitMQSender connectingSender;
    private fr.elias.oessentials.storage.internal.homes.HomeTeleportBroker homeTpBroker;
    private Map<UUID, BackLocation> pendingBackTeleports = new ConcurrentHashMap<>();

    @Override
    protected void start() {
        cleanup("connectingSender", () -> { if (connectingSender != null) connectingSender.close(); });
        cleanup("backBroker", () -> { if (backBroker != null) backBroker.shutdown(); });
        cleanup("packetManager", () -> { if (packetManager != null) packetManager.close(); });
        cleanup("networkCountReceiver", () -> { if (networkCountReceiver != null) networkCountReceiver.stop(); });
        cleanup("pending-back", pendingBackTeleports::clear);
        initRabbitMQ();
    }

    @Override public TpCrossServerBroker getTpBroker() { return tpBroker; }
    @Override public BackBroker getBackBroker() { return backBroker; }
    @Override public fr.elias.oessentials.messaging.internal.network.NetworkCountReceiver getNetworkCountReceiver() { return networkCountReceiver; }
    @Override public TpaCrossServerBroker getTpaBroker() { return tpaBroker; }
    @Override public ModBridge getModBridge() { return modBridge; }
    @Override public InvseeService getInvseeService() { return invseeService; }
    @Override public InvseeCrossServerBroker getInvseeBroker() { return invseeBroker; }
    @Override public TradeCrossServerBroker getTradeBroker() { return tradeBroker; }
    @Override public PacketManager getPacketManager() { return packetManager; }
    @Override public fr.elias.oessentials.storage.internal.homes.HomeTeleportBroker getHomeTpBroker() { return homeTpBroker; }
    @Override public Map<UUID, BackLocation> getPendingBackTeleports() { return pendingBackTeleports; }

    private void initRabbitMQ() {
        final boolean invSyncEnabled = services(ConfigurationServices.class).getSettingsConfig().featureOption("cross-server", "invsee", true);

        if (!services(ConfigurationServices.class).getRabbitEnabled()) {
            plugin.getLogger().info("[RABBIT] Disabled.");
            this.packetManager = null; this.networkCountReceiver = null;
            this.invseeBroker  = null; this.invseeService = new InvseeService(plugin, null);
            plugin.getLogger().info("[INVSEE] Local mode (RabbitMQ disabled)");
            return;
        }

        // Start in local mode immediately so the server thread is never blocked.
        // The RabbitMQ connection attempt runs async and upgrades to cross-server
        // mode on the main thread if it succeeds.
        services(EconomyServices.class).getOfflinePlayerCache();
        this.packetManager = null; this.networkCountReceiver = null;
        this.invseeBroker  = null; this.invseeService = new InvseeService(plugin, null);
        plugin.getLogger().info("[RABBIT] Connecting async (non-blocking)...");

        final String uri        = plugin.getConfig().getString("rabbitmq.uri");
        final String serverName = services(ConfigurationServices.class).getConfigService().serverName();

        fr.elias.oessentials.platform.scheduling.OreScheduler.runAsync(plugin, () -> {
            if (!isActive()) return;
            RabbitMQSender rabbit = new RabbitMQSender(uri, serverName, services(ConfigurationServices.class).getConfigService()::isDebugEnabled);
            connectingSender = rabbit;
            if (!isActive()) { rabbit.close(); return; }
            boolean connected = rabbit.connect();
            if (!isActive()) { rabbit.close(); return; }

            try {
            fr.elias.oessentials.platform.scheduling.OreScheduler.run(plugin, () -> {
                if (!isActive()) { rabbit.close(); return; }
                if (!connected) {
                    rabbit.close();
                    connectingSender = null;
                    plugin.getLogger().severe("[RABBIT] Connect failed; continuing without messaging.");
                    plugin.getLogger().info("[INVSEE] Local mode (RabbitMQ connection failed)");
                    initBrokers();
                    return;
                }

                this.packetManager = new PacketManager(plugin, rabbit);
                registerAllPacketsDeterministically(this.packetManager);

                try {
                    this.networkCountReceiver = new fr.elias.oessentials.messaging.internal.network.NetworkCountReceiver(plugin, uri);
                    if (this.networkCountReceiver.start()) { plugin.getLogger().info("[NetworkCountReceiver] Listening for proxy player counts."); }
                    else { plugin.getLogger().warning("[NetworkCountReceiver] Failed to start."); this.networkCountReceiver = null; }
                } catch (Throwable t) { plugin.getLogger().warning("[NetworkCountReceiver] Error: " + t.getMessage()); this.networkCountReceiver = null; }

                if (invSyncEnabled) {
                    try {
                        this.invseeBroker  = new InvseeCrossServerBroker(plugin, this.packetManager, services(ConfigurationServices.class).getConfigService().serverName(), null);
                        this.invseeService = new InvseeService(plugin, this.invseeBroker);
                        this.invseeBroker.setService(this.invseeService);
                        plugin.getLogger().info("[INVSEE] Cross-server enabled.");
                    } catch (Throwable t) {
                        this.invseeBroker  = null; this.invseeService = new InvseeService(plugin, null);
                        plugin.getLogger().warning("[INVSEE] Cross-server failed, using local mode: " + t.getMessage());
                    }
                } else {
                    this.invseeBroker = null; this.invseeService = new InvseeService(plugin, null);
                    plugin.getLogger().info("[INVSEE] Local mode (invSyncEnabled=false)");
                }

                this.packetManager.subscribeChannel(PacketChannels.GLOBAL);
                this.packetManager.subscribeChannel(fr.elias.oessentials.messaging.internal.rabbitmq.channel.PacketChannel.individual(services(ConfigurationServices.class).getConfigService().serverName()));
                plugin.getLogger().info("[RABBIT] Subscribed channels: global + individual(" + services(ConfigurationServices.class).getConfigService().serverName() + ")");

                if (this.invseeBroker != null) {
                    this.packetManager.subscribe(InvseeOpenRequestPacket.class, (channel, pkt) -> this.invseeBroker.handleOpenRequest(pkt));
                    this.packetManager.subscribe(InvseeStatePacket.class,       (channel, pkt) -> this.invseeBroker.handleState(pkt));
                    this.packetManager.subscribe(InvseeEditPacket.class,        (channel, pkt) -> this.invseeBroker.handleEdit(pkt));
                    plugin.getLogger().info("[INVSEE] Subscribed Invsee packets.");
                }

                this.packetManager.subscribe(fr.elias.oessentials.messaging.internal.rabbitmq.packet.impl.SendRemoteMessagePacket.class, new RemoteMessagePacketHandler());
                this.packetManager.subscribe(fr.elias.oessentials.chat.internal.msg.CrossServerMsgPacket.class, new fr.elias.oessentials.chat.internal.msg.CrossServerMsgHandler(services(PlayerServices.class).getMessageService()));
                this.packetManager.subscribe(TradeStartPacket.class,   new TradeStartPacketHandler(plugin));
                this.packetManager.subscribe(PlayerJoinPacket.class,   new PlayerJoinPacketHandler(plugin));
                this.packetManager.subscribe(PlayerQuitPacket.class,   new PlayerQuitPacketHandler(plugin));
                if (services(PlayerServices.class).getVanishService() != null) {
                    this.packetManager.subscribe(
                            fr.elias.oessentials.player.internal.vanish.rabbit.VanishSyncPacket.class,
                            (channel, pkt) -> services(PlayerServices.class).getVanishService().handleRemoteState(pkt.getPlayerId(), pkt.isVanished(), pkt.getSourceServer()));
                    plugin.getLogger().info("[VANISH] Cross-server sync subscribed.");
                }
                this.packetManager.subscribe(fr.elias.oessentials.messaging.internal.rabbitmq.packet.impl.DeathMessagePacket.class, new fr.elias.oessentials.messaging.internal.rabbitmq.handler.DeathMessagePacketHandler(plugin));
                this.packetManager.subscribe(TradeInvitePacket.class,  new TradeInvitePacketHandler(plugin));
                this.packetManager.subscribe(TradeStatePacket.class,   new TradeStatePacketHandler(plugin));
                this.packetManager.subscribe(TradeConfirmPacket.class, new TradeConfirmPacketHandler(plugin));
                this.packetManager.subscribe(TradeCancelPacket.class,  new TradeCancelPacketHandler(plugin));
                this.packetManager.subscribe(TradeGrantPacket.class,   new TradeGrantPacketHandler(plugin));
                this.packetManager.subscribe(TradeClosePacket.class,   new TradeClosePacketHandler(plugin));

                if (services(InventoriesServices.class).getAuctionHouse() != null && services(InventoriesServices.class).getAuctionHouse().enabled()) {
                    this.packetManager.subscribe(
                            fr.elias.oessentials.inventory.internal.auctionhouse.rabbitmq.AuctionSyncPacket.class,
                            (channel, pkt) -> {
                                try { services(InventoriesServices.class).getAuctionHouse().applyIncomingSync(pkt); }
                                catch (Throwable t) { plugin.getLogger().warning("[AH] Failed to handle AuctionSyncPacket: " + t.getMessage()); }
                            });
                    plugin.getLogger().info("[AH] Cross-server sync subscribed.");
                }

                if (services(InventoriesServices.class).getOrdersModule() != null && services(InventoriesServices.class).getOrdersModule().enabled()) {
                    this.packetManager.subscribe(
                            fr.elias.oessentials.inventory.internal.orders.rabbitmq.OrderSyncPacket.class,
                            (channel, pkt) -> {
                                try { services(InventoriesServices.class).getOrdersModule().getEventBus().onReceive(pkt); }
                                catch (Throwable t) { plugin.getLogger().warning("[Orders] Failed to handle OrderSyncPacket: " + t.getMessage()); }
                            });
                    plugin.getLogger().info("[Orders] Cross-server sync subscribed.");
                }

                // Mail delivery notifications from other servers
                if (services(MailServices.class).getMailListener() != null) {
                    this.packetManager.subscribe(
                            fr.elias.oessentials.mail.internal.rabbitmq.MailDeliveryPacket.class,
                            (channel, pkt) -> {
                                try { services(MailServices.class).getMailListener().onMailDelivery(pkt); }
                                catch (Throwable t) { plugin.getLogger().warning("[Mail] Failed to handle MailDeliveryPacket: " + t.getMessage()); }
                            });
                    plugin.getLogger().info("[Mail] Cross-server delivery subscribed.");
                }

                this.packetManager.init();

                try { if (services(AfkServices.class).getAfkPoolService() != null) services(AfkServices.class).getAfkPoolService().tryHookCrossServerNow(); }
                catch (Throwable t) { plugin.getLogger().warning("[AfkPool] Failed to hook cross-server handlers: " + t.getMessage()); }

                try { if (services(AfkServices.class).getAfkService() != null) services(AfkServices.class).getAfkService().tryHookCrossServerNow(); }
                catch (Throwable t) { plugin.getLogger().warning("[AfkService] Failed to hook cross-server handlers: " + t.getMessage()); }

                try { plugin.getLogger().info("[RABBIT] Packet registry checksum=" + this.packetManager.registryChecksum()); }
                catch (Throwable ignored) {}

                plugin.getLogger().info("[RABBIT] Connected and subscriptions active.");
                initBrokers();
                publishEvent(new MessagingReady(packetManager));
            }); // end OreScheduler.run (main thread)
            } catch (RuntimeException schedulingFailure) {
                rabbit.close();
                if (isActive()) throw schedulingFailure;
            }
        }); // end OreScheduler.runAsync
    }

    private void initBrokers() {
        if (packetManager != null && packetManager.isInitialized()) {
            this.modBridge = new ModBridge(plugin, packetManager, services(ConfigurationServices.class).getConfigService().serverName());
            plugin.getLogger().info("[MOD-BRIDGE] Cross-server moderation bridge ready.");
        } else {
            this.modBridge = null;
            plugin.getLogger().info("[MOD-BRIDGE] Disabled (PacketManager unavailable).");
        }

        if (packetManager != null && packetManager.isInitialized() && services(TradeServices.class).getTradeService() != null && services(ConfigurationServices.class).getSettingsConfig().tradeCrossServerEnabled()) {
            this.tradeBroker = new TradeCrossServerBroker(plugin, packetManager, services(ConfigurationServices.class).getConfigService().serverName(), services(TradeServices.class).getTradeService());
            plugin.getLogger().info("[TRADE] Cross-server trade broker ready.");
        } else {
            this.tradeBroker = null;
            plugin.getLogger().info("[TRADE] Cross-server trade broker disabled.");
        }

        if (packetManager != null && packetManager.isInitialized()) {
            final var cs = plugin.getCrossServerSettings();
            if (cs.spawn() || cs.warps()) {
                new CrossServerTeleportBroker(plugin, services(StorageServices.class).getSpawnService(), services(StorageServices.class).getWarpService(), packetManager, services(ConfigurationServices.class).getConfigService().serverName());
                plugin.getLogger().info("[BROKER] CrossServerTeleportBroker ready.");
            }
            if (cs.homes()) {
                this.homeTpBroker = new fr.elias.oessentials.storage.internal.homes.HomeTeleportBroker(plugin, services(StorageServices.class).getHomeService(), packetManager);
                plugin.getLogger().info("[BROKER] HomeTeleportBroker ready.");
            }

            this.tpaBroker = new TpaCrossServerBroker(plugin, services(PlayerServices.class).getTeleportService(), this.packetManager, services(ProxyMessagingServices.class).getProxyMessenger(), services(ConfigurationServices.class).getConfigService().serverName());
            plugin.getLogger().info("[BROKER] TPA cross-server broker ready.");

            this.tpBroker = new TpCrossServerBroker(plugin, services(PlayerServices.class).getTeleportService(), this.packetManager, services(ProxyMessagingServices.class).getProxyMessenger(), services(ConfigurationServices.class).getConfigService().serverName());
            plugin.getLogger().info("[BROKER] TP cross-server broker ready.");
        } else {
            this.tpaBroker = null;
            this.tpBroker  = null;
            plugin.getLogger().warning("[BROKER] Brokers not started: PacketManager unavailable.");
        }

        if (services(ConfigurationServices.class).getRabbitEnabled() && packetManager != null && packetManager.isInitialized()) {
            try {
                com.rabbitmq.client.ConnectionFactory factory = new com.rabbitmq.client.ConnectionFactory();
                factory.setUri(plugin.getConfig().getString("rabbitmq.uri"));
                com.rabbitmq.client.Connection rabbitConn = factory.newConnection();
                this.backBroker = new BackBroker(plugin, services(PlayerServices.class).getBackService(), rabbitConn);
                this.backBroker.start();
                plugin.getServer().getPluginManager().registerEvents(new BackJoinListener(plugin, services(PlayerServices.class).getBackService()), plugin);
                plugin.getLogger().info("[BackBroker] Cross-server /back broker ready.");
            } catch (Exception e) {
                this.backBroker = null;
                plugin.getLogger().severe("[BackBroker] Failed to initialize: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            this.backBroker = null;
            plugin.getLogger().info("[BackBroker] Disabled (RabbitMQ not available).");
        }

        if (packetManager != null && packetManager.isInitialized() && services(StorageServices.class).getPlayerWarpService() != null && services(ProxyMessagingServices.class).getProxyMessenger() != null) {
            try {
                new fr.elias.oessentials.storage.internal.playerwarp.PlayerWarpCrossServerBroker(plugin, services(StorageServices.class).getPlayerWarpService(), packetManager, services(ProxyMessagingServices.class).getProxyMessenger(), services(ConfigurationServices.class).getConfigService().serverName());
                plugin.getLogger().info("[BROKER] PlayerWarpCrossServerBroker enabled.");
            } catch (Throwable t) {
                plugin.getLogger().warning("[BROKER] Failed to init PlayerWarpCrossServerBroker: " + t.getMessage());
            }
        }

    }

    private void registerAllPacketsDeterministically(PacketManager pm) {
        pm.registerPacket(fr.elias.oessentials.messaging.internal.rabbitmq.packet.impl.SendRemoteMessagePacket.class, fr.elias.oessentials.messaging.internal.rabbitmq.packet.impl.SendRemoteMessagePacket::new);
        pm.registerPacket(fr.elias.oessentials.chat.internal.msg.CrossServerMsgPacket.class, fr.elias.oessentials.chat.internal.msg.CrossServerMsgPacket::new);
        pm.registerPacket(PlayerJoinPacket.class, PlayerJoinPacket::new);
        pm.registerPacket(PlayerQuitPacket.class, PlayerQuitPacket::new);
        pm.registerPacket(fr.elias.oessentials.player.internal.vanish.rabbit.VanishSyncPacket.class, fr.elias.oessentials.player.internal.vanish.rabbit.VanishSyncPacket::new);
        pm.registerPacket(PlayerWarpTeleportRequestPacket.class, PlayerWarpTeleportRequestPacket::new);
        pm.registerPacket(fr.elias.oessentials.rtp.internal.RtpTeleportRequestPacket.class, fr.elias.oessentials.rtp.internal.RtpTeleportRequestPacket::new);
        pm.registerPacket(InvseeOpenRequestPacket.class, InvseeOpenRequestPacket::new);
        pm.registerPacket(InvseeStatePacket.class, InvseeStatePacket::new);
        pm.registerPacket(InvseeEditPacket.class, InvseeEditPacket::new);
        pm.registerPacket(fr.elias.oessentials.messaging.internal.rabbitmq.packet.impl.DeathMessagePacket.class, fr.elias.oessentials.messaging.internal.rabbitmq.packet.impl.DeathMessagePacket::new);
        pm.registerPacket(TradeStartPacket.class, TradeStartPacket::new);
        pm.registerPacket(TradeInvitePacket.class, TradeInvitePacket::new);
        pm.registerPacket(TradeStatePacket.class, TradeStatePacket::new);
        pm.registerPacket(TradeConfirmPacket.class, TradeConfirmPacket::new);
        pm.registerPacket(TradeCancelPacket.class, TradeCancelPacket::new);
        pm.registerPacket(TradeGrantPacket.class, TradeGrantPacket::new);
        pm.registerPacket(TradeClosePacket.class, TradeClosePacket::new);
        pm.registerPacket(TpJumpPacket.class, TpJumpPacket::new);
        pm.registerPacket(BackTeleportPacket.class, BackTeleportPacket::new);
        pm.registerPacket(TpaBringPacket.class, TpaBringPacket::new);
        pm.registerPacket(TpaRequestPacket.class, TpaRequestPacket::new);
        pm.registerPacket(TpaSummonPacket.class, TpaSummonPacket::new);
        pm.registerPacket(TpaAcceptPacket.class, TpaAcceptPacket::new);
        pm.registerPacket(fr.elias.oessentials.currency.internal.rabbitmq.CurrencyTransferPacket.class, fr.elias.oessentials.currency.internal.rabbitmq.CurrencyTransferPacket::new);
        pm.registerPacket(fr.elias.oessentials.currency.internal.rabbitmq.CurrencyUpdatePacket.class, fr.elias.oessentials.currency.internal.rabbitmq.CurrencyUpdatePacket::new);
        pm.registerPacket(fr.elias.oessentials.currency.internal.rabbitmq.CurrencySyncPacket.class, fr.elias.oessentials.currency.internal.rabbitmq.CurrencySyncPacket::new);
        pm.registerPacket(AfkPoolEnterPacket.class, AfkPoolEnterPacket::new);
        pm.registerPacket(AfkPoolExitPacket.class, AfkPoolExitPacket::new);
        pm.registerPacket(fr.elias.oessentials.afk.internal.rabbit.packets.AfkStatusPacket.class, fr.elias.oessentials.afk.internal.rabbit.packets.AfkStatusPacket::new);
        pm.registerPacket(fr.elias.oessentials.inventory.internal.auctionhouse.rabbitmq.AuctionSyncPacket.class, fr.elias.oessentials.inventory.internal.auctionhouse.rabbitmq.AuctionSyncPacket::new);
        pm.registerPacket(fr.elias.oessentials.inventory.internal.orders.rabbitmq.OrderSyncPacket.class, fr.elias.oessentials.inventory.internal.orders.rabbitmq.OrderSyncPacket::new);
        pm.registerPacket(fr.elias.oessentials.portals.internal.rabbit.PortalTeleportPacket.class, fr.elias.oessentials.portals.internal.rabbit.PortalTeleportPacket::new);
        pm.registerPacket(fr.elias.oessentials.mail.internal.rabbitmq.MailDeliveryPacket.class, fr.elias.oessentials.mail.internal.rabbitmq.MailDeliveryPacket::new);
        pm.registerPacket(fr.elias.oessentials.grouprtp.internal.rabbit.GroupRtpSyncPacket.class, fr.elias.oessentials.grouprtp.internal.rabbit.GroupRtpSyncPacket::new);
        plugin.getLogger().info("[RABBIT] Registered 35 packet types deterministically");
    }
}
