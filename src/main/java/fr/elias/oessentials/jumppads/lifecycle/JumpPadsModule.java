package fr.elias.oessentials.jumppads.lifecycle;

import dev.oreo.modulith.core.PluginModule;
import fr.elias.oessentials.platform.modularity.ManagedModule;
import fr.elias.oessentials.configuration.lifecycle.ConfigurationServices;

@PluginModule(value = "jump-pads", dependencies = {"configuration::services"}, after = {"group-rtp"}, optional = true)
public final class JumpPadsModule extends ManagedModule implements JumpPadsServices {
    private fr.elias.oessentials.jumppads.internal.jumpads.JumpPadsManager jumpPads;

    @Override
    protected void start() {
        initJumpPads();
    }

    @Override public fr.elias.oessentials.jumppads.internal.jumpads.JumpPadsManager getJumpPads() { return jumpPads; }

    private void initJumpPads() {
        this.jumpPads = new fr.elias.oessentials.jumppads.internal.jumpads.JumpPadsManager(plugin);
        plugin.getServer().getPluginManager().registerEvents(new fr.elias.oessentials.jumppads.internal.jumpads.JumpPadsListener(this.jumpPads), plugin);
        var jumpCmd = new fr.elias.oessentials.jumppads.internal.jumpads.JumpPadsCommand(this.jumpPads);
        services(ConfigurationServices.class).getCommands().registerLegacy("jumpad", jumpCmd, jumpCmd);
    }
}
