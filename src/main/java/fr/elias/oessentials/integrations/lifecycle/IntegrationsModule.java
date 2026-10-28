package fr.elias.oessentials.integrations.lifecycle;

import dev.oreo.modulith.core.PluginModule;
import fr.elias.oessentials.integrations.internal.placeholders.PlaceholderAPIHook;
import fr.elias.oessentials.platform.modularity.ManagedModule;
import fr.elias.oessentials.commandtoggle.lifecycle.CommandToggleServices;
import fr.elias.oessentials.configuration.lifecycle.ConfigurationServices;
import fr.elias.oessentials.currency.lifecycle.CurrencySystemServices;
import fr.elias.oessentials.crafting.lifecycle.CustomCraftingServices;
import fr.elias.oessentials.kits.lifecycle.KitsServices;
import fr.elias.oessentials.currency.internal.placeholders.CurrencyPlaceholderExpansion;
import fr.elias.oessentials.platform.scheduling.OreScheduler;
import java.util.HashMap;
import java.util.Map;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.AdvancedPie;
import org.bstats.charts.SimplePie;
import org.bstats.charts.SingleLineChart;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

@PluginModule(value = "integrations", dependencies = {"command-toggle::services", "configuration::services", "currency-system::services", "custom-crafting::services", "freeze::services", "kits::services", "messaging::services", "storage::services"}, after = {"nametag"}, optional = true)
public final class IntegrationsModule extends ManagedModule implements IntegrationsServices {
    private Metrics metrics;
    private PlaceholderAPIHook placeholderHook;
    private CurrencyPlaceholderExpansion currencyPlaceholders;

    @Override
    protected void start() {
        cleanup("placeholderHook", () -> { if (placeholderHook != null) placeholderHook.unregister(); });
        cleanup("currencyPlaceholders", () -> { if (currencyPlaceholders != null) currencyPlaceholders.unregister(); });
        cleanup("metrics", () -> { if (metrics != null) metrics.shutdown(); });
        initializeBStats();
        warnMissingDependencies();
        showCompletionBanner();
        tryRegisterPlaceholderAPI();
        applyCommandToggles();
    }

    @Override public Metrics getMetrics() { return metrics; }
    @Override public PlaceholderAPIHook getPlaceholderHook() { return placeholderHook; }
    @Override public CurrencyPlaceholderExpansion getCurrencyPlaceholders() { return currencyPlaceholders; }

    // -------------------------------------------------------------------------
    // bStats
    // -------------------------------------------------------------------------

