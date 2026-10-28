package fr.elias.oessentials.crafting.lifecycle;

import dev.oreo.modulith.core.ModuleApi;
import fr.elias.oessentials.crafting.internal.customcraft.CraftActionsConfig;
import fr.elias.oessentials.crafting.internal.customcraft.CustomCraftingService;

@ModuleApi("services")
public interface CustomCraftingServices {
    CustomCraftingService getCustomCraftingService();
    CraftActionsConfig getCraftActionsConfig();

    /** The public OEssentials API for this module, or {@code null} while it is not available. */
    default fr.elias.oessentials.api.ICustomCraftAPI getCustomCraftApi() {
        var s = getCustomCraftingService();
        return s == null ? null : new fr.elias.oessentials.crafting.internal.api.CustomCraftAPIImpl(s);
    }

    default int customRecipeCount() {
        var service = getCustomCraftingService();
        return service == null ? 0 : service.getRecipeCount();
    }
}
