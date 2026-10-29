package fr.elias.oessentials.messaging.internal.rabbitmq.handler;

import fr.elias.oessentials.OEssentials;
import fr.elias.oessentials.messaging.api.PacketChannel;
import fr.elias.oessentials.messaging.api.PacketSubscriber;
import fr.elias.oessentials.messaging.api.SendRemoteMessagePacket;
import fr.elias.oessentials.platform.scheduling.OreScheduler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class RemoteMessagePacketHandler implements PacketSubscriber<SendRemoteMessagePacket> {

    @Override
    public void onReceive(PacketChannel channel, SendRemoteMessagePacket packet) {
        UUID targetId = packet.getTargetId();
        String message = packet.getMessage();

        if (message == null || message.trim().isEmpty()) {
            Bukkit.getLogger().warning("[OEssentials] ⚠ Received invalid SendRemoteMessagePacket: message is null/empty.");
            return;
        }

        // null UUID = broadcast to all online players on this server
        if (targetId == null) {
            OreScheduler.run(OEssentials.get(), () -> {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.sendMessage(message);
                }
            });
            return;
        }

        Player player = Bukkit.getPlayer(targetId);

        if (player != null && player.isOnline()) {
            OreScheduler.runForEntity(OEssentials.get(), player, () -> {
                player.sendMessage(message);
                Bukkit.getLogger().info("[OEssentials] ✓ Delivered remote message to " + player.getName() + ": " + message);
            });
        } else {
            Bukkit.getLogger().info("[OEssentials] ⚠ Player not on this server: " + targetId
                    + " (normal if player is on different server)");
        }
    }
}