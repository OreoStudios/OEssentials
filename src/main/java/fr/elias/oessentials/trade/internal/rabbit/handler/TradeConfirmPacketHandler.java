package fr.elias.oessentials.trade.internal.rabbit.handler;

import fr.elias.oessentials.OEssentials;
import fr.elias.oessentials.messaging.api.PacketChannel;
import fr.elias.oessentials.messaging.api.PacketSubscriber;
import fr.elias.oessentials.trade.internal.rabbit.packet.TradeConfirmPacket;

public final class TradeConfirmPacketHandler implements PacketSubscriber<TradeConfirmPacket> {
    private final OEssentials plugin;
    public TradeConfirmPacketHandler(OEssentials plugin) { this.plugin = plugin; }

    private boolean dbg(){
        try { return OEssentials.get().getTradeService().getConfig().debugDeep; }
        catch (Throwable t){ return false; }
    }

    @Override
    public void onReceive(PacketChannel channel, TradeConfirmPacket packet) {
        if (dbg()) plugin.getLogger().info("[TRADE] RECV TradeConfirmPacket ch=" + channel +
                " session=" + packet.getSessionId() + " confirmer=" + packet.getConfirmerId());

        if (plugin.getTradeBroker() != null) {
            plugin.getTradeBroker().handleRemoteConfirm(packet);
        } else if (dbg()) {
            plugin.getLogger().warning("[TRADE]   TradeBroker is null; cannot handle confirm.");
        }
    }
}
