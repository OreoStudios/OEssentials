package fr.elias.oessentials.player.lifecycle;

import dev.oreo.modulith.core.ModuleListener;
import dev.oreo.modulith.core.PluginModule;
import fr.elias.oessentials.messaging.lifecycle.MessagingReady;
import fr.elias.oessentials.messaging.lifecycle.MessagingServices;
import fr.elias.oessentials.player.internal.back.BackLocation;
import fr.elias.oessentials.player.internal.back.listeners.BackJoinListener;
import fr.elias.oessentials.player.internal.back.rabbit.BackBroker;
import fr.elias.oessentials.player.internal.msg.ChatPacketNamespace;
import fr.elias.oessentials.player.internal.msg.CrossServerMsgHandler;
import fr.elias.oessentials.player.internal.msg.CrossServerMsgPacket;
import fr.elias.oessentials.player.internal.rabbit.PlayerPacketNamespace;
import fr.elias.oessentials.player.internal.tp.rabbit.brokers.TpCrossServerBroker;
import fr.elias.oessentials.player.internal.tp.rabbit.brokers.TpaCrossServerBroker;
import fr.elias.oessentials.player.internal.vanish.rabbit.VanishSyncPacket;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import fr.elias.oessentials.platform.modularity.ManagedModule;
import fr.elias.oessentials.configuration.lifecycle.ConfigurationServices;
import fr.elias.oessentials.storage.lifecycle.StorageServices;
import fr.elias.oessentials.player.internal.listeners.GodListener;
import fr.elias.oessentials.player.internal.back.service.BackService;
import fr.elias.oessentials.player.internal.listeners.PlayerTrackingListener;
import fr.elias.oessentials.player.internal.deathback.DeathBackListener;
import fr.elias.oessentials.player.internal.deathback.DeathBackService;
import fr.elias.oessentials.player.internal.tp.service.TeleportService;
import fr.elias.oessentials.player.internal.services.GodService;
import fr.elias.oessentials.player.internal.services.MessageService;
import fr.elias.oessentials.player.internal.services.VanishService;

@PluginModule(value = "player-services", dependencies = {"configuration::services", "ignore::services", "messaging::events", "messaging::services", "storage::services"}, after = {"inventory-sync"})
public final class PlayerServicesModule extends ManagedModule implements PlayerServices {
    private TeleportService teleportService;
    private BackService backService;
    private MessageService messageService;
    private DeathBackService deathBackService;
    private GodService godService;
    // Temporary holder so initCommands can access vanishService
    private transient VanishService _vanishService;
    private TpaCrossServerBroker tpaBroker;
    private TpCrossServerBroker tpBroker;
    private BackBroker backBroker;
    private final Map<UUID, BackLocation> pendingBackTeleports = new ConcurrentHashMap<>();

    @Override
    protected void start() {
        cleanup("teleportService", () -> { if (teleportService != null) teleportService.shutdown(); });
        cleanup("backBroker", () -> { if (backBroker != null) backBroker.shutdown(); });
        cleanup("pending-back", pendingBackTeleports::clear);
        var messaging = services(MessagingServices.class);
        messaging.registerNamespace(new PlayerPacketNamespace());
        messaging.registerNamespace(new ChatPacketNamespace());
        initCoreServices();
        messaging.subscribe(CrossServerMsgPacket.class, new CrossServerMsgHandler(messageService));
        messaging.subscribe(VanishSyncPacket.class,
                (channel, pkt) -> _vanishService.handleRemoteState(pkt.getPlayerId(), pkt.isVanished(), pkt.getSourceServer()));
        listenForModuleEvents(this);
        registerCommands();
        providePublicApi(fr.elias.oessentials.api.ITeleportAPI.class, this::getTeleportApi);
        providePublicApi(fr.elias.oessentials.api.IBackAPI.class, this::getBackApi);
        providePublicApi(fr.elias.oessentials.api.IDeathBackAPI.class, this::getDeathBackApi);
        providePublicApi(fr.elias.oessentials.api.IVanishAPI.class, this::getVanishApi);
    }

