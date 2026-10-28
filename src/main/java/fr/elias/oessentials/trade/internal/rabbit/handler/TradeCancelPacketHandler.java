package fr.elias.oessentials.trade.internal.rabbit.handler;

import fr.elias.oessentials.OEssentials;
import fr.elias.oessentials.messaging.api.PacketChannel;
import fr.elias.oessentials.messaging.api.PacketSubscriber;
import fr.elias.oessentials.trade.internal.rabbit.packet.TradeCancelPacket;

public final class TradeCancelPacketHandler implements PacketSubscriber<TradeCancelPacket> {
    private final OEssentials plugin;
    public TradeCancelPacketHandler(OEssentials plugin) { this.plugin = plugin; }

    private boolean dbg(){
        try { return OEssentials.get().getTradeService().getConfig().debugDeep; }
        catch (Throwable t){ return false; }
    }

    @Override
    public void onReceive(PacketChannel channel, TradeCancelPacket packet) {
        if (dbg()) plugin.getLogger().info("[TRADE] RECV TradeCancelPacket ch=" + channel +
                " session=" + packet.getSessionId() + " reason=\"" + packet.getReason() + "\"");

        if (plugin.getTradeBroker() != null) {
            plugin.getTradeBroker().handleRemoteCancel(packet);
        } else if (dbg()) {
            plugin.getLogger().warning("[TRADE]   TradeBroker is null; cannot handle cancel.");
        }
    }
}
