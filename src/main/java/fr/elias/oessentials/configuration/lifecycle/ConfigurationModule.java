package fr.elias.oessentials.configuration.lifecycle;

import com.google.gson.Gson;
import dev.oreo.modulith.core.PluginModule;
import fr.elias.oessentials.platform.modularity.BootstrapSupport;
import fr.elias.oessentials.platform.modularity.ManagedModule;
import fr.elias.oessentials.platform.commands.CommandManager;
import fr.elias.oessentials.configuration.internal.config.ConfigService;
import fr.elias.oessentials.configuration.internal.config.CrossServerSettings;
import fr.elias.oessentials.configuration.internal.config.SettingsConfig;
import fr.elias.oessentials.configuration.api.BossBarToggles;
import fr.elias.oessentials.configuration.api.SafeZoneRelay;
import fr.elias.oessentials.configuration.api.ServerConnector;
import fr.elias.oessentials.configuration.internal.config.OeSettingsCommand;
import fr.elias.oessentials.configuration.internal.autoreboot.AutoRebootService;
import fr.elias.oessentials.configuration.internal.tempfly.TempFlyCommand;
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
    private volatile SafeZoneRelay safeZoneRelay;
    private volatile ServerConnector serverConnector;
    private final fr.elias.oessentials.configuration.internal.config.PublicApiRegistry publicApis = new fr.elias.oessentials.configuration.internal.config.PublicApiRegistry();
    private final fr.elias.oessentials.configuration.internal.config.ReloadStepRegistry reloadSteps = new fr.elias.oessentials.configuration.internal.config.ReloadStepRegistry();
    private volatile BossBarToggles bossBarToggles;
    private CommandManager commands;
    private boolean economyEnabled;
    private boolean redisEnabled;
    private boolean rabbitEnabled;
    private volatile CrossServerSettings crossServerSettings;
    private Gson gson = new Gson();

    @Override
    protected void start() {
        cleanup("commands", () -> { if (commands != null) commands.shutdown(); });
        cleanup("autoRebootService", () -> { if (autoRebootService != null) autoRebootService.stop(); });
        cleanup("tempFlyService", () -> { if (tempFlyService != null) tempFlyService.shutdown(); });
        initConfig();
        publicApis.provide(fr.elias.oessentials.api.ITempFlyAPI.class, this::getTempFly);
        publicApis.provide(fr.elias.oessentials.api.IAutoRebootAPI.class, this::getAutoReboot);
    }

    @Override public SettingsConfig getFeatureSettings() { return settingsConfig; }
    @Override public SettingsConfig getPerformanceGuardSettings() { return settingsConfig; }
    @Override public ConfigService getServerConfig() { return configService; }
    @Override public CrossServerSettings getCrossServerFeatures() { return crossServerSettings; }
    @Override public fr.elias.oessentials.configuration.internal.tempfly.TempFlyService getTempFly() { return tempFlyService; }
    @Override public AutoRebootService getAutoReboot() { return autoRebootService; }
    @Override public void setSafeZoneRelay(SafeZoneRelay relay) { this.safeZoneRelay = relay; }
    @Override public void setServerConnector(ServerConnector connector) { this.serverConnector = connector; }
    @Override public ServerConnector getServerConnector() { return serverConnector; }
    @Override public fr.elias.oessentials.configuration.api.PublicApis getPublicApis() { return publicApis; }
    @Override public fr.elias.oessentials.configuration.api.ReloadSteps getReloadSteps() { return reloadSteps; }
    @Override public void setBossBarToggles(BossBarToggles toggles) { this.bossBarToggles = toggles; }
    @Override public CommandManager getCommands() { return commands; }
    @Override public boolean getEconomyEnabled() { return economyEnabled; }
    @Override public boolean getRedisEnabled() { return redisEnabled; }
    @Override public boolean getRabbitEnabled() { return rabbitEnabled; }
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

        plugin.getLogger().info("[Economy] " + (economyEnabled ? "Enabled" : "Disabled") + " via settings.yml");

        this.autoRebootService = new AutoRebootService(plugin, () -> safeZoneRelay, () -> serverConnector);
        this.autoRebootService.start();


        this.commands = new CommandManager(plugin);

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

        this.commands.register(new OeSettingsCommand(plugin, () -> bossBarToggles));
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
