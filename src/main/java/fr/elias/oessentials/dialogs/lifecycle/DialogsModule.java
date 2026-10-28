package fr.elias.oessentials.dialogs.lifecycle;

import dev.oreo.modulith.core.PluginModule;
import fr.elias.oessentials.platform.modularity.ManagedModule;

@PluginModule(value = "dialogs", dependencies = {"configuration::services", "storage::services"}, after = {"player-services"})
public final class DialogsModule extends ManagedModule implements DialogsServices {
    private fr.elias.oessentials.dialogs.internal.dialogs.OreoDialogManager dialogManager;

    @Override
    protected void start() {
        initDialogs();
    }

    @Override public fr.elias.oessentials.dialogs.internal.dialogs.OreoDialogManager getDialogManager() { return dialogManager; }

    private void initDialogs() {
        this.dialogManager = new fr.elias.oessentials.dialogs.internal.dialogs.OreoDialogManager(plugin);
        var storage = services(fr.elias.oessentials.storage.lifecycle.StorageServices.class);
        final var manager = this.dialogManager;
        storage.setDialogs(new fr.elias.oessentials.storage.api.StorageDialogs() {
            @Override public boolean openHomeDialog(org.bukkit.entity.Player player) { return storage.getHomes() != null && manager.openHomeDialog(player, storage.getHomes()); }
            @Override public boolean openWarpDialog(org.bukkit.entity.Player player) { return storage.getWarpsApi() != null && manager.openWarpDialog(player, storage.getWarpsApi()); }
            @Override public boolean confirmDelHome(org.bukkit.entity.Player player, String homeName) { return manager.confirmDelHome(player, homeName); }
            @Override public boolean confirmDelWarp(org.bukkit.entity.Player player, String warpName) { return manager.confirmDelWarp(player, warpName); }
            @Override public boolean confirmPlayerWarpRemove(org.bukkit.entity.Player player, String warpName) { return manager.confirmPlayerWarpRemove(player, warpName); }
        });
        cleanup("storageDialogs", () -> storage.setDialogs(null));
    }
}
