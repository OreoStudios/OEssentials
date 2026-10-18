package fr.elias.oessentials.crafting.lifecycle;

import dev.oreo.modulith.core.PluginModule;
import fr.elias.oessentials.platform.modularity.ManagedModule;
import fr.elias.oessentials.configuration.lifecycle.ConfigurationServices;
import fr.elias.oessentials.inventory.lifecycle.InventoriesServices;
import fr.elias.oessentials.crafting.internal.customcraft.CustomCraftingService;
import fr.elias.oessentials.crafting.internal.customcraft.OeCraftCommand;

@PluginModule(value = "custom-crafting", dependencies = {"configuration::services", "inventories::services"}, after = {"trade"})
public final class CustomCraftingModule extends ManagedModule implements CustomCraftingServices {
    private CustomCraftingService customCraftingService;

    @Override
    protected void start() {
        initCustomCrafting();
    }

    @Override public CustomCraftingService getCustomCraftingService() { return customCraftingService; }

    private void initCustomCrafting() {
        this.customCraftingService = new CustomCraftingService(plugin);
        this.customCraftingService.loadAllAndRegister();

        plugin.getServer().getPluginManager().registerEvents(
                new fr.elias.oessentials.crafting.internal.customcraft.CustomCraftingListener(this.customCraftingService, services(ConfigurationServices.class).getCraftActionsConfig()), plugin);

        services(ConfigurationServices.class).getCommands().registerLegacy("oecraft", new OeCraftCommand(plugin, services(InventoriesServices.class).getInvManager(), customCraftingService));
    }
}
