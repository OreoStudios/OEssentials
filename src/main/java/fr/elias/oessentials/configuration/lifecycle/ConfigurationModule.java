package fr.elias.oessentials.configuration.lifecycle;

import com.google.gson.Gson;
import dev.oreo.modulith.core.PluginModule;
import fr.elias.oessentials.platform.modularity.BootstrapSupport;
import fr.elias.oessentials.platform.modularity.ManagedModule;
import fr.elias.oessentials.platform.commands.CommandManager;
import fr.elias.oessentials.configuration.internal.config.ConfigService;
import fr.elias.oessentials.configuration.internal.config.CrossServerSettings;
import fr.elias.oessentials.configuration.internal.config.SettingsConfig;
import fr.elias.oessentials.configuration.internal.autoreboot.AutoRebootService;
import fr.elias.oessentials.crafting.internal.customcraft.CraftActionsConfig;
import fr.elias.oessentials.configuration.internal.invlook.listeners.InvlookListener;
import fr.elias.oessentials.configuration.internal.invlook.manager.InvlookManager;
import fr.elias.oessentials.configuration.internal.tempfly.TempFlyCommand;
import fr.elias.oessentials.configuration.internal.logging.KillallLogger;
import fr.elias.oessentials.shared.Lang;
import java.io.File;
import java.util.Map;

@PluginModule(value = "configuration")
public final class ConfigurationModule extends ManagedModule implements ConfigurationServices {
    private SettingsConfig settingsConfig;
    private fr.elias.oessentials.configuration.internal.tempfly.TempFlyService tempFlyService;
    private fr.elias.oessentials.configuration.internal.tempfly.TempFlyConfig tempFlyConfig;
    private ConfigService configService;
    private AutoRebootService autoRebootService;
    private CraftActionsConfig craftActionsConfig;
    private CommandManager commands;
    private KillallLogger killallLogger;
    private boolean economyEnabled;
    private boolean redisEnabled;
    private boolean rabbitEnabled;
    private InvlookManager invlookManager;
    private volatile CrossServerSettings crossServerSettings;
    private Gson gson = new Gson();

    @Override
    protected void start() {
        cleanup("commands", () -> { if (commands != null) commands.shutdown(); });
        cleanup("autoRebootService", () -> { if (autoRebootService != null) autoRebootService.stop(); });
        cleanup("tempFlyService", () -> { if (tempFlyService != null) tempFlyService.shutdown(); });
        cleanup("invlookManager", () -> { if (invlookManager != null) invlookManager.clear(); });
        initConfig();
    }

    @Override public SettingsConfig getFeatureSettings() { return settingsConfig; }
    @Override public SettingsConfig getPerformanceGuardSettings() { return settingsConfig; }
    @Override public ConfigService getServerConfig() { return configService; }
    @Override public CrossServerSettings getCrossServerFeatures() { return crossServerSettings; }
    @Override public fr.elias.oessentials.configuration.internal.tempfly.TempFlyService getTempFlyService() { return tempFlyService; }
    @Override public fr.elias.oessentials.configuration.internal.tempfly.TempFlyConfig getTempFlyConfig() { return tempFlyConfig; }
    @Override public AutoRebootService getAutoRebootService() { return autoRebootService; }
    @Override public CraftActionsConfig getCraftActionsConfig() { return craftActionsConfig; }
    @Override public CommandManager getCommands() { return commands; }
    @Override public KillallLogger getKillallLogger() { return killallLogger; }
    @Override public boolean getEconomyEnabled() { return economyEnabled; }
    @Override public boolean getRedisEnabled() { return redisEnabled; }
    @Override public boolean getRabbitEnabled() { return rabbitEnabled; }
    @Override public InvlookManager getInvlookManager() { return invlookManager; }
    @Override public Gson getGson() { return gson; }

    @Override
    public void reloadSettings() {
        settingsConfig.reload();
        crossServerSettings = CrossServerSettings.load(plugin);
    }

