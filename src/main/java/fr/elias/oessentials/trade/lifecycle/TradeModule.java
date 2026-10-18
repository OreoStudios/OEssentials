package fr.elias.oessentials.trade.lifecycle;

import dev.oreo.modulith.core.PluginModule;
import fr.elias.oessentials.platform.modularity.BootstrapSupport;
import fr.elias.oessentials.platform.modularity.ManagedModule;
import fr.elias.oessentials.configuration.lifecycle.ConfigurationServices;
import fr.elias.oessentials.trade.internal.command.TradeCommand;
import fr.elias.oessentials.trade.internal.config.TradeConfig;
import fr.elias.oessentials.trade.internal.service.TradeService;

@PluginModule(value = "trade", dependencies = {"configuration::services"}, after = {"notes"})
public final class TradeModule extends ManagedModule implements TradeServices {
    private TradeConfig tradeConfig;
    private TradeService tradeService;

    @Override
    protected void start() {
        cleanup("tradeService", () -> { if (tradeService != null) tradeService.cancelAll(); });
        initTrade();
    }

    @Override public TradeConfig getTradeConfig() { return tradeConfig; }
    @Override public TradeService getTradeService() { return tradeService; }

    private void initTrade() {
        this.tradeConfig = new TradeConfig(plugin);

        if (this.tradeConfig.enabled && services(ConfigurationServices.class).getSettingsConfig().tradeEnabled()) {
            this.tradeService = new TradeService(plugin, this.tradeConfig);
            services(ConfigurationServices.class).getCommands().registerLegacy("trade", new TradeCommand(plugin, this.tradeService));
            plugin.getLogger().info("[Trade] Enabled.");
            org.bukkit.Bukkit.getPluginManager().registerEvents(new fr.elias.oessentials.trade.internal.ui.TradeGuiGuardListener(plugin), plugin);
            plugin.getServer().getPluginManager().registerEvents(new fr.elias.oessentials.trade.internal.ui.TradeInventoryCloseListener(plugin), plugin);
        } else {
            this.tradeService = null;
            BootstrapSupport.unregisterCommandHard(plugin, "trade");
            plugin.getLogger().info("[Trade] Disabled (trade.yml or settings.yml).");
        }
    }
}
