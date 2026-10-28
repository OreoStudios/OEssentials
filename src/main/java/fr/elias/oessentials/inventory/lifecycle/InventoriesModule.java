package fr.elias.oessentials.inventory.lifecycle;

import dev.oreo.modulith.core.PluginModule;
import fr.elias.oessentials.messaging.lifecycle.MessagingServices;
import fr.elias.oessentials.platform.modularity.ManagedModule;
import fr.elias.oessentials.commandtoggle.lifecycle.CommandToggleServices;
import fr.elias.oessentials.configuration.lifecycle.ConfigurationServices;
import fr.elias.oessentials.inventory.internal.auctionhouse.AuctionHouseModule;
import fr.elias.oessentials.inventory.internal.auctionhouse.commands.AdminCommand;
import fr.elias.oessentials.inventory.internal.auctionhouse.commands.AuctionHouseCommand;
import fr.elias.oessentials.inventory.internal.auctionhouse.commands.ExpiredCommand;
import fr.elias.oessentials.inventory.internal.auctionhouse.commands.SearchCommand;
import fr.elias.oessentials.inventory.internal.auctionhouse.commands.SellCommand;
import fr.elias.oessentials.inventory.internal.auctionhouse.commands.SoldCommand;
import fr.elias.oessentials.inventory.internal.auctionhouse.hooks.AuctionPlaceholders;
import fr.elias.oessentials.inventory.internal.sellgui.manager.SellGuiManager;
import fr.minuskube.inv.InventoryManager;
import java.io.File;
import org.bukkit.Bukkit;

@PluginModule(value = "inventories", dependencies = {"command-toggle::services", "configuration::services", "messaging::services", "storage::services"}, uses = {"currency-system::services", "shop::services"}, after = {"mail"})
public final class InventoriesModule extends ManagedModule implements InventoriesServices {
    private AuctionHouseModule auctionHouse;
    private fr.elias.oessentials.inventory.internal.orders.OrdersModule ordersModule;
    private InventoryManager invManager;
    private SellGuiManager sellGuiManager;

    @Override
    protected void start() {
        cleanup("auctionHouse", () -> { if (auctionHouse != null) auctionHouse.stop(); });
        cleanup("ordersModule", () -> { if (ordersModule != null) ordersModule.stop(); });
        services(MessagingServices.class).registerNamespace(new fr.elias.oessentials.inventory.internal.InventoriesPacketNamespace());
        initInventoryManagers();
        subscribeCrossServerSync();
        registerCommands();
        providePublicApi(fr.elias.oessentials.api.ISellGuiAPI.class, this::getSellGuiApi);
        providePublicApi(fr.elias.oessentials.api.IOrdersAPI.class, this::getOrdersApi);
        providePublicApi(fr.elias.oessentials.api.IAuctionHouseAPI.class, this::getAuctionHouseApi);
    }

    private void subscribeCrossServerSync() {
        var messaging = services(MessagingServices.class);
        if (auctionHouse != null && auctionHouse.enabled()) {
            messaging.subscribe(
                    fr.elias.oessentials.inventory.internal.auctionhouse.rabbitmq.AuctionSyncPacket.class,
                    (channel, pkt) -> {
                        try { auctionHouse.applyIncomingSync(pkt); }
                        catch (Throwable t) { plugin.getLogger().warning("[AH] Failed to handle AuctionSyncPacket: " + t.getMessage()); }
                    });
        }
        if (ordersModule != null && ordersModule.enabled()) {
            messaging.subscribe(
                    fr.elias.oessentials.inventory.internal.orders.rabbitmq.OrderSyncPacket.class,
                    (channel, pkt) -> {
                        try { ordersModule.getEventBus().onReceive(pkt); }
                        catch (Throwable t) { plugin.getLogger().warning("[Orders] Failed to handle OrderSyncPacket: " + t.getMessage()); }
                    });
        }
    }

    @Override public AuctionHouseModule getAuctionHouse() { return auctionHouse; }
    @Override public fr.elias.oessentials.inventory.internal.orders.OrdersModule getOrdersModule() { return ordersModule; }
    @Override public InventoryManager getInvManager() { return invManager; }
    @Override public SellGuiManager getSellGuiManager() { return sellGuiManager; }

    private void initInventoryManagers() {
        this.invManager    = new InventoryManager(plugin);
        this.invManager.init();
        this.sellGuiManager = new SellGuiManager(plugin, this.invManager);

        this.auctionHouse = new AuctionHouseModule(plugin);
        if (auctionHouse.enabled()) {
            services(ConfigurationServices.class).getCommands().registerLegacy("ah",        new AuctionHouseCommand(auctionHouse));
            services(ConfigurationServices.class).getCommands().registerLegacy("ahs",       new SellCommand(auctionHouse));
            services(ConfigurationServices.class).getCommands().registerLegacy("ahsearch",  new SearchCommand(auctionHouse));
            services(ConfigurationServices.class).getCommands().registerLegacy("ahexpired", new ExpiredCommand(auctionHouse));
            services(ConfigurationServices.class).getCommands().registerLegacy("ahsold",    new SoldCommand(auctionHouse));
            services(ConfigurationServices.class).getCommands().registerLegacy("ahadmin",   new AdminCommand(auctionHouse));
            if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) new AuctionPlaceholders(auctionHouse).register();
        }

        var toggles = services(CommandToggleServices.class);
        if (toggles.getCommandToggleConfig() != null) {
            toggles.onCommandToggled("auctionhouse", () -> {
                boolean shouldBeEnabled = toggles.isCommandEnabled("auctionhouse");
                File ahConfig = new File(plugin.getDataFolder(), "config/auctionhouse/config.yml");
                org.bukkit.configuration.file.YamlConfiguration ahCfg = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(ahConfig);
                ahCfg.set("enabled", shouldBeEnabled);
                try { ahCfg.save(ahConfig); } catch (Exception e) { plugin.getLogger().warning("[CommandToggle] Failed to save AH config: " + e.getMessage()); }
                auctionHouse.reload();
                plugin.getLogger().info("[CommandToggle] AuctionHouse " + (shouldBeEnabled ? "enabled" : "disabled") + " via toggle.");
            });
            plugin.getLogger().info("[CommandToggle] AuctionHouse module callback registered.");
        }

        // Orders / Market module
        try {
            this.ordersModule = new fr.elias.oessentials.inventory.internal.orders.OrdersModule(plugin);
            if (ordersModule.enabled()) {
                plugin.getLogger().info("[Orders] Market module initialised.");
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("[Orders] Failed to initialise: " + t.getMessage());
            this.ordersModule = null;
        }
    }

    /** Commands this module owns (registered by the commands module until 7.1.6). */
    private void registerCommands() {
        var commands = services(fr.elias.oessentials.configuration.lifecycle.ConfigurationServices.class).getCommands();
        commands.register(new fr.elias.oessentials.inventory.internal.sellgui.command.SellGuiCommand(plugin));
        // Orders / Market command
        if (ordersModule != null) {
            commands.register(new fr.elias.oessentials.inventory.internal.orders.command.OrderCommand(ordersModule));
        }
    }
}
