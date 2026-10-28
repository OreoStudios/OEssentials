package fr.elias.oessentials.configuration.lifecycle;

import com.google.gson.Gson;
import dev.oreo.modulith.core.ModuleApi;
import fr.elias.oessentials.api.ITempFlyAPI;
import fr.elias.oessentials.configuration.api.AutoReboot;
import fr.elias.oessentials.configuration.api.CrossServerFeatures;
import fr.elias.oessentials.configuration.api.FeatureSettings;
import fr.elias.oessentials.configuration.api.PerformanceGuardSettings;
import fr.elias.oessentials.configuration.api.SafeZoneRelay;
import fr.elias.oessentials.configuration.api.ServerConfig;
import fr.elias.oessentials.platform.commands.CommandManager;
import fr.elias.oessentials.crafting.internal.customcraft.CraftActionsConfig;
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

    CraftActionsConfig getCraftActionsConfig();
    CommandManager getCommands();
    boolean getEconomyEnabled();
    boolean getRedisEnabled();
    boolean getRabbitEnabled();
    Gson getGson();
}
