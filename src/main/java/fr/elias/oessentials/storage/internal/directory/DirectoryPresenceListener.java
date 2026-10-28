package fr.elias.oessentials.storage.internal.directory;

import fr.elias.oessentials.OEssentials;
import fr.elias.oessentials.platform.scheduling.OreScheduler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class DirectoryPresenceListener implements Listener {
    private final PlayerDirectory dir;
    private final String server;

    public DirectoryPresenceListener(PlayerDirectory dir, String server) {
        this.dir = dir;
        this.server = server;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        OreScheduler.runAsync(OEssentials.get(), () -> {
            try {
                dir.upsertPresence(e.getPlayer().getUniqueId(), e.getPlayer().getName(), server);
            } catch (Throwable t) {
                OEssentials.get().getLogger().warning("[PlayerDirectory] upsertPresence failed: " + t.getMessage());
            }
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        OreScheduler.runAsync(OEssentials.get(), () -> {
            try {
                dir.clearCurrentServer(e.getPlayer().getUniqueId(), server);
            } catch (Throwable t) {
                OEssentials.get().getLogger().warning("[PlayerDirectory] clearCurrentServer failed: " + t.getMessage());
            }
        });
    }

    /** Call once after register to seed already-online players (e.g., after /reload). */
    public void backfillOnline() {
        OreScheduler.runAsync(OEssentials.get(), () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                try {
                    dir.upsertPresence(p.getUniqueId(), p.getName(), server);
                } catch (Throwable ignored) {}
            }
        });
    }

}
