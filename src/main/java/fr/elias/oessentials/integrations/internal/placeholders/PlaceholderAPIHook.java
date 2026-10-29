package fr.elias.oessentials.integrations.internal.placeholders;

import fr.elias.oessentials.OEssentials;
import fr.elias.oessentials.configuration.lifecycle.ConfigurationServices;

import fr.elias.oessentials.shared.Lang;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class PlaceholderAPIHook extends PlaceholderExpansion {
    private final OEssentials plugin;
    private boolean debug;
    private final String thisServerName;


    public PlaceholderAPIHook(OEssentials plugin) {
        this.plugin = plugin;
        this.debug = plugin.getConfig().getBoolean("placeholder-debug", false);
        this.thisServerName = plugin.getServerConfig().serverName();
    }

    @Override
    public @NotNull String getIdentifier() {
        return "oreo";
    }

    @Override
    public @NotNull String getAuthor() {
        return String.join(", ", plugin.getDescription().getAuthors());
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String idRaw) {
        if (player == null) {
            debugLog("Player is null for placeholder: %oreo_" + idRaw + "%");
            return "";
        }

        final String id = idRaw.toLowerCase(Locale.ROOT);

        debugLog("Processing placeholder: %oreo_" + idRaw + "% for player: " + player.getName());

        // Kits, playtime, vaults and balance placeholders are answered by their modules.
        var registered = plugin.getModuleRegistry().read(ConfigurationServices.class, ConfigurationServices::getPlaceholders);
        String fromModule = registered == null ? null : registered.resolve(player, id, idRaw);
        if (fromModule != null) return fromModule;

        if (id.equals("server_name")) {
            try {
                return plugin.getServerConfig().serverName();
            } catch (Throwable t) {
                return Bukkit.getServer().getName();
            }
        }

        if (id.equals("server_nick")) {
            return resolveServerNick(Bukkit.getServer().getName());
        }

        if (id.startsWith("server_nick_")) {
            String serverId = id.substring("server_nick_".length());
            if (serverId.isBlank()) return "";
            return resolveServerNick(serverId);
        }

        if (id.equals("crossserver_players_total")) {
            var counts = plugin.getModuleRegistry().read(fr.elias.oessentials.messaging.lifecycle.MessagingServices.class,
                    fr.elias.oessentials.messaging.lifecycle.MessagingServices::getNetworkPlayerCounts);
            return String.valueOf(counts == null ? 0 : counts.networkTotal());
        }

        if (id.equals("server_players_total")) {
            var counts = plugin.getModuleRegistry().read(fr.elias.oessentials.messaging.lifecycle.MessagingServices.class,
                    fr.elias.oessentials.messaging.lifecycle.MessagingServices::getNetworkPlayerCounts);
            int cached = counts == null ? 0 : counts.serverTotal(thisServerName);
            return String.valueOf(cached > 0 ? cached : Bukkit.getOnlinePlayers().size());
        }

        if (id.startsWith("server_players_")) {
            String targetServer = idRaw.substring("server_players_".length());
            return String.valueOf(serverPlayerCount(targetServer));
        }

        if (id.endsWith("_players_total")
                && !id.equals("crossserver_players_total")
                && !id.equals("server_players_total")) {
            String targetServer = idRaw.substring(0, idRaw.length() - "_players_total".length());
            return String.valueOf(serverPlayerCount(targetServer));
        }

        if (id.equals("world_name") || id.equals("world")) {
            Player p = player.getPlayer();
            if (p == null) return "";
            return p.getWorld().getName();
        }

        if (id.equals("world_nick")) {
            Player p = player.getPlayer();
            if (p == null) return "";
            return resolveWorldNick(p.getWorld().getName());
        }

        if (id.startsWith("world_nick_")) {
            String worldId = id.substring("world_nick_".length());
            if (worldId.isBlank()) return "";
            return resolveWorldNick(worldId);
        }

        if (id.equals("network_online")) {
            return String.valueOf(Bukkit.getOnlinePlayers().size());
        }
        if (id.equals("tempfly_active")) {
            fr.elias.oessentials.api.ITempFlyAPI tfs = plugin.getModuleRegistry().read(ConfigurationServices.class, ConfigurationServices::getTempFly);
            if (tfs == null) return "false";
            Player p = player.getPlayer();
            if (p == null) return "false";
            return tfs.getTimeRemaining(p) > 0 ? "true" : "false";
        }

        if (id.equals("tempfly_seconds")) {
            fr.elias.oessentials.api.ITempFlyAPI tfs = plugin.getModuleRegistry().read(ConfigurationServices.class, ConfigurationServices::getTempFly);
            if (tfs == null) return "0";
            Player p = player.getPlayer();
            if (p == null) return "0";
            return String.valueOf(tfs.getTimeRemaining(p));
        }

        if (id.equals("tempfly_time")) {
            fr.elias.oessentials.api.ITempFlyAPI tfs = plugin.getModuleRegistry().read(ConfigurationServices.class, ConfigurationServices::getTempFly);
            if (tfs == null) return "0s";
            Player p = player.getPlayer();
            if (p == null) return "0s";
            int secs = tfs.getTimeRemaining(p);
            if (secs <= 0) return "inactive";
            return formatTempFlyTime(secs);
        }

        if (id.equals("tempfly_time_or_empty")) {
            fr.elias.oessentials.api.ITempFlyAPI tfs = plugin.getModuleRegistry().read(ConfigurationServices.class, ConfigurationServices::getTempFly);
            if (tfs == null) return "";
            Player p = player.getPlayer();
            if (p == null) return "";
            int secs = tfs.getTimeRemaining(p);
            return secs > 0 ? formatTempFlyTime(secs) : "";
        }
        if (id.equals("homes_used") || id.equals("homes_max") || id.equals("homes")) {
            int used = 0;
            int max = 0;

            try {
                Object homes = homeService();
                if (homes != null && player.getUniqueId() != null) {
                    Method m = homes.getClass().getMethod("homes", UUID.class);
                    Object list = m.invoke(homes, player.getUniqueId());
                    if (list instanceof Collection<?> coll) {
                        used = coll.size();
                    } else if (list instanceof Map<?, ?> map) {
                        used = map.size();
                    }
                }
            } catch (Throwable ignored) {}

            try {
                Object cfg = configService();
                Player p = player.getPlayer();
                if (cfg != null && p != null) {
                    Method m = cfg.getClass().getMethod("getMaxHomesFor", Player.class);
                    Object out = m.invoke(cfg, p);
                    if (out instanceof Number n) max = n.intValue();
                } else if (cfg != null) {
                    Method m = cfg.getClass().getMethod("defaultMaxHomes");
                    Object out = m.invoke(cfg);
                    if (out instanceof Number n) max = n.intValue();
                }
            } catch (Throwable ignored) {}

            if (id.equals("homes_used")) return String.valueOf(used);
            if (id.equals("homes_max")) return String.valueOf(max);
            return used + "/" + max;
        }

        debugLog("Unknown placeholder: %oreo_" + idRaw + "%");
        return null;
    }

    private void debugLog(String message) {
        if (debug) {
            plugin.getLogger().info("[PLACEHOLDER DEBUG] " + message);
        }
    }



    private String resolveServerNick(String serverName) {
        try {
            var c = plugin.getConfig();
            String def = c.getString("server_nicknames.default", serverName);
            var sec = c.getConfigurationSection("server_nicknames.map");
            if (sec == null) return def;
            for (String key : sec.getKeys(false)) {
                if (key != null && key.equalsIgnoreCase(serverName)) {
                    return sec.getString(key, def);
                }
            }
            return def;
        } catch (Throwable t) {
            return serverName;
        }
    }

    private String resolveWorldNick(String worldName) {
        try {
            var c = plugin.getConfig();
            String def = c.getString("world_nicknames.default", worldName);
            var sec = c.getConfigurationSection("world_nicknames.map");
            if (sec == null) return def;
            for (String key : sec.getKeys(false)) {
                if (key != null && key.equalsIgnoreCase(worldName)) {
                    return sec.getString(key, def);
                }
            }
            return def;
        } catch (Throwable t) {
            return worldName;
        }
    }


    private static String trim64(String s) {
        if (s == null) return "";
        return s.length() > 64 ? s.substring(0, 61) + "..." : s;
    }









    private Object homeService() {
        try {
            Method m = plugin.getClass().getMethod("getHomeService");
            return m.invoke(plugin);
        } catch (Throwable ignored) {
            return null;
        }
    }
    private int serverPlayerCount(String serverName) {
        var counts = plugin.getModuleRegistry().read(fr.elias.oessentials.messaging.lifecycle.MessagingServices.class,
                fr.elias.oessentials.messaging.lifecycle.MessagingServices::getNetworkPlayerCounts);
        return counts == null ? 0 : counts.serverTotal(serverName);
    }

    private String formatTempFlyTime(int seconds) {
        if (seconds < 60) return seconds + "s";
        if (seconds < 3600) {
            int m = seconds / 60, s = seconds % 60;
            return s > 0 ? m + "m " + s + "s" : m + "m";
        }
        int h = seconds / 3600, m = (seconds % 3600) / 60;
        return m > 0 ? h + "h " + m + "m" : h + "h";
    }
    private Object configService() {
        try {
            Method m = plugin.getClass().getMethod("getConfigService");
            return m.invoke(plugin);
        } catch (Throwable ignored) {
            return null;
        }
    }
}