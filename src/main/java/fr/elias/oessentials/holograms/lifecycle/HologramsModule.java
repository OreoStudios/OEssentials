package fr.elias.oessentials.holograms.lifecycle;

import dev.oreo.modulith.core.PluginModule;
import fr.elias.oessentials.platform.modularity.BootstrapSupport;
import fr.elias.oessentials.platform.modularity.ManagedModule;
import fr.elias.oessentials.configuration.lifecycle.ConfigurationServices;

@PluginModule(value = "holograms", dependencies = {"configuration::services"}, after = {"tab"}, optional = true)
public final class HologramsModule extends ManagedModule implements HologramsServices {
    private fr.elias.oessentials.holograms.internal.OHolograms embeddedOHolograms;

    @Override
    protected void start() {
        cleanup("embeddedOHolograms", () -> { if (embeddedOHolograms != null) embeddedOHolograms.shutdown(); });
        initHolograms();
        providePublicApi(fr.elias.oessentials.api.IHologramsAPI.class, this::getHologramsApi);
    }

    @Override public fr.elias.oessentials.holograms.internal.OHolograms getEmbeddedOHolograms() { return embeddedOHolograms; }

    private void initHolograms() {
        if (services(ConfigurationServices.class).getFeatureSettings().oreoHologramsEnabled()) {
            try {
                try {
                    Class.forName("org.bukkit.entity.Display");
                } catch (ClassNotFoundException x) {
                    plugin.getLogger().warning("[OHolograms] Display entities not available. Requires Paper/Folia.");
                    throw x;
                }

                BootstrapSupport.unregisterCommandHard(plugin, "ohologram");
                BootstrapSupport.unregisterCommandHard(plugin, "hologram");

                this.embeddedOHolograms = fr.elias.oessentials.holograms.internal.OHolograms.bootstrap(plugin);
                plugin.getLogger().info("[OHolograms] Embedded OHolograms enabled.");
            } catch (Throwable t) {
                if (fr.elias.oessentials.holograms.internal.OHolograms.canGet()) {
                    this.embeddedOHolograms = fr.elias.oessentials.holograms.internal.OHolograms.get();
                    plugin.getLogger().warning("[OHolograms] Non-fatal startup error after partial initialization: " + t.getMessage());
                    plugin.getLogger().info("[OHolograms] Embedded OHolograms kept active despite startup warning.");
                } else {
                    this.embeddedOHolograms = null;
                    plugin.getLogger().warning("[OHolograms] Failed to initialize: " + t.getMessage());
                }
            }
        } else {
            BootstrapSupport.unregisterCommandHard(plugin, "ohologram");
            BootstrapSupport.unregisterCommandHard(plugin, "hologram");
            this.embeddedOHolograms = null;
            plugin.getLogger().info("[OHolograms] Disabled by settings.yml.");
        }
    }
}
