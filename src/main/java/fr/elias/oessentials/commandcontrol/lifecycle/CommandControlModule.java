package fr.elias.oessentials.commandcontrol.lifecycle;

import dev.oreo.modulith.core.PluginModule;
import fr.elias.oessentials.platform.modularity.ManagedModule;
import org.bukkit.Bukkit;

@PluginModule(value = "command-control", dependencies = {"configuration::services"}, after = {"economy"})
public final class CommandControlModule extends ManagedModule implements CommandControlServices {
    private fr.elias.oessentials.commandcontrol.internal.CommandControlService commandControlService;

    @Override
    protected void start() {
        initCommandControl();
        providePublicApi(fr.elias.oessentials.api.ICommandControlAPI.class, this::getCommandControlApi);
    }

    @Override public fr.elias.oessentials.commandcontrol.internal.CommandControlService getCommandControlService() { return commandControlService; }

    private void initCommandControl() {
        try {
            java.io.File f = new java.io.File(plugin.getDataFolder(), "config/commandsmodule/command-control.yml");
            if (!f.exists()) { f.getParentFile().mkdirs(); plugin.saveResource("config/commandsmodule/command-control.yml", false); }
            var yml = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(f);
            this.commandControlService = new fr.elias.oessentials.commandcontrol.internal.CommandControlService();
            commandControlService.load(yml);
            if (commandControlService.isEnabled()) {
                Bukkit.getPluginManager().registerEvents(new fr.elias.oessentials.commandcontrol.internal.CommandControlListener(plugin, commandControlService), plugin);
                if (commandControlService.isHideFromTab()) {
                    Bukkit.getPluginManager().registerEvents(new fr.elias.oessentials.commandcontrol.internal.CommandControlTabHideListener(commandControlService), plugin);
                }
                plugin.getLogger().info("[CommandControl] Enabled (hideFromTab=" + commandControlService.isHideFromTab() + ")");
            } else {
                plugin.getLogger().info("[CommandControl] Disabled.");
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("[CommandControl] Failed to init: " + t.getMessage());
        }

        try {
            java.io.File f = new java.io.File(plugin.getDataFolder(), "config/server/clearlag.yml");
            if (!f.exists()) { f.getParentFile().mkdirs(); plugin.saveResource("config/server/clearlag.yml", false); }
        } catch (Throwable ignored) {}
    }
}
