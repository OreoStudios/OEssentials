package fr.elias.oessentials.messaging.internal.rabbitmq.namespace;

import fr.elias.oessentials.messaging.api.PacketDefinition;
import fr.elias.oessentials.messaging.api.PacketNamespace;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Packet ids travel on the wire (the namespace id does not), so servers on different builds must
 * agree on them. Until 7.1.6 the last 23 were numbered by startup registration order from 27006;
 * they now have those numbers as fixed ids. Moving packets between modules must not change this table.
 */
class PacketWireIdsTest {
    private static final Path SOURCES = Path.of("src/main/java");

    @Test
    void everyPacketKeepsItsWireId() throws Exception {
        assertEquals(EXPECTED_IDS, describe(new PacketRegistry(registeredNamespaces())));
    }

    @Test
    void everyNamespaceInTheCodebaseIsRegistered() throws Exception {
        var registered = registeredNamespaces().stream().map(ns -> ns.getClass().getName()).sorted().toList();
        var declared = new ArrayList<String>();
        Pattern subclass = Pattern.compile("class (\\w+) extends PacketNamespace\\b");
        Pattern pkg = Pattern.compile("^package ([\\w.]+);", Pattern.MULTILINE);
        try (Stream<Path> files = Files.walk(SOURCES)) {
            for (Path file : files.filter(f -> f.toString().endsWith(".java")).toList()) {
                String source = Files.readString(file);
                var match = subclass.matcher(source);
                if (match.find() && !match.group(1).equals("DynamicPacketNamespace")) {
                    var p = pkg.matcher(source);
                    p.find();
                    declared.add(p.group(1) + "." + match.group(1));
                }
            }
        }
        assertEquals(declared.stream().sorted().toList(), registered,
                "Every PacketNamespace must be passed to MessagingServices.registerNamespace");
    }

    /** What messaging registers on startup. */
    @SuppressWarnings("unchecked")
    static List<PacketNamespace> registeredNamespaces() throws Exception {
        var module = new fr.elias.oessentials.messaging.lifecycle.MessagingModule();
        var register = module.getClass().getDeclaredMethod("registerNamespaces");
        register.setAccessible(true);
        register.invoke(module);
        Field field = module.getClass().getDeclaredField("namespaces");
        field.setAccessible(true);
        return List.copyOf((List<PacketNamespace>) field.get(module));
    }

    static String describe(PacketRegistry registry) throws Exception {
        Field field = PacketRegistry.class.getDeclaredField("idToDefinition");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<Long, PacketDefinition<?>> byId = (Map<Long, PacketDefinition<?>>) field.get(registry);
        return new TreeMap<>(byId).entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue().getPacketClass().getSimpleName())
                .collect(Collectors.joining("\n"));
    }

    private static final String EXPECTED_IDS = """
            0=PlayerJoinPacket
            1=PlayerQuitPacket
            2=SendRemoteMessagePacket
            1001=HomeTeleportRequestPacket
            1002=OtherHomeTeleportRequestPacket
            1003=SpawnTeleportRequestPacket
            1004=WarpTeleportRequestPacket
            2001=AfkPoolEnterPacket
            2002=AfkPoolExitPacket
            2003=AfkStatusPacket
            2101=SafeZoneEnterPacket
            9101=CrossInvPacket
            27000=TradeStartPacket
            27001=TradeInvitePacket
            27002=TradeStatePacket
            27003=TradeConfirmPacket
            27004=TradeCancelPacket
            27005=TradeGrantPacket
            27006=CrossServerMsgPacket
            27007=VanishSyncPacket
            27008=PlayerWarpTeleportRequestPacket
            27009=RtpTeleportRequestPacket
            27010=InvseeOpenRequestPacket
            27011=InvseeStatePacket
            27012=InvseeEditPacket
            27013=DeathMessagePacket
            27014=TradeClosePacket
            27015=TpJumpPacket
            27016=BackTeleportPacket
            27017=TpaBringPacket
            27018=TpaRequestPacket
            27019=TpaSummonPacket
            27020=TpaAcceptPacket
            27021=CurrencyTransferPacket
            27022=CurrencyUpdatePacket
            27023=CurrencySyncPacket
            27024=AuctionSyncPacket
            27025=OrderSyncPacket
            27026=PortalTeleportPacket
            27027=MailDeliveryPacket
            27028=GroupRtpSyncPacket""";
}
