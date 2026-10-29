package fr.elias.oessentials.player.internal.msg;

import fr.elias.oessentials.OEssentials;
import fr.elias.oessentials.player.internal.services.MessageService;
import fr.elias.oessentials.messaging.api.PacketChannel;
import fr.elias.oessentials.platform.scheduling.OreScheduler;
import fr.elias.oessentials.messaging.api.PacketSubscriber;
import fr.elias.oessentials.shared.Lang;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;

public class CrossServerMsgHandler implements PacketSubscriber<CrossServerMsgPacket> {

    private final MessageService messageService;

    public CrossServerMsgHandler(MessageService messageService) {
        this.messageService = messageService;
    }

    @Override
    public void onReceive(PacketChannel channel, CrossServerMsgPacket packet) {
        OreScheduler.run(OEssentials.get(), () -> {
            Player target = Bukkit.getPlayer(packet.getTargetUuid());
            if (target == null || !target.isOnline()) return;

            Lang.send(target, "msg.receive",
                    "<gray>[<light_purple>MSG</light_purple>] <aqua>%sender%</aqua>: <white>%message%</white></gray>",
                    Map.of("sender", packet.getSenderName(), "message", packet.getMessage()));

            // Record so the target can /reply back cross-server
            messageService.record(packet.getSenderUuid(), packet.getTargetUuid());
        });
    }
}
