package fr.elias.oessentials.kits.lifecycle;

import dev.oreo.modulith.core.PluginModule;
import fr.elias.oessentials.platform.modularity.BootstrapSupport;
import fr.elias.oessentials.platform.modularity.ManagedModule;
import fr.elias.oessentials.configuration.lifecycle.ConfigurationServices;

@PluginModule(value = "kits", dependencies = {"configuration::services"}, after = {"command-toggle"})
public final class KitsModule extends ManagedModule implements KitsServices {
    private fr.elias.oessentials.kits.internal.KitsManager kitsManager;

    @Override
    protected void start() {
        cleanup("kitsManager", () -> { if (kitsManager != null) kitsManager.saveData(); });
        initKits();
        providePublicApi(fr.elias.oessentials.api.IKitsAPI.class, this::getKitsApi);
        registerReloadStep(5, "kits", this::reloadKits);
        registerPlaceholders(new fr.elias.oessentials.kits.internal.KitsPlaceholders(plugin));
    }

    @Override public fr.elias.oessentials.kits.internal.KitsManager getKitsManager() { return kitsManager; }

    private void initKits() {
        boolean kitsFeature  = services(ConfigurationServices.class).getFeatureSettings().kitsEnabled();
        boolean kitsRegister = services(ConfigurationServices.class).getFeatureSettings().kitsCommandsEnabled();

        if (kitsFeature) {
            this.kitsManager = new fr.elias.oessentials.kits.internal.KitsManager(plugin);
            if (kitsRegister) {
                new fr.elias.oessentials.kits.internal.KitCommands(plugin, this.kitsManager);
                plugin.getLogger().info("[Kits] Loaded " + this.kitsManager.getKits().size() + " kits.");
            } else {
                BootstrapSupport.unregisterCommandHard(plugin, "kits");
                BootstrapSupport.unregisterCommandHard(plugin, "kit");
                plugin.getLogger().info("[Kits] Module loaded, commands NOT registered.");
            }
        } else {
            BootstrapSupport.unregisterCommandHard(plugin, "kits");
            BootstrapSupport.unregisterCommandHard(plugin, "kit");
            this.kitsManager = null;
            plugin.getLogger().info("[Kits] Disabled by config.");
        }
    }

    /** /oereload step (moved from ReloadAllCommand). */
    private fr.elias.oessentials.configuration.api.ReloadSteps.Outcome reloadKits(org.bukkit.command.CommandSender sender) {
        try {
            var target = plugin.getKitsManager();
            if (target != null) {
                target.reload();
                fr.elias.oessentials.shared.Lang.send(sender, "admin.reload.kits",
                        "<green>✔ Reloaded <white>kits.yml</white></green>");
                return fr.elias.oessentials.configuration.api.ReloadSteps.Outcome.RELOADED;
            }
            fr.elias.oessentials.shared.Lang.send(sender, "admin.reload.kits-skip",
                    "<yellow>• Skipped kits.yml (manager unavailable)</yellow>");
            return fr.elias.oessentials.configuration.api.ReloadSteps.Outcome.SKIPPED;
        } catch (Throwable t) {
            fr.elias.oessentials.shared.Lang.send(sender, "admin.reload.kits-skip",
                    "<yellow>• Skipped kits.yml: <gray>%error%</gray></yellow>",
                    java.util.Map.of("error", String.valueOf(t.getMessage())));
            return fr.elias.oessentials.configuration.api.ReloadSteps.Outcome.SKIPPED;
        }
    }
}
