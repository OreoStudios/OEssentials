package fr.elias.oessentials.shop.lifecycle;

import dev.oreo.modulith.core.PluginModule;
import fr.elias.oessentials.platform.modularity.ManagedModule;
import fr.elias.oessentials.configuration.lifecycle.ConfigurationServices;

@PluginModule(value = "shop", dependencies = {"configuration::services"}, after = {"clear-lag"})
public final class ShopModule extends ManagedModule implements ShopServices {
    private fr.elias.oessentials.shop.internal.ShopModule shopModule;

    @Override
    protected void start() {
        cleanup("shopModule", () -> { if (shopModule != null) shopModule.shutdown(); });
        initShop();
        providePublicApi(fr.elias.oessentials.api.IShopAPI.class, this::getShopApi);
    }

    @Override public fr.elias.oessentials.shop.internal.ShopModule getShopModule() { return shopModule; }

    private void initShop() {
        if (!services(ConfigurationServices.class).getFeatureSettings().shopEnabled()) {
            plugin.getLogger().info("[Shop] Disabled by settings.yml");
            return;
        }
        try {
            this.shopModule = new fr.elias.oessentials.shop.internal.ShopModule(plugin);
            if (shopModule.isEnabled()) {
                plugin.getLogger().info("[Shop] Module ready.");
            }
        } catch (Throwable t) {
            plugin.getLogger().severe("[Shop] Failed to initialise: " + t.getMessage());
            t.printStackTrace();
            this.shopModule = null;
        }
    }
}
