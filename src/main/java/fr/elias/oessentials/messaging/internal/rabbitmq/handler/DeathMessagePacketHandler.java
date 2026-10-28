package fr.elias.oessentials.messaging.internal.rabbitmq.handler;

import fr.elias.oessentials.OEssentials;
import fr.elias.oessentials.messaging.api.PacketChannel;
import fr.elias.oessentials.messaging.api.PacketSubscriber;
import fr.elias.oessentials.messaging.internal.rabbitmq.packet.impl.DeathMessagePacket;
import fr.elias.oessentials.platform.scheduling.OreScheduler;
import org.bukkit.Bukkit;

public class DeathMessagePacketHandler implements PacketSubscriber<DeathMessagePacket> {

    private final OEssentials plugin;

    public DeathMessagePacketHandler(OEssentials plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onReceive(PacketChannel channel, DeathMessagePacket packet) {
        if (packet == null) return;

        String message = packet.getMessage();
        String sourceServer = packet.getSourceServer();
        String deadPlayerName = packet.getDeadPlayerName();

        if (message == null || message.isEmpty()) {
            plugin.getLogger().warning("[Rabbit] DeathMessagePacket missing message data");
            return;
        }

        String localServer = plugin.getServerConfig().serverName();

        if (sourceServer != null && sourceServer.equalsIgnoreCase(localServer)) {
            plugin.getLogger().fine("[Rabbit] Death @" + channel + " -> " + deadPlayerName + " (local server, skipping broadcast)");
            return;
        }

        plugin.getLogger().fine("[Rabbit] Death @" + channel + " from=" + sourceServer + " -> " + deadPlayerName);

        try {
            OreScheduler.run(plugin, () -> {
                Bukkit.broadcastMessage(message);
            });
        } catch (Throwable t) {
            plugin.getLogger().warning("[Rabbit] Failed to broadcast death message: " + t.getMessage());
        }
    }
}