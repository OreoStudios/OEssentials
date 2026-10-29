package fr.elias.oessentials.playtime.internal;

import fr.elias.oessentials.OEssentials;
import fr.elias.oessentials.configuration.api.Placeholders;
import fr.elias.oessentials.shared.Lang;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/** %oreo_...% placeholders of this module (moved from integrations' PlaceholderAPIHook). */
public final class PlaytimePlaceholders implements Placeholders.Resolver {
    private final OEssentials plugin;
    private final boolean debug;

    public PlaytimePlaceholders(OEssentials plugin) {
        this.plugin = plugin;
        this.debug = plugin.getConfig().getBoolean("placeholder-debug", false);
    }

    @Override
    public String resolve(OfflinePlayer player, String id, String idRaw) {
        if (id.equals("playtime_total_seconds")) {
            long secs = playtimeTotalSeconds(player);
            return String.valueOf(secs);
        }

        if (id.equals("playtime_total_minutes")) {
            long secs = playtimeTotalSeconds(player);
            return String.valueOf(secs / 60);
        }

        if (id.equals("playtime_total_hours")) {
            long secs = playtimeTotalSeconds(player);
            return String.valueOf(secs / 3600);
        }

        if (id.equals("playtime_total_days")) {
            long secs = playtimeTotalSeconds(player);
            return String.valueOf(secs / 86400);
        }

        if (id.equals("prewards_enabled")) {
            PlaytimeRewardsService svc = prewards();
            return (svc != null && svc.isEnabled()) ? "true" : "false";
        }

        if (id.equals("prewards_ready_count")) {
            Player p = player.getPlayer();
            PlaytimeRewardsService svc = prewards();
            if (p == null || svc == null || !svc.isEnabled()) return "0";
            return String.valueOf(svc.rewardsReady(p).size());
        }

        if (id.equals("prewards_ready_list")) {
            Player p = player.getPlayer();
            PlaytimeRewardsService svc = prewards();
            if (p == null || svc == null || !svc.isEnabled()) return "";
            return trim64(String.join(", ", svc.rewardsReady(p)));
        }

        if (id.startsWith("prewards_state_")) {
            Player p = player.getPlayer();
            PlaytimeRewardsService svc = prewards();
            if (p == null || svc == null || !svc.isEnabled()) return "";
            String rewardId = id.substring("prewards_state_".length());
            var entry = svc.rewards.get(rewardId);
            if (entry == null) return "";
            return svc.stateOf(p, entry).name();
        }

        return null;
    }

    private void debugLog(String message) {
        if (debug) plugin.getLogger().info("[PLACEHOLDER DEBUG] " + message);
    }

    private long playtimeTotalSeconds(OfflinePlayer off) {
        Player p = off.getPlayer();
        if (p != null) {
            PlaytimeRewardsService svc = prewards();
            if (svc != null && svc.isEnabled()) {
                try {
                    return Math.max(0, svc.getPlaytimeSeconds(p));
                } catch (Throwable ignored) {}
            }
        }

        PlaytimeTracker tr = tracker();
        if (tr != null && off.getUniqueId() != null) {
            try {
                return Math.max(0, tr.getSeconds(off.getUniqueId()));
            } catch (Throwable ignored) {}
        }

        try {
            if (p != null) {
                int ticks = p.getStatistic(Statistic.PLAY_ONE_MINUTE);
                return Math.max(0, ticks / 20L);
            }
        } catch (Throwable ignored) {}

        return 0;
    }

    private static String trim64(String s) {
        if (s == null) return "";
        return s.length() > 64 ? s.substring(0, 61) + "..." : s;
    }

    private PlaytimeRewardsService prewards() {
        try {
            Method m = plugin.getClass().getMethod("getPlaytimeRewardsService");
            return (PlaytimeRewardsService) m.invoke(plugin);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private PlaytimeTracker tracker() {
        try {
            Method m = plugin.getClass().getMethod("getPlaytimeTracker");
            return (PlaytimeTracker) m.invoke(plugin);
        } catch (Throwable ignored) {
            return null;
        }
    }
}
