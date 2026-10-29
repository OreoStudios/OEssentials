package fr.elias.oessentials.kits.internal;

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
public final class KitsPlaceholders implements Placeholders.Resolver {
    private final OEssentials plugin;
    private final boolean debug;

    public KitsPlaceholders(OEssentials plugin) {
        this.plugin = plugin;
        this.debug = plugin.getConfig().getBoolean("placeholder-debug", false);
    }

    @Override
    public String resolve(OfflinePlayer player, String id, String idRaw) {
        if (id.equals("kits_enabled")) {
            KitsManager km = kits();
            return (km != null && km.isEnabled()) ? "true" : "false";
        }

        if (id.equals("kits_count")) {
            KitsManager km = kits();
            return (km != null) ? String.valueOf(km.getKits().size()) : "0";
        }

        if (id.equals("kits_ready_count")) {
            Player p = player.getPlayer();
            KitsManager km = kits();
            if (p == null || km == null) return "0";
            return String.valueOf((int) km.getKits().values().stream()
                    .filter(k -> isKitReady(km, p, k))
                    .count());
        }

        if (id.equals("kits_ready_list")) {
            Player p = player.getPlayer();
            KitsManager km = kits();
            if (p == null || km == null) return "";
            List<String> ready = km.getKits().values().stream()
                    .filter(k -> isKitReady(km, p, k))
                    .map(Kit::getId)
                    .collect(Collectors.toList());
            return trim64(String.join(", ", ready));
        }

        if (id.startsWith("kit_ready_")) {
            Player p = player.getPlayer();
            if (p == null) return "false";
            String kitId = id.substring("kit_ready_".length());
            KitsManager km = kits();
            if (km == null) return "false";
            Kit k = km.getKits().get(kitId.toLowerCase(Locale.ROOT));
            return (k != null && isKitReady(km, p, k)) ? "true" : "false";
        }

        if (id.startsWith("kit_cooldown_formatted_") || (id.startsWith("kit_") && id.endsWith("_cooldown_formatted"))) {
            Player p = player.getPlayer();
            if (p == null) return "";

            String kitId;
            if (id.startsWith("kit_cooldown_formatted_")) {
                kitId = id.substring("kit_cooldown_formatted_".length());
            } else {
                kitId = id.substring("kit_".length(), id.length() - "_cooldown_formatted".length());
            }

            KitsManager km = kits();
            if (km == null) return "";
            Kit k = km.getKits().get(kitId.toLowerCase(Locale.ROOT));
            if (k == null) return "";

            long left = Math.max(0, km.getSecondsLeft(p, k));
            if (left <= 0) return "ready";

            return Lang.timeHuman(left);
        }

        if (id.startsWith("kit_cooldown_")) {
            Player p = player.getPlayer();
            if (p == null) return "";
            String kitId = id.substring("kit_cooldown_".length());
            KitsManager km = kits();
            if (km == null) return "";
            Kit k = km.getKits().get(kitId.toLowerCase(Locale.ROOT));
            if (k == null) return "";
            long left = Math.max(0, km.getSecondsLeft(p, k));
            return left <= 0 ? "ready" : String.valueOf(left);
        }

        return null;
    }

    private void debugLog(String message) {
        if (debug) plugin.getLogger().info("[PLACEHOLDER DEBUG] " + message);
    }

    private boolean isKitReady(KitsManager km, Player p, Kit k) {
        if (!km.isEnabled()) return false;
        if (!p.hasPermission("oreo.kit.claim")) return false;
        long left = Math.max(0, km.getSecondsLeft(p, k));
        return left == 0 || p.hasPermission("oreo.kit.bypasscooldown");
    }

    private static String trim64(String s) {
        if (s == null) return "";
        return s.length() > 64 ? s.substring(0, 61) + "..." : s;
    }

    private KitsManager kits() {
        try {
            Method m = plugin.getClass().getMethod("getKitsManager");
            return (KitsManager) m.invoke(plugin);
        } catch (Throwable ignored) {
            return null;
        }
    }
}
