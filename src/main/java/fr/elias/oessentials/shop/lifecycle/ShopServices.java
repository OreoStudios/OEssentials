package fr.elias.oessentials.shop.lifecycle;

import dev.oreo.modulith.core.ModuleApi;
import fr.elias.oessentials.shop.internal.ShopModule;

@ModuleApi("services")
public interface ShopServices {
    ShopModule getShopModule();

    /** The public OEssentials API for this module, or {@code null} while it is not available. */
    default fr.elias.oessentials.api.IShopAPI getShopApi() {
        var s = getShopModule();
        return s == null ? null : new fr.elias.oessentials.shop.internal.api.ShopAPIImpl(s);
    }
}
