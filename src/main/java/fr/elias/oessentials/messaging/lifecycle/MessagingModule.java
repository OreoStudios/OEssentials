package fr.elias.oessentials.messaging.lifecycle;

import dev.oreo.modulith.core.PluginModule;
import fr.elias.oessentials.configuration.api.SafeZoneRelay;
import fr.elias.oessentials.messaging.internal.rabbitmq.packet.impl.tp.SafeZoneEnterPacket;
import org.bukkit.entity.Player;
import fr.elias.oessentials.messaging.internal.invlook.commands.InvlookCommand;
import fr.elias.oessentials.messaging.internal.invlook.listeners.InvlookListener;
import fr.elias.oessentials.messaging.internal.invlook.manager.InvlookManager;
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
import fr.elias.oessentials.messaging.api.PacketChannels;
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
    private final InvlookManager invlookManager = new InvlookManager();
    private final java.util.List<fr.elias.oessentials.messaging.api.PacketNamespace> namespaces = new java.util.concurrent.CopyOnWriteArrayList<>();
    private final java.util.List<java.util.function.Consumer<PacketManager>> pendingSubscriptions = new java.util.concurrent.CopyOnWriteArrayList<>();

    @Override
    protected void start() {
        cleanup("connectingSender", () -> { if (connectingSender != null) connectingSender.close(); });
        cleanup("backBroker", () -> { if (backBroker != null) backBroker.shutdown(); });
        cleanup("packetManager", () -> { if (packetManager != null) packetManager.close(); });
        cleanup("networkCountReceiver", () -> { if (networkCountReceiver != null) networkCountReceiver.stop(); });
        cleanup("pending-back", pendingBackTeleports::clear);
        cleanup("invlookManager", invlookManager::clear);
        registerNamespaces();
        initInvlook();
        initSafeZoneRelay();
        initRabbitMQ();
    }

    @Override public TpCrossServerBroker getTpBroker() { return tpBroker; }
    @Override public BackBroker getBackBroker() { return backBroker; }
    @Override public fr.elias.oessentials.messaging.internal.network.NetworkCountReceiver getNetworkCountReceiver() { return networkCountReceiver; }
    @Override public TpaCrossServerBroker getTpaBroker() { return tpaBroker; }
    @Override public ModBridge getModBridge() { return modBridge; }
    @Override public InvseeService getInvseeService() { return invseeService; }
    @Override public InvlookManager getInvlookManager() { return invlookManager; }
    @Override public InvseeCrossServerBroker getInvseeBroker() { return invseeBroker; }
    @Override public TradeCrossServerBroker getTradeBroker() { return tradeBroker; }
    @Override public PacketManager getPacketBus() { return packetManager; }

    @Override
    public void registerNamespace(fr.elias.oessentials.messaging.api.PacketNamespace namespace) {
        namespaces.add(namespace);
        PacketManager live = packetManager;
        if (live != null) live.getPacketRegistry().register(namespace);
    }

    @Override
    public <T extends fr.elias.oessentials.messaging.api.Packet> void subscribe(Class<T> packetClass, fr.elias.oessentials.messaging.api.PacketSubscriber<T> subscriber) {
        pendingSubscriptions.add(bus -> bus.subscribe(packetClass, subscriber));
        PacketManager live = packetManager;
        if (live != null) live.subscribe(packetClass, subscriber);
    }

    /** Messaging's own packets, and (until each owner registers its own) everyone else's. */
    private void registerNamespaces() {
        registerNamespace(new fr.elias.oessentials.messaging.internal.oreobotfeatures.rabbit.OreoBotPacketNamespace());
        registerNamespace(new fr.elias.oessentials.messaging.internal.rabbitmq.namespace.RebootPacketNamespace());
        registerNamespace(new fr.elias.oessentials.messaging.internal.cross.rabbit.namespace.CrossInvPacketNamespace());
        registerNamespace(new fr.elias.oessentials.messaging.internal.invsee.rabbit.InvseePacketNamespace());
        registerNamespace(new fr.elias.oessentials.messaging.internal.rabbitmq.namespace.DeathMessagePacketNamespace());
        registerNamespace(new fr.elias.oessentials.afk.internal.rabbit.namespace.AfkPacketNamespace());
        registerNamespace(new fr.elias.oessentials.storage.internal.homes.rabbit.packet.HomesPacketNamespace());
        registerNamespace(new fr.elias.oessentials.storage.internal.warps.rabbit.namespace.WarpsPacketNamespace());
        registerNamespace(new fr.elias.oessentials.storage.internal.spawn.rabbit.namespace.SpawnPacketNamespace());
        registerNamespace(new fr.elias.oessentials.storage.internal.playerwarp.rabbit.PlayerWarpPacketNamespace());
        registerNamespace(new fr.elias.oessentials.trade.internal.rabbit.namespace.TradePacketNamespace());
        registerNamespace(new fr.elias.oessentials.chat.internal.msg.ChatPacketNamespace());
        registerNamespace(new fr.elias.oessentials.player.internal.rabbit.PlayerPacketNamespace());
        registerNamespace(new fr.elias.oessentials.inventory.internal.InventoriesPacketNamespace());
        registerNamespace(new fr.elias.oessentials.mail.internal.rabbitmq.MailPacketNamespace());
    }
    @Override public fr.elias.oessentials.storage.internal.homes.HomeTeleportBroker getHomeTpBroker() { return homeTpBroker; }
    @Override public Map<UUID, BackLocation> getPendingBackTeleports() { return pendingBackTeleports; }

    /** Read-only /invsee; registered whether or not RabbitMQ connects, and checks for the invsee service per use. */
    private void initInvlook() {
        plugin.getServer().getPluginManager().registerEvents(new InvlookListener(plugin), plugin);
        var commands = services(ConfigurationServices.class).getCommands();
        commands.register(new InvlookCommand());
        commands.rewireTab("invlook", new InvlookCommand());
    }

    /** Auto-reboot (configuration) sends players to its safe-zone server through messaging. */
    private void initSafeZoneRelay() {
        var configuration = services(ConfigurationServices.class);
        configuration.setSafeZoneRelay(new SafeZoneRelay() {
            @Override public boolean packetsReady() { return packetManager != null && packetManager.isInitialized(); }
            @Override public void requestSafeZone(UUID playerId, String targetServer, String worldName, String regionName) {
                packetManager.sendPacket(new SafeZoneEnterPacket(playerId, targetServer, worldName, regionName));
            }
            @Override public void connect(Player player, String targetServer) {
                services(ProxyMessagingServices.class).getProxyMessenger().sendToServer(player, targetServer);
            }
        });
        cleanup("safeZoneRelay", () -> configuration.setSafeZoneRelay(null));
    }

    private void initRabbitMQ() {
        final boolean invSyncEnabled = services(ConfigurationServices.class).getFeatureSettings().featureOption("cross-server", "invsee", true);

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
        final String serverName = services(ConfigurationServices.class).getServerConfig().serverName();

        fr.elias.oessentials.platform.scheduling.OreScheduler.runAsync(plugin, () -> {
            if (!isActive()) return;
            RabbitMQSender rabbit = new RabbitMQSender(uri, serverName, services(ConfigurationServices.class).getServerConfig()::isDebugEnabled);
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

                PacketManager bus = new PacketManager(plugin, rabbit, namespaces);
                pendingSubscriptions.forEach(subscription -> subscription.accept(bus));
                this.packetManager = bus;

                try {
                    this.networkCountReceiver = new fr.elias.oessentials.messaging.internal.network.NetworkCountReceiver(plugin, uri);
                    if (this.networkCountReceiver.start()) { plugin.getLogger().info("[NetworkCountReceiver] Listening for proxy player counts."); }
                    else { plugin.getLogger().warning("[NetworkCountReceiver] Failed to start."); this.networkCountReceiver = null; }
                } catch (Throwable t) { plugin.getLogger().warning("[NetworkCountReceiver] Error: " + t.getMessage()); this.networkCountReceiver = null; }

                if (invSyncEnabled) {
                    try {
                        this.invseeBroker  = new InvseeCrossServerBroker(plugin, this.packetManager, services(ConfigurationServices.class).getServerConfig().serverName(), null);
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
                this.packetManager.subscribeChannel(fr.elias.oessentials.messaging.api.PacketChannel.individual(services(ConfigurationServices.class).getServerConfig().serverName()));
                plugin.getLogger().info("[RABBIT] Subscribed channels: global + individual(" + services(ConfigurationServices.class).getServerConfig().serverName() + ")");

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

                this.packetManager.subscribe(SafeZoneEnterPacket.class, (channel, pkt) -> {
                    var autoReboot = services(ConfigurationServices.class).getAutoReboot();
                    if (pkt != null && autoReboot != null) {
                        autoReboot.acceptSafeZoneEnter(pkt.getPlayerId(), pkt.getTargetServer(), pkt.getWorldName(), pkt.getRegionName());
                    }
                });

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
            this.modBridge = new ModBridge(plugin, packetManager, services(ConfigurationServices.class).getServerConfig().serverName());
            plugin.getLogger().info("[MOD-BRIDGE] Cross-server moderation bridge ready.");
        } else {
            this.modBridge = null;
            plugin.getLogger().info("[MOD-BRIDGE] Disabled (PacketManager unavailable).");
        }

        if (packetManager != null && packetManager.isInitialized() && services(TradeServices.class).getTradeService() != null && services(ConfigurationServices.class).getFeatureSettings().tradeCrossServerEnabled()) {
            this.tradeBroker = new TradeCrossServerBroker(plugin, packetManager, services(ConfigurationServices.class).getServerConfig().serverName(), services(TradeServices.class).getTradeService());
            plugin.getLogger().info("[TRADE] Cross-server trade broker ready.");
        } else {
            this.tradeBroker = null;
            plugin.getLogger().info("[TRADE] Cross-server trade broker disabled.");
        }

        if (packetManager != null && packetManager.isInitialized()) {
            final var cs = plugin.getCrossServerFeatures();
            if (cs.spawn() || cs.warps()) {
                new CrossServerTeleportBroker(plugin, services(StorageServices.class).getSpawnService(), services(StorageServices.class).getWarpService(), packetManager, services(ConfigurationServices.class).getServerConfig().serverName());
                plugin.getLogger().info("[BROKER] CrossServerTeleportBroker ready.");
            }
            if (cs.homes()) {
                this.homeTpBroker = new fr.elias.oessentials.storage.internal.homes.HomeTeleportBroker(plugin, services(StorageServices.class).getHomeService(), packetManager);
                plugin.getLogger().info("[BROKER] HomeTeleportBroker ready.");
            }

            this.tpaBroker = new TpaCrossServerBroker(plugin, services(PlayerServices.class).getTeleportService(), this.packetManager, services(ProxyMessagingServices.class).getProxyMessenger(), services(ConfigurationServices.class).getServerConfig().serverName());
            plugin.getLogger().info("[BROKER] TPA cross-server broker ready.");

            this.tpBroker = new TpCrossServerBroker(plugin, services(PlayerServices.class).getTeleportService(), this.packetManager, services(ProxyMessagingServices.class).getProxyMessenger(), services(ConfigurationServices.class).getServerConfig().serverName());
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
                new fr.elias.oessentials.storage.internal.playerwarp.PlayerWarpCrossServerBroker(plugin, services(StorageServices.class).getPlayerWarpService(), packetManager, services(ProxyMessagingServices.class).getProxyMessenger(), services(ConfigurationServices.class).getServerConfig().serverName());
                plugin.getLogger().info("[BROKER] PlayerWarpCrossServerBroker enabled.");
            } catch (Throwable t) {
                plugin.getLogger().warning("[BROKER] Failed to init PlayerWarpCrossServerBroker: " + t.getMessage());
            }
        }

    }
}