    /** Cross-server /tpa, /tp and /back, once RabbitMQ is connected. */
    @ModuleListener
    public void onMessagingReady(MessagingReady event) {
        if (!isActive()) return;
        String serverName = services(ConfigurationServices.class).getServerConfig().serverName();
        this.tpaBroker = new TpaCrossServerBroker(plugin, teleportService, event.packets(), services(fr.elias.oessentials.configuration.lifecycle.ConfigurationServices.class).getServerConnector(), serverName);
        plugin.getLogger().info("[BROKER] TPA cross-server broker ready.");
        this.tpBroker = new TpCrossServerBroker(plugin, teleportService, event.packets(), services(fr.elias.oessentials.configuration.lifecycle.ConfigurationServices.class).getServerConnector(), serverName);
        plugin.getLogger().info("[BROKER] TP cross-server broker ready.");

        try {
            com.rabbitmq.client.ConnectionFactory factory = new com.rabbitmq.client.ConnectionFactory();
            factory.setUri(plugin.getConfig().getString("rabbitmq.uri"));
            com.rabbitmq.client.Connection rabbitConn = factory.newConnection();
            this.backBroker = new BackBroker(plugin, backService, rabbitConn);
            this.backBroker.start();
            plugin.getServer().getPluginManager().registerEvents(new BackJoinListener(plugin, backService), plugin);
            plugin.getLogger().info("[BackBroker] Cross-server /back broker ready.");
        } catch (Exception e) {
            this.backBroker = null;
            plugin.getLogger().severe("[BackBroker] Failed to initialize: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override public TeleportService getTeleportService() { return teleportService; }
    @Override public BackService getBackService() { return backService; }
    @Override public MessageService getMessageService() { return messageService; }
    @Override public DeathBackService getDeathBackService() { return deathBackService; }
    @Override public GodService getGodService() { return godService; }
    @Override public VanishService getVanishService() { return _vanishService; }
    @Override public TpaCrossServerBroker getTpaBroker() { return tpaBroker; }
    @Override public TpCrossServerBroker getTpBroker() { return tpBroker; }
    @Override public BackBroker getBackBroker() { return backBroker; }
    @Override public Map<UUID, BackLocation> getPendingBackTeleports() { return pendingBackTeleports; }

    private void initCoreServices() {
        this.backService      = new BackService(services(StorageServices.class).getBackDataStore());
        this.messageService   = new MessageService();
        this.teleportService  = new TeleportService(plugin, backService, services(ConfigurationServices.class).getServerConfig());
        this.deathBackService = new DeathBackService();
        this.godService       = new GodService();

        boolean vanishCrossServer = services(ConfigurationServices.class).getFeatureSettings().featureOption("cross-server", "vanish", true);
        fr.elias.oessentials.player.internal.services.vanish.VanishStateStorage vanishStorage =
                new fr.elias.oessentials.player.internal.services.vanish.YamlVanishStateStorage(plugin);

        if (vanishCrossServer && services(StorageServices.class).getHomesMongoClient() != null) {
            String dbName = plugin.getConfig().getString("storage.mongo.database", "oreo");
            String prefix = plugin.getConfig().getString("storage.mongo.collectionPrefix", "oreo_");
            vanishStorage = new fr.elias.oessentials.player.internal.services.vanish.MongoVanishStateStorage(services(StorageServices.class).getHomesMongoClient(), dbName, prefix);
            plugin.getLogger().info("[VANISH] Using MongoDB storage for persistent cross-server state.");
        } else {
            plugin.getLogger().info("[VANISH] Using local YAML storage for persistent state.");
        }

        VanishService vanishService = new VanishService(plugin, vanishStorage, vanishCrossServer, services(ConfigurationServices.class).getServerConfig().serverName());
        plugin.getServer().getPluginManager().registerEvents(new fr.elias.oessentials.player.internal.listeners.VanishListener(vanishService, plugin), plugin);
        plugin.getServer().getPluginManager().registerEvents(new PlayerTrackingListener(backService), plugin);
        plugin.getServer().getPluginManager().registerEvents(new DeathBackListener(deathBackService), plugin);
        plugin.getServer().getPluginManager().registerEvents(new GodListener(godService), plugin);
        plugin.getServer().getPluginManager().registerEvents(new fr.elias.oessentials.rtp.internal.listeners.DeathRespawnListener(plugin), plugin);
        vanishService.restoreOnlinePlayers();

        // store vanishService ref for commands block
        this._vanishService = vanishService;
    }

    /** Commands this module owns (registered by the commands module until 7.1.6). */
    private void registerCommands() {
        var commands = services(fr.elias.oessentials.configuration.lifecycle.ConfigurationServices.class).getCommands();
        var tphere = new fr.elias.oessentials.player.internal.tp.command.TphereCommand(plugin);
        commands.register(tphere);
        commands.rewireTab("tphere", tphere);
        var msg = new fr.elias.oessentials.player.internal.msg.MsgCommand(messageService, plugin);
        var ignore = services(fr.elias.oessentials.configuration.lifecycle.ConfigurationServices.class).getPublicApis().get(fr.elias.oessentials.api.IIgnoreAPI.class);
        if (ignore != null) msg.setIgnoreService(ignore);
        commands
                .register(new fr.elias.oessentials.player.internal.back.command.BackCommand(backService, teleportService, plugin))
                .register(new fr.elias.oessentials.player.internal.tp.command.TpaCommand(teleportService))
                .register(new fr.elias.oessentials.player.internal.tp.command.TpAcceptCommand(teleportService))
                .register(new fr.elias.oessentials.player.internal.tp.command.TpDenyCommand(teleportService))
                .register(msg)
                .register(new fr.elias.oessentials.player.internal.msg.ReplyCommand(messageService, plugin))
                .register(new fr.elias.oessentials.player.internal.deathback.DeathBackCommand(deathBackService))
                .register(new fr.elias.oessentials.player.internal.commands.core.moderation.GodCommand(godService))
                .register(new fr.elias.oessentials.player.internal.commands.core.moderation.VanishCommand(_vanishService))
                .register(new fr.elias.oessentials.player.internal.tp.command.TpCommand(teleportService))
                .register(new fr.elias.oessentials.player.internal.tp.command.MoveCommand(teleportService));
        var tpaCompleter = new fr.elias.oessentials.player.internal.tp.completer.TpaTabCompleter(plugin);
        commands.rewireTab("tpa", tpaCompleter);
        commands.rewireTab("move", tpaCompleter);
        commands.rewireTab("tp", new fr.elias.oessentials.player.internal.tp.completer.TpTabCompleter(plugin));
    }
}