    private void initializeBStats() {
        try {
            int pluginId = 34390;
            this.metrics = new Metrics(plugin, pluginId);
            metrics.addCustomChart(new SimplePie("storage_type", () -> plugin.getConfig().getString("essentials.storage", "yaml").toUpperCase()));
            metrics.addCustomChart(new SimplePie("economy_type", () -> services(ConfigurationServices.class).getEconomyEnabled() ? plugin.getConfig().getString("economy.type", "none").toUpperCase() : "Disabled"));
            metrics.addCustomChart(new AdvancedPie("enabled_features", () -> {
                Map<String, Integer> features = new HashMap<>();
                if (services(ConfigurationServices.class).getFeatureSettings().kitsEnabled())           features.put("Kits", 1);
                if (services(ConfigurationServices.class).getFeatureSettings().tradeEnabled())          features.put("Trade", 1);
                if (services(ConfigurationServices.class).getFeatureSettings().rtpEnabled())            features.put("RTP", 1);
                if (services(ConfigurationServices.class).getFeatureSettings().bossbarEnabled())        features.put("BossBar", 1);
                if (services(ConfigurationServices.class).getFeatureSettings().scoreboardEnabled())     features.put("Scoreboard", 1);
                if (services(ConfigurationServices.class).getFeatureSettings().tabEnabled())            features.put("Tab", 1);
                if (services(ConfigurationServices.class).getFeatureSettings().clearLagEnabled())       features.put("ClearLag", 1);
                if (services(ConfigurationServices.class).getFeatureSettings().oreoHologramsEnabled())  features.put("Holograms", 1);
                if (services(ConfigurationServices.class).getFeatureSettings().playtimeRewardsEnabled()) features.put("PlaytimeRewards", 1);
                if (services(ConfigurationServices.class).getFeatureSettings().worldShardingEnabled())  features.put("Sharding", 1);
                return features;
            }));
            metrics.addCustomChart(new SimplePie("cross_server_mode", () -> {
                if (!services(ConfigurationServices.class).getRabbitEnabled()) return "Disabled";
                boolean anyCross = services(ConfigurationServices.class).getCrossServerFeatures().homes() || services(ConfigurationServices.class).getCrossServerFeatures().warps() || services(ConfigurationServices.class).getCrossServerFeatures().spawn() || services(ConfigurationServices.class).getCrossServerFeatures().economy();
                return anyCross ? "Enabled" : "Disabled";
            }));
            metrics.addCustomChart(new SimplePie("redis_enabled", () -> services(ConfigurationServices.class).getRedisEnabled() ? "Enabled" : "Disabled"));
            metrics.addCustomChart(new SingleLineChart("total_kits", () -> services(KitsServices.class).getKitsManager() == null ? 0 : services(KitsServices.class).getKitsManager().getKits().size()));
            metrics.addCustomChart(new SingleLineChart("custom_recipes", () -> services(CustomCraftingServices.class).customRecipeCount()));
            plugin.getLogger().info("[bStats] Metrics initialized. View at: https://bstats.org/plugin/bukkit/" + pluginId);
        } catch (Exception e) {
            plugin.getLogger().warning("[bStats] Failed to initialize metrics: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    // Missing-dependency warnings
    // -------------------------------------------------------------------------

    private void warnMissingDependencies() {
        java.util.List<String[]> missing = new java.util.ArrayList<>();

        // Vault — needed for economy Vault bridge (economy still works internally without it)
        if (services(ConfigurationServices.class).getEconomyEnabled() && Bukkit.getPluginManager().getPlugin("Vault") == null) {
            missing.add(new String[]{"Vault", "economy.enabled=true but Vault is not installed",
                    "Economy works internally; other plugins cannot use it via Vault API",
                    "https://www.spigotmc.org/resources/34315/"});
        }

        // PlaceholderAPI — needed for all %placeholder% expansion
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            missing.add(new String[]{"PlaceholderAPI", "No PlaceholderAPI found",
                    "Scoreboards, tab, nametags and chat will show raw %placeholder% tokens",
                    "https://www.spigotmc.org/resources/6245/"});
        }

        // LuckPerms — needed for prefix/suffix in chat, nametags, tab
        if (Bukkit.getPluginManager().getPlugin("LuckPerms") == null) {
            missing.add(new String[]{"LuckPerms", "LuckPerms not found",
                    "%luckperms_prefix% and %luckperms_suffix% will be empty in chat/nametag/tab",
                    "https://luckperms.net/"});
        }

        if (missing.isEmpty()) return;

        for (String[] dep : missing) {
            String name    = dep[0];
            String reason  = dep[1];
            String impact  = dep[2];
            String url     = dep[3];
            plugin.getLogger().warning("Missing dependency: " + name + ". Please download it: " + url);
            plugin.getLogger().warning("Reason: " + reason + ". Impact: " + impact);
        }

        plugin.getLogger().warning("OEssentials will continue with affected features limited. Install the missing plugins and restart the server.");
    }

    private void showCompletionBanner() {
        plugin.getLogger().info("+------------------------------------------------------------+");
        plugin.getLogger().info("|            ** OREOESSENTIALS READY **                      |");
        plugin.getLogger().info("|  Players online: " + String.format("%-43d", Bukkit.getOnlinePlayers().size()) + "|");
        plugin.getLogger().info("+------------------------------------------------------------+");
    }

    // -------------------------------------------------------------------------
    // PlaceholderAPI
    // -------------------------------------------------------------------------

    private void tryRegisterPlaceholderAPI() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            plugin.getLogger().info("PlaceholderAPI not found; skipping placeholders.");
            return;
        }
        try {
            placeholderHook = new PlaceholderAPIHook(plugin);
            OreScheduler.runLater(plugin, () -> {
                try {
                    if (placeholderHook.register()) {
                        plugin.getLogger().info("[OK] PlaceholderAPI expansion 'oreo' registered successfully!");
                        if (plugin.getConfig().getBoolean("placeholder-debug", false)) {
                            Player testPlayer = Bukkit.getOnlinePlayers().stream().findFirst().orElse(null);
                            if (testPlayer != null) plugin.getLogger().info("[PAPI TEST] %oreo_network_online% = " + placeholderHook.onRequest(testPlayer, "network_online"));
                        }
                    } else {
                        plugin.getLogger().severe("[FAIL] Failed to register PlaceholderAPI expansion!");
                    }
                } catch (Exception e) {
                    plugin.getLogger().severe("[FAIL] Error during PlaceholderAPI registration: " + e.getMessage());
                    e.printStackTrace();
                }
                if (services(CurrencySystemServices.class).getCurrencyService() != null) {
                    try {
                        this.currencyPlaceholders = new CurrencyPlaceholderExpansion(plugin);
                        if (this.currencyPlaceholders.register()) { plugin.getLogger().info("[OK] PlaceholderAPI currency expansion registered!"); }
                        else { plugin.getLogger().warning("[FAIL] Failed to register PlaceholderAPI currency expansion!"); }
                    } catch (Throwable t) { plugin.getLogger().warning("[Currency] PlaceholderAPI expansion failed: " + t.getMessage()); }
                }
            }, 60L);
        } catch (Throwable t) {
            plugin.getLogger().severe("Failed to register PlaceholderAPI: " + t.getMessage());
            t.printStackTrace();
        }
    }

    private void applyCommandToggles() {
        try {
            if (services(CommandToggleServices.class).getCommandToggleService() != null) {
                services(CommandToggleServices.class).applyToggles();
                plugin.getLogger().info("[CommandToggle] Command toggles applied successfully");
            }
        } catch (Exception e) {
            plugin.getLogger().severe("[CommandToggle] Failed to apply command toggles: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
