package fr.elias.oessentials.economy.internal;

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
public final class EconomyPlaceholders implements Placeholders.Resolver {
    private final OEssentials plugin;
    private final boolean debug;

    public EconomyPlaceholders(OEssentials plugin) {
        this.plugin = plugin;
        this.debug = plugin.getConfig().getBoolean("placeholder-debug", false);
    }

    @Override
    public String resolve(OfflinePlayer player, String id, String idRaw) {
        if (id.equals("balance")) {
            EconomyService eco = getEconomyService();
            if (eco == null || player.getUniqueId() == null) {
                debugLog("Economy is null or player UUID is null for 'balance'");
                return "0";
            }
            double bal = eco.getBalance(player.getUniqueId());
            String result = String.valueOf(bal);
            debugLog("balance = " + result);
            return result;
        }

        if (id.equals("balance_formatted")) {
            EconomyService eco = getEconomyService();
            if (eco == null || player.getUniqueId() == null) {
                debugLog("Economy is null or player UUID is null for 'balance_formatted'");
                return "0";
            }
            double bal = eco.getBalance(player.getUniqueId());
            DecimalFormat df = new DecimalFormat("#,##0.##");
            String result = df.format(bal);
            debugLog("balance_formatted = " + result);
            return result;
        }

        return null;
    }

    private void debugLog(String message) {
        if (debug) plugin.getLogger().info("[PLACEHOLDER DEBUG] " + message);
    }

    private EconomyService getEconomyService() {
        try {
            return plugin.getEcoBootstrap().api();
        } catch (Throwable t) {
            debugLog("Failed to get EconomyService: " + t.getMessage());
            return null;
        }
    }
}
