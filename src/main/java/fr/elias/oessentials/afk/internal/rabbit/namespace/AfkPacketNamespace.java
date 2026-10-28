package fr.elias.oessentials.afk.internal.rabbit.namespace;

import fr.elias.oessentials.messaging.api.PacketNamespace;
import fr.elias.oessentials.afk.internal.rabbit.packets.AfkPoolEnterPacket;
import fr.elias.oessentials.afk.internal.rabbit.packets.AfkPoolExitPacket;
import fr.elias.oessentials.afk.internal.rabbit.packets.AfkStatusPacket;

public final class AfkPacketNamespace extends PacketNamespace {

    public static final int AFK_POOL_ENTER_ID = 2001;
    public static final int AFK_POOL_EXIT_ID  = 2002;
    public static final int AFK_STATUS_ID     = 2003;

    public AfkPacketNamespace() {
        super((short) 20);
    }

    @Override
    protected void registerPackets() {
        registerPacket(AFK_POOL_ENTER_ID, AfkPoolEnterPacket.class, AfkPoolEnterPacket::new);
        registerPacket(AFK_POOL_EXIT_ID,  AfkPoolExitPacket.class,  AfkPoolExitPacket::new);
        registerPacket(AFK_STATUS_ID,     AfkStatusPacket.class,    AfkStatusPacket::new);
    }
}
