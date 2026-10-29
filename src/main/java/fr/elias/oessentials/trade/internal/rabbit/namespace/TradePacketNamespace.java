package fr.elias.oessentials.trade.internal.rabbit.namespace;

import fr.elias.oessentials.messaging.api.PacketNamespace;
import fr.elias.oessentials.trade.internal.cross.CrossTradeSignalPacket;

/**
 * Trade packets. Ids 27000-27005 and 27014 belonged to the removed packet-based broker and stay
 * reserved: never reuse them, servers on older versions may still send them.
 */
public final class TradePacketNamespace extends PacketNamespace {

    public static final short NS_ID = (short) 27;

    /** Cross-server trade notifications (the trade record lives in MongoDB). */
    public static final int CROSS_TRADE_SIGNAL_ID = 27029;

    public TradePacketNamespace() {
        super(NS_ID);
    }

    @Override
    protected void registerPackets() {
        registerPacket(CROSS_TRADE_SIGNAL_ID, CrossTradeSignalPacket.class, CrossTradeSignalPacket::new);
    }
}
