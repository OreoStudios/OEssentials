package fr.elias.oessentials.player.internal.back.rabbit;

import fr.elias.oessentials.OEssentials;
import fr.elias.oessentials.player.internal.back.BackLocation;
import fr.elias.oessentials.player.internal.back.rabbit.packets.BackTeleportPacket;
import fr.elias.oessentials.messaging.api.PacketChannel;
import fr.elias.oessentials.messaging.api.PacketBus;
import fr.elias.oessentials.messaging.api.PacketSubscriber;
import fr.elias.oessentials.player.internal.tp.service.TeleportService;
import fr.elias.oessentials.shared.Lang;
import fr.elias.oessentials.configuration.api.ServerConnector;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BackCrossServerBroker implements Listener {

    private final OEssentials plugin;
    private final TeleportService teleportService;
    private final PacketBus packetManager;
    private final ServerConnector proxyMessenger;
    private final String localServer;

    private final Map<UUID, BackLocation> pendingBacks = new ConcurrentHashMap<>();

    public BackCrossServerBroker(OEssentials plugin,
                                 TeleportService teleportService,
                                 PacketBus packetManager,
                                 ServerConnector proxyMessenger,
                                 String localServer) {
        this.plugin = plugin;
        this.teleportService = teleportService;
        this.packetManager = packetManager;
        this.proxyMessenger = proxyMessenger;
        this.localServer = localServer;

        Bukkit.getPluginManager().registerEvents(this, plugin);

        packetManager.subscribe(
                BackTeleportPacket.class,
                (PacketSubscriber<BackTeleportPacket>) (channel, pkt) -> handleIncomingBack(pkt)
        );
    }


    public void requestCrossServerBack(Player player, BackLocation loc) {
        if (player == null || loc == null) return;
        if (packetManager == null || !packetManager.isInitialized()) {
            Lang.send(player, "teleport.back.cross-server.no-messaging",
                    "<red>Cross-server messaging is not available; cannot /back across servers.</red>");
            return;
        }
        if (loc.getServer() == null || loc.getServer().isBlank()) {
            Lang.send(player, "teleport.back.cross-server.no-server",
                    "<red>Back location has no target server.</red>");
            return;
        }

        BackTeleportPacket pkt = new BackTeleportPacket(
                player.getUniqueId(),
                loc.getServer(),
                loc.getWorldName(),
                loc.getX(),
                loc.getY(),
                loc.getZ(),
                loc.getYaw(),
                loc.getPitch()
        );

        packetManager.sendPacket(
                PacketChannel.individual(loc.getServer()),
                pkt
        );

        proxyMessenger.connect(player, loc.getServer());

        Lang.send(player, "teleport.back.cross-server.jumping",
                "<gray>Teleporting back to your previous location on <aqua>%server%</aqua>…</gray>",
                Map.of("server", loc.getServer()));
    }

    private void handleIncomingBack(BackTeleportPacket pkt) {
        if (pkt == null) return;

        if (!localServer.equalsIgnoreCase(pkt.getServer())) {
            return;
        }

        UUID playerId = pkt.getPlayerUuid();
        if (playerId == null) return;

        BackLocation backLoc = new BackLocation(
                pkt.getServer(),
                pkt.getWorld(),
                pkt.getX(),
                pkt.getY(),
                pkt.getZ(),
                pkt.getYaw(),
                pkt.getPitch()
        );

        Player p = Bukkit.getPlayer(playerId);
        if (p != null && p.isOnline()) {
            teleportToBackNow(p, backLoc);
        } else {
            pendingBacks.put(playerId, backLoc);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        BackLocation loc = pendingBacks.remove(p.getUniqueId());
        if (loc == null) return;

        teleportToBackNow(p, loc);
    }

    private void teleportToBackNow(Player p, BackLocation loc) {
        World w = Bukkit.getWorld(loc.getWorldName());
        if (w == null) {
            Lang.send(p, "teleport.back.cross-server.world-not-loaded",
                    "<red>Teleport failed: world <yellow>%world%</yellow> is not loaded.</red>",
                    Map.of("world", loc.getWorldName()));
            return;
        }

        Location bLoc = new Location(w, loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
        teleportService.teleportSilently(p, bLoc);

        Lang.send(p, "teleport.back.cross-server.success",
                "<green>Teleported back.</green>");
    }
}