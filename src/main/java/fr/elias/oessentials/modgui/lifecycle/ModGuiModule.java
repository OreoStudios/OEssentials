package fr.elias.oessentials.modgui.lifecycle;

import dev.oreo.modulith.core.PluginModule;
import fr.elias.oessentials.platform.modularity.ManagedModule;
import fr.elias.oessentials.modgui.internal.world.WorldTweaksListener;

@PluginModule(value = "mod-gui", after = {"inventories"})
public final class ModGuiModule extends ManagedModule implements ModGuiServices {
    private fr.elias.oessentials.modgui.internal.ModGuiService modGuiService;

    @Override
    protected void start() {
        initModGui();
    }

    @Override public fr.elias.oessentials.modgui.internal.ModGuiService getModGuiService() { return modGuiService; }

    private void initModGui() {
        try {
            this.modGuiService = new fr.elias.oessentials.modgui.internal.ModGuiService(plugin);
            plugin.getLogger().info("[ModGUI] Server management GUI ready (/modgui).");
            new WorldTweaksListener(plugin);
        } catch (Throwable t) {
            plugin.getLogger().warning("[ModGUI] Failed to init: " + t.getMessage());
            this.modGuiService = null;
        }
    }
}
