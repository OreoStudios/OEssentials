package fr.elias.oessentials.tab.internal;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate;
import fr.elias.oessentials.OEssentials;

/**
 * Hides vanilla list entries while retaining their profile and chat data.
 * Kept out of {@link CustomTablistLayout} so that class still loads without PacketEvents installed.
 */
final class VanillaTabInterceptor {
    private final OEssentials plugin;
    private PacketListenerAbstract listener;

    VanillaTabInterceptor(OEssentials plugin) {
        this.plugin = plugin;
    }

    boolean register(PacketTablistManager ptm) {
        listener = new PacketListenerAbstract(PacketListenerPriority.HIGH) {
            @Override
            public void onPacketSend(PacketSendEvent event) {
                if (event.isCancelled()) return;
                if (event.getPacketType() != PacketType.Play.Server.PLAYER_INFO_UPDATE) return;
                if (ptm == null) return;

                User user = event.getUser();
                if (user == null) return;
                if (!ptm.isInitialized(user.getUUID())) return;

                try {
                    WrapperPlayServerPlayerInfoUpdate wrapper = new WrapperPlayServerPlayerInfoUpdate(event);
                    // The client needs ADD_PLAYER profiles to spawn players in the world.
                    // Preserve every action (including signed chat), changing only tab visibility.
                    if (delistRealEntries(wrapper)) {
                        event.markForReEncode(true);
                    }
                } catch (Throwable t) {
                    plugin.getLogger().warning("[CustomTab] Interceptor error: " + t.getMessage());
                }
            }
        };
        try {
            PacketEvents.getAPI().getEventManager().registerListener(listener);
            return true;
        } catch (Throwable t) {
            plugin.getLogger().warning("[CustomTab] Failed to register vanilla tab interceptor: " + t.getMessage());
            listener = null;
            return false;
        }
    }

    void unregister() {
        if (listener == null) return;
        try {
            PacketEvents.getAPI().getEventManager().unregisterListener(listener);
        } catch (Throwable ignored) {}
        listener = null;
    }

    static boolean delistRealEntries(WrapperPlayServerPlayerInfoUpdate wrapper) {
        if (!wrapper.getActions().contains(WrapperPlayServerPlayerInfoUpdate.Action.UPDATE_LISTED)) {
            return false;
        }
        boolean changed = false;
        for (WrapperPlayServerPlayerInfoUpdate.PlayerInfo info : wrapper.getEntries()) {
            // Handle mixed packets per entry so decorative rows remain listed.
            if (!CustomTablistLayout.isFakeUuid(info.getProfileId()) && info.isListed()) {
                info.setListed(false);
                changed = true;
            }
        }
        return changed;
    }
}
