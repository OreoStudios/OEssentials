package fr.elias.oessentials.configuration.lifecycle;

import com.google.gson.Gson;
import dev.oreo.modulith.core.ModuleApi;
import fr.elias.oessentials.api.ITempFlyAPI;
import fr.elias.oessentials.configuration.api.AutoReboot;
import fr.elias.oessentials.configuration.api.BossBarToggles;
import fr.elias.oessentials.configuration.api.CrossServerFeatures;
import fr.elias.oessentials.configuration.api.FeatureSettings;
import fr.elias.oessentials.configuration.api.PerformanceGuardSettings;
import fr.elias.oessentials.configuration.api.SafeZoneRelay;
import fr.elias.oessentials.configuration.api.ServerConnector;
import fr.elias.oessentials.configuration.api.ServerConfig;
import fr.elias.oessentials.platform.commands.CommandManager;
import java.util.Map;

@ModuleApi("services")
public interface ConfigurationServices {
    FeatureSettings getFeatureSettings();
    PerformanceGuardSettings getPerformanceGuardSettings();
    ServerConfig getServerConfig();
    CrossServerFeatures getCrossServerFeatures();

    /** Reloads {@code config/settings.yml} and the cross-server switches read from it. */
    void reloadSettings();

    /** Writes the given keys to {@code config/settings.yml}, saves it and reloads it. */
    void updateSettings(Map<String, Object> values);

    /** Temporary flight, or {@code null} when {@code features.tempfly} is disabled. */
    ITempFlyAPI getTempFly();

    AutoReboot getAutoReboot();

    /** Registers how auto-reboot reaches other servers, or {@code null} to remove it. */
    void setSafeZoneRelay(SafeZoneRelay relay);

    /** Registers how players are sent to other servers, or {@code null} to remove it. */
    void setServerConnector(ServerConnector connector);

    /** How players are sent to other servers, or {@code null} before proxy-messaging starts. */
    ServerConnector getServerConnector();

    /** Modules' parts of the public OEssentials API. */
    fr.elias.oessentials.configuration.api.PublicApis getPublicApis();

    /** The module steps of /oereload. */
    fr.elias.oessentials.configuration.api.ReloadSteps getReloadSteps();

    /** Which module reloads which configuration file. */
    fr.elias.oessentials.configuration.api.ConfigFiles getConfigFiles();

    /** %oreo_...% placeholders contributed by modules. */
    fr.elias.oessentials.configuration.api.Placeholders getPlaceholders();

    /** Registers the boss bars shown in the {@code /oesettings} menu, or {@code null} to remove them. */
    void setBossBarToggles(BossBarToggles toggles);

    CommandManager getCommands();
    boolean getEconomyEnabled();
    boolean getRedisEnabled();
    boolean getRabbitEnabled();
    Gson getGson();
}
