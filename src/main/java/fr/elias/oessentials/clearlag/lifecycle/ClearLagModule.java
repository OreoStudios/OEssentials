package fr.elias.oessentials.clearlag.lifecycle;

import dev.oreo.modulith.core.PluginModule;
import fr.elias.oessentials.platform.modularity.BootstrapSupport;
import fr.elias.oessentials.platform.modularity.ManagedModule;
import fr.elias.oessentials.configuration.lifecycle.ConfigurationServices;
import fr.elias.oessentials.clearlag.internal.ClearLagManager;

@PluginModule(value = "clear-lag", dependencies = {"configuration::services"}, after = {"mobs"}, optional = true)
public final class ClearLagModule extends ManagedModule implements ClearLagServices {
    private ClearLagManager clearLag;

    @Override
    protected void start() {
        cleanup("clearLag", () -> { if (clearLag != null) clearLag.shutdown(); });
        initClearLag();
        providePublicApi(fr.elias.oessentials.api.IClearLagAPI.class, this::getClearLagApi);
    }

    @Override public ClearLagManager getClearLag() { return clearLag; }

    private void initClearLag() {
        if (services(ConfigurationServices.class).getFeatureSettings().clearLagEnabled()) {
            try {
                this.clearLag = new ClearLagManager(plugin);
                var olagg = new fr.elias.oessentials.clearlag.internal.ClearLagCommands(clearLag);
                services(ConfigurationServices.class).getCommands().registerLegacy("olagg", olagg);
                plugin.getLogger().info("[OreoLag] Enabled - /olagg active.");
            } catch (Throwable t) {
                plugin.getLogger().warning("[OreoLag] FAILED to initialize: " + t.getMessage());
                this.clearLag = null;
            }
        } else {
            BootstrapSupport.unregisterCommandHard(plugin, "olagg");
            this.clearLag = null;
            plugin.getLogger().info("[OreoLag] Disabled by settings.yml.");
        }
    }
}
