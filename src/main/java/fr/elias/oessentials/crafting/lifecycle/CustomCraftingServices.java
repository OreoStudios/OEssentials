package fr.elias.oessentials.crafting.lifecycle;

import dev.oreo.modulith.core.ModuleApi;
import fr.elias.oessentials.crafting.internal.customcraft.CraftActionsConfig;
import fr.elias.oessentials.crafting.internal.customcraft.CustomCraftingService;

@ModuleApi("services")
public interface CustomCraftingServices {
    CustomCraftingService getCustomCraftingService();
    CraftActionsConfig getCraftActionsConfig();
}
