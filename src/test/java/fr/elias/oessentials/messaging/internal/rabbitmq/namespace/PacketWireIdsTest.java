package fr.elias.oessentials.messaging.internal.rabbitmq.namespace;

import fr.elias.oessentials.messaging.api.PacketDefinition;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Packet ids travel on the wire, so servers on different builds must agree on them.
 * Moving packet classes or namespaces between modules must not change this table.
 */
class PacketWireIdsTest {
    @Test
    void fixedIdsAndFirstDynamicIdAreStable() throws Exception {
        PacketRegistry registry = new PacketRegistry();
        assertEquals(EXPECTED_IDS, describe(registry));
        assertEquals(EXPECTED_FIRST_DYNAMIC_ID, nextDynamicId(registry));
    }

    /** Startup also registers packets without a namespace; their ids follow on from the fixed ones. */
    @Test
    void idsAfterStartupRegistrationAreStable() throws Exception {
        var plugin = org.mockito.Mockito.mock(fr.elias.oessentials.OEssentials.class);
        org.mockito.Mockito.when(plugin.getLogger()).thenReturn(java.util.logging.Logger.getAnonymousLogger());
        var manager = new fr.elias.oessentials.messaging.internal.rabbitmq.packet.PacketManager(plugin,
                org.mockito.Mockito.mock(fr.elias.oessentials.messaging.internal.rabbitmq.packet.event.PacketSender.class));
        var module = new fr.elias.oessentials.messaging.lifecycle.MessagingModule();
        Field pluginField = fr.elias.oessentials.platform.modularity.ManagedModule.class.getDeclaredField("plugin");
        pluginField.setAccessible(true);
        pluginField.set(module, plugin);
        var register = module.getClass().getDeclaredMethod("registerAllPacketsDeterministically",
                fr.elias.oessentials.messaging.internal.rabbitmq.packet.PacketManager.class);
        register.setAccessible(true);
        register.invoke(module, manager);
        assertEquals(EXPECTED_IDS + "\n" + EXPECTED_STARTUP_IDS, describe(manager.getPacketRegistry()));
    }

    static String describe(PacketRegistry registry) throws Exception {
        Field field = PacketRegistry.class.getDeclaredField("idToDefinition");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<Long, PacketDefinition<?>> byId = (Map<Long, PacketDefinition<?>>) field.get(registry);
        return new TreeMap<>(byId).entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue().getNamespace().getNamespaceId() + "/" + e.getValue().getPacketClass().getSimpleName())
                .collect(Collectors.joining("\n"));
    }

    static int nextDynamicId(PacketRegistry registry) throws Exception {
        Field field = PacketRegistry.class.getDeclaredField("nextDynamicId");
        field.setAccessible(true);
        return ((AtomicInteger) field.get(registry)).get();
    }

    private static final int EXPECTED_FIRST_DYNAMIC_ID = 27006;
    private static final String EXPECTED_STARTUP_IDS = """
            27006=32000/CrossServerMsgPacket
            27007=32000/VanishSyncPacket
            27008=32000/PlayerWarpTeleportRequestPacket
            27009=32000/RtpTeleportRequestPacket
            27010=32000/InvseeOpenRequestPacket
            27011=32000/InvseeStatePacket
            27012=32000/InvseeEditPacket
            27013=32000/DeathMessagePacket
            27014=32000/TradeClosePacket
            27015=32000/TpJumpPacket
            27016=32000/BackTeleportPacket
            27017=32000/TpaBringPacket
            27018=32000/TpaRequestPacket
            27019=32000/TpaSummonPacket
            27020=32000/TpaAcceptPacket
            27021=32000/CurrencyTransferPacket
            27022=32000/CurrencyUpdatePacket
            27023=32000/CurrencySyncPacket
            27024=32000/AuctionSyncPacket
            27025=32000/OrderSyncPacket
            27026=32000/PortalTeleportPacket
            27027=32000/MailDeliveryPacket
            27028=32000/GroupRtpSyncPacket""";
    private static final String EXPECTED_IDS = """
            0=1/PlayerJoinPacket
            1=1/PlayerQuitPacket
            2=1/SendRemoteMessagePacket
            1001=10/HomeTeleportRequestPacket
            1002=10/OtherHomeTeleportRequestPacket
            1003=12/SpawnTeleportRequestPacket
            1004=11/WarpTeleportRequestPacket
            2001=20/AfkPoolEnterPacket
            2002=20/AfkPoolExitPacket
            2003=20/AfkStatusPacket
            2101=21/SafeZoneEnterPacket
            9101=91/CrossInvPacket
            27000=27/TradeStartPacket
            27001=27/TradeInvitePacket
            27002=27/TradeStatePacket
            27003=27/TradeConfirmPacket
            27004=27/TradeCancelPacket
            27005=27/TradeGrantPacket""";
}
