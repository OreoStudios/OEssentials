package fr.elias.oessentials.trade.internal.rabbit.handler;

import fr.elias.oessentials.OEssentials;
import fr.elias.oessentials.messaging.api.PacketChannel;
import fr.elias.oessentials.messaging.api.PacketSubscriber;
import fr.elias.oessentials.trade.internal.rabbit.packet.TradeClosePacket;
import fr.elias.oessentials.platform.scheduling.OreScheduler;
import org.bukkit.Bukkit;

public final class TradeClosePacketHandler implements PacketSubscriber<TradeClosePacket> {
    private final OEssentials plugin;

    public TradeClosePacketHandler(OEssentials plugin) {
        this.plugin = plugin;
    }

    private boolean dbg() {
        try { return OEssentials.get().getTradeService().getConfig().debugDeep; }
        catch (Throwable t) { return false; }
    }

    @Override
    public void onReceive(PacketChannel channel, TradeClosePacket packet) {
        if (dbg()) plugin.getLogger().info("[TRADE] RECV TradeClosePacket ch=" + channel +
                " session=" + packet.getSessionId() + " target=" + packet.getGrantTo());

        OreScheduler.run(plugin, () -> {
            if (plugin.getTradeService() != null) {
                plugin.getTradeService().handleRemoteClose(packet);
            } else if (dbg()) {
                plugin.getLogger().warning("[TRADE]   TradeService is null; cannot handle close.");
            }
        });
    }
}
