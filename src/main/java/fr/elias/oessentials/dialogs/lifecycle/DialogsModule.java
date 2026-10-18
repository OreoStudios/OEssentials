package fr.elias.oessentials.dialogs.lifecycle;

import dev.oreo.modulith.core.PluginModule;
import fr.elias.oessentials.platform.modularity.ManagedModule;

@PluginModule(value = "dialogs", after = {"player-services"})
public final class DialogsModule extends ManagedModule implements DialogsServices {
    private fr.elias.oessentials.dialogs.internal.dialogs.OreoDialogManager dialogManager;

    @Override
    protected void start() {
        initDialogs();
    }

    @Override public fr.elias.oessentials.dialogs.internal.dialogs.OreoDialogManager getDialogManager() { return dialogManager; }

    private void initDialogs() { this.dialogManager = new fr.elias.oessentials.dialogs.internal.dialogs.OreoDialogManager(plugin); }
}
