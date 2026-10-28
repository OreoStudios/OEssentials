package fr.elias.oessentials.messaging.internal.rabbitmq.namespace;

import fr.elias.oessentials.messaging.api.PacketNamespace;

public final class DynamicPacketNamespace extends PacketNamespace {

    public static final short ID = (short) 32000;

    public DynamicPacketNamespace() {
        super(ID);
    }

    @Override
    protected void registerPackets() {
    }
}
