package fr.elias.oessentials.configuration.api;

import dev.oreo.modulith.core.ModuleApi;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;

/**
 * Read access to {@code config/settings.yml}: feature switches and per-feature options.
 *
 * <p>Values are read on every call, so they reflect the last reload. Write through
 * {@link fr.elias.oessentials.configuration.lifecycle.ConfigurationServices#updateSettings}.</p>
 */
@ModuleApi("services")
public interface FeatureSettings {
    /** {@code features.<key>.enabled}, default {@code true}. */
    boolean isEnabled(String featureKey);

    /** {@code features.<key>.<subKey>}. */
    boolean featureOption(String featureKey, String subKey, boolean def);

    boolean getBoolean(String path, boolean def);

    /** The whole settings tree, for feature sections that have no typed accessor. Do not modify it. */
    ConfigurationSection getRoot();

    boolean currencyEnabled();
    boolean economyEnabled();
    boolean kitsEnabled();
    boolean kitsCommandsEnabled();
    boolean tradeEnabled();
    boolean tradeCrossServerEnabled();
    boolean chatEnabled();
    boolean chatDiscordBridgeEnabled();
    boolean bannedWordsEnabled();
    List<String> bannedWords();
    boolean isDialogEnabled();
    boolean isDialogEnabledFor(String feature);
    boolean playerVaultsEnabled();
    boolean portalsEnabled();
    boolean jumpPadsEnabled();
    boolean rtpEnabled();
    boolean rtpWarmupEnabled();
    int rtpWarmupSeconds();
    boolean bossbarEnabled();
    boolean scoreboardEnabled();
    boolean tabEnabled();
    boolean sitEnabled();
    boolean playtimeRewardsEnabled();
    boolean discordModerationEnabled();
    boolean oreoHologramsEnabled();
    boolean clearLagEnabled();
    boolean mobsEnabled();
    boolean mobsHealthbarEnabled();
    boolean worldShardingEnabled();
    boolean maintenanceEnabled();
    boolean shopEnabled();
    boolean helpEnabled();
    boolean homesEnabled();
    boolean playerWarpsEnabled();
    boolean tempFlyEnabled();

    /** {@code performance.ui-safeguards}: whether a UI module is switched off on the named server. */
    boolean uiModuleDisabledForServer(String serverName, String moduleKey);
}
