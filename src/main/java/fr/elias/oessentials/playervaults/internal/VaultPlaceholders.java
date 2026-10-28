package fr.elias.oessentials.playervaults.internal;

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
public final class VaultPlaceholders implements Placeholders.Resolver {
    private final OEssentials plugin;
    private final boolean debug;
    // Vault-access cache (avoids repeated sync DB/permission checks per placeholder tick)
    private final ConcurrentHashMap<UUID, long[]> vaultCacheTime = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, List<Integer>> vaultAccessibleCache = new ConcurrentHashMap<>();
    private static final long VAULT_CACHE_TTL = 4_000L;

    public VaultPlaceholders(OEssentials plugin) {
        this.plugin = plugin;
        this.debug = plugin.getConfig().getBoolean("placeholder-debug", false);
    }

    @Override
    public String resolve(OfflinePlayer player, String id, String idRaw) {
        if (id.equals("vaults_enabled")) {
            PlayerVaultsService pv = pvService();
            return (pv != null && pv.enabled()) ? "true" : "false";
        }

        if (id.equals("vaults_max")) {
            int max = pvConfigMax();
            return String.valueOf(Math.max(0, max));
        }

        if (id.equals("vaults_unlocked_count")) {
            Player p = player.getPlayer();
            PlayerVaultsService pv = pvService();
            if (p == null || pv == null || !pv.enabled()) return "0";
            int max = pvConfigMax();
            return String.valueOf(getCachedAccessibleVaults(p, pv, max).size());
        }

        if (id.equals("vaults_unlocked_list")) {
            Player p = player.getPlayer();
            PlayerVaultsService pv = pvService();
            if (p == null || pv == null || !pv.enabled()) return "";
            int max = pvConfigMax();
            List<String> ids = new ArrayList<>();
            for (int vid : getCachedAccessibleVaults(p, pv, max)) ids.add(String.valueOf(vid));
            return trim64(String.join(", ", ids));
        }

        if (id.equals("vaults_locked_list")) {
            Player p = player.getPlayer();
            PlayerVaultsService pv = pvService();
            if (p == null || pv == null || !pv.enabled()) return "";
            int max = pvConfigMax();
            List<Integer> accessible = getCachedAccessibleVaults(p, pv, max);
            List<String> ids = new ArrayList<>();
            for (int i = 1; i <= max; i++) {
                if (!accessible.contains(i)) ids.add(String.valueOf(i));
            }
            return trim64(String.join(", ", ids));
        }

        if (id.startsWith("vault_can_access_")) {
            Player p = player.getPlayer();
            PlayerVaultsService pv = pvService();
            if (p == null || pv == null || !pv.enabled()) return "false";
            int vid = parseIntSafe(id.substring("vault_can_access_".length()));
            if (vid <= 0) return "false";
            return pv.canAccess(p, vid) ? "true" : "false";
        }

        if (id.startsWith("vault_slots_")) {
            Player p = player.getPlayer();
            PlayerVaultsService pv = pvService();
            if (p == null || pv == null || !pv.enabled()) return "0";
            int vid = parseIntSafe(id.substring("vault_slots_".length()));
            if (vid <= 0) return "0";
            return String.valueOf(pv.resolveSlots(p, vid));
        }

        if (id.startsWith("vault_rows_")) {
            Player p = player.getPlayer();
            PlayerVaultsService pv = pvService();
            if (p == null || pv == null || !pv.enabled()) return "0";
            int vid = parseIntSafe(id.substring("vault_rows_".length()));
            if (vid <= 0) return "0";
            int slots = pv.resolveSlots(p, vid);
            int rows = Math.max(1, (int) Math.ceil(slots / 9.0));
            return String.valueOf(rows);
        }

        if (id.startsWith("vault_title_preview_")) {
            Player p = player.getPlayer();
            PlayerVaultsService pv = pvService();
            PlayerVaultsConfig cfg = pvConfig();
            if (p == null || pv == null || !pv.enabled() || cfg == null) return "";
            int vid = parseIntSafe(id.substring("vault_title_preview_".length()));
            if (vid <= 0) return "";
            int slots = pv.resolveSlots(p, vid);
            int rows = Math.max(1, (int) Math.ceil(slots / 9.0));
            String title = cfg.vaultTitle()
                    .replace("<id>", String.valueOf(vid))
                    .replace("<rows>", String.valueOf(rows));
            return org.bukkit.ChatColor.translateAlternateColorCodes('&', title);
        }

        return null;
    }

    private void debugLog(String message) {
        if (debug) plugin.getLogger().info("[PLACEHOLDER DEBUG] " + message);
    }

    /**
     * Returns the list of vault IDs (1-based, up to {@code max}) that {@code p} can
     * access, using a short-lived per-player cache (TTL: {@value #VAULT_CACHE_TTL} ms).
     *
     * <p>Without caching, each of the three vault placeholder handlers
     * ({@code vaults_unlocked_count}, {@code vaults_unlocked_list},
     * {@code vaults_locked_list}) would invoke {@code pv.canAccess()} in a tight loop
     * on every placeholder refresh, potentially causing synchronous DB or permission
     * I/O on the main thread for every online player.</p>
     */
    private List<Integer> getCachedAccessibleVaults(Player p, PlayerVaultsService pv, int max) {
        UUID uuid = p.getUniqueId();
        long now = System.currentTimeMillis();
        long[] ts = vaultCacheTime.get(uuid);
        if (ts != null && now - ts[0] < VAULT_CACHE_TTL) {
            return vaultAccessibleCache.getOrDefault(uuid, Collections.emptyList());
        }
        List<Integer> accessible = new ArrayList<>();
        for (int i = 1; i <= max; i++) {
            if (pv.canAccess(p, i)) accessible.add(i);
        }
        vaultAccessibleCache.put(uuid, accessible);
        vaultCacheTime.put(uuid, new long[]{now});
        return accessible;
    }

    private PlayerVaultsService pvService() {
        try {
            Method m = plugin.getClass().getMethod("getPlayerVaultsService");
            return (PlayerVaultsService) m.invoke(plugin);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private PlayerVaultsConfig pvConfig() {
        try {
            PlayerVaultsService svc = pvService();
            if (svc == null) return null;
            Method gm = svc.getClass().getMethod("getConfigBean");
            return (PlayerVaultsConfig) gm.invoke(svc);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private int pvConfigMax() {
        PlayerVaultsConfig c = pvConfig();
        return (c != null) ? c.maxVaults() : 0;
    }

    private static int parseIntSafe(String s) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return -1;
        }
    }

    private static String trim64(String s) {
        if (s == null) return "";
        return s.length() > 64 ? s.substring(0, 61) + "..." : s;
    }
}
