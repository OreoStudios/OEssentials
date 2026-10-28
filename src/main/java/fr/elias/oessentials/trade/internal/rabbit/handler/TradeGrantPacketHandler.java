package fr.elias.oessentials.trade.internal.rabbit.handler;

import fr.elias.oessentials.OEssentials;
import fr.elias.oessentials.messaging.api.PacketChannel;
import fr.elias.oessentials.messaging.api.PacketSubscriber;
import fr.elias.oessentials.trade.internal.rabbit.packet.TradeGrantPacket;

public final class TradeGrantPacketHandler implements PacketSubscriber<TradeGrantPacket> {
    private final OEssentials plugin;
    public TradeGrantPacketHandler(OEssentials plugin) { this.plugin = plugin; }

    private boolean dbg(){
        try { return OEssentials.get().getTradeService().getConfig().debugDeep; }
        catch (Throwable t){ return false; }
    }

    @Override
    public void onReceive(PacketChannel channel, TradeGrantPacket packet) {
        if (dbg()) plugin.getLogger().info("[TRADE] RECV TradeGrantPacket ch=" + channel +
                " session=" + packet.getSessionId() + " grantTo=" + packet.getGrantTo() +
                " bytes=" + (packet.getItemsBytes()==null?0:packet.getItemsBytes().length));

        if (plugin.getTradeBroker() != null) {
            plugin.getTradeBroker().handleRemoteGrant(packet);
        } else if (dbg()) {
            plugin.getLogger().warning("[TRADE]   TradeBroker is null; cannot handle grant.");
        }
    }
}
