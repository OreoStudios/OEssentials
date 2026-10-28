package fr.elias.oessentials.messaging.internal.rabbitmq.packet;

import fr.elias.oessentials.OEssentials;
import fr.elias.oessentials.messaging.api.FriendlyByteOutputStream;
import fr.elias.oessentials.messaging.api.PacketChannels;
import fr.elias.oessentials.messaging.api.SendRemoteMessagePacket;
import fr.elias.oessentials.messaging.internal.rabbitmq.namespace.MessagingPacketNamespace;
import fr.elias.oessentials.messaging.internal.rabbitmq.packet.event.PacketSender;
import fr.elias.oessentials.platform.scheduling.OreScheduler;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class PacketManagerDispatchTest {
    @Test
    void packetsAreNotDispatchedOnceThePluginIsDisabling() {
        OEssentials plugin = mock(OEssentials.class);
        when(plugin.getLogger()).thenReturn(Logger.getAnonymousLogger());
        var manager = new PacketManager(plugin, mock(PacketSender.class), List.of(new MessagingPacketNamespace()));
        var received = new AtomicInteger();
        manager.subscribe(SendRemoteMessagePacket.class, (channel, packet) -> received.incrementAndGet());

        byte[] bytes = encode(manager, new SendRemoteMessagePacket(UUID.randomUUID(), "hi"));

        try (var scheduler = mockStatic(OreScheduler.class)) {
            scheduler.when(() -> OreScheduler.run(eq(plugin), any(Runnable.class))).thenAnswer(call -> {
                call.getArgument(1, Runnable.class).run();
                return null;
            });
            when(plugin.isEnabled()).thenReturn(true);
            manager.onReceive(PacketChannels.GLOBAL, bytes);
            assertEquals(1, received.get(), "delivered while enabled");

            when(plugin.isEnabled()).thenReturn(false);
            manager.onReceive(PacketChannels.GLOBAL, bytes);
            assertEquals(1, received.get(), "dropped once the plugin is disabling");
        }
    }

    private static byte[] encode(PacketManager manager, SendRemoteMessagePacket packet) {
        var out = new FriendlyByteOutputStream();
        out.writeLong(manager.getPacketRegistry().getDefinition(SendRemoteMessagePacket.class).getRegistryId());
        packet.writeData(out);
        return out.toByteArray();
    }
}