    @Override
    public void updateSettings(Map<String, Object> values) {
        settingsConfig.update(values);
    }

    // Init methods

    private void initConfig() {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            plugin.getDataFolder().mkdirs();
            plugin.saveDefaultConfig();
            plugin.getLogger().info("[Config] Created default config.yml");
        } else {
            plugin.getLogger().info("[Config] Loading existing config.yml");
        }

        fr.elias.oessentials.configuration.internal.config.LegacySettingsMigrator.migrate(plugin);
        fr.elias.oessentials.configuration.internal.migration.ScoreboardTabMigrator.migrate(plugin);
        fr.elias.oessentials.configuration.internal.migration.ChatMessagingMigrator.migrate(plugin);
        fr.elias.oessentials.configuration.internal.migration.FeatureConfigMigrator.migrate(plugin);
        plugin.reloadConfig();

        this.settingsConfig = new SettingsConfig(plugin);

        showStartupBanner();

        this.configService    = new ConfigService(plugin);
        this.crossServerSettings = CrossServerSettings.load(plugin);
        this.economyEnabled   = settingsConfig.economyEnabled();
        this.redisEnabled     = plugin.getConfig().getBoolean("redis.enabled", false);
        this.rabbitEnabled    = plugin.getConfig().getBoolean("rabbitmq.enabled", false);
        this.invlookManager   = new InvlookManager();
        this.killallLogger    = new KillallLogger(plugin);

        plugin.getLogger().info("[Economy] " + (economyEnabled ? "Enabled" : "Disabled") + " via settings.yml");

        this.autoRebootService = new AutoRebootService(plugin);
        this.autoRebootService.start();

        this.craftActionsConfig = new CraftActionsConfig(plugin);

        this.commands = new CommandManager(plugin);

        plugin.getServer().getPluginManager().registerEvents(new InvlookListener(plugin), plugin);

        Lang.init(plugin);

        if (settingsConfig.isEnabled("tempfly")) {
            this.tempFlyConfig  = new fr.elias.oessentials.configuration.internal.tempfly.TempFlyConfig(plugin.getDataFolder());
            this.tempFlyService = new fr.elias.oessentials.configuration.internal.tempfly.TempFlyService(plugin, tempFlyConfig);
            var tempFlyCmd = new TempFlyCommand(tempFlyService);
            this.commands.register(tempFlyCmd);
            commands.rewireTab("tempfly", tempFlyCmd);
            commands.rewireTab("tfly",    tempFlyCmd);
            plugin.getLogger().info("[TempFly] Enabled.");
        } else {
            BootstrapSupport.unregisterCommandHard(plugin, "tempfly");
            BootstrapSupport.unregisterCommandHard(plugin, "tfly");
            plugin.getLogger().info("[TempFly] Disabled by settings.yml.");
        }

        this.commands.register(new fr.elias.oessentials.commands.internal.commands.core.admins.OeSettingsCommand(plugin));
        plugin.getLogger().info("[Settings] GUI command registered (/oesettings).");

        plugin.getLogger().info("[BOOT] storage=" + plugin.getConfig().getString("essentials.storage", "yaml")
                + " economyType=" + plugin.getConfig().getString("economy.type", "none")
                + " redis=" + redisEnabled + " rabbit=" + rabbitEnabled
                + " server.name=" + configService.serverName());
    }

    // -------------------------------------------------------------------------
    // Banners / diagnostics
    // -------------------------------------------------------------------------

    private void showStartupBanner() {
        String version = plugin.getDescription().getVersion();
        plugin.getLogger().info("+------------------------------------------------------------+");
        plugin.getLogger().info("|          STARTING OREOESSENTIALS PREMIUM                   |");
        plugin.getLogger().info("|  Version: " + String.format("%-50s", version) + "|");
        plugin.getLogger().info("|          Loading all features and modules...               |");
        plugin.getLogger().info("+------------------------------------------------------------+");
    }
}
