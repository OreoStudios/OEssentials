package fr.elias.oessentials.shop.internal;

import fr.elias.oessentials.OEssentials;
import fr.elias.oessentials.shop.internal.commands.SellCommand;
import fr.elias.oessentials.shop.internal.commands.ShopCommand;
import fr.elias.oessentials.shop.internal.gui.AmountSelectionGUI;
import fr.elias.oessentials.shop.internal.gui.MainMenuGUI;
import fr.elias.oessentials.shop.internal.gui.ShopGUI;
import fr.elias.oessentials.shop.internal.gui.TransactionProcessor;
import fr.elias.oessentials.platform.hooks.ItemsAdderHook;
import fr.elias.oessentials.platform.hooks.NexoHook;
import fr.elias.oessentials.shop.internal.listeners.AntiDupeListener;
import fr.elias.oessentials.shop.internal.logger.TransactionLogger;
import fr.elias.oessentials.shop.internal.managers.DynamicPricingManager;
import fr.elias.oessentials.shop.internal.managers.PriceModifierManager;
import fr.elias.oessentials.shop.internal.managers.ShopManager;
import fr.elias.oessentials.shop.internal.rotation.ShopRotationManager;


public final class ShopModule {

    private final OEssentials plugin;

    private ShopConfig            shopConfig;
    private ShopManager           shopManager;
    private ShopRotationManager   rotationManager;
    private ShopEconomy           economy;
    private PriceModifierManager  priceModifierManager;
    private DynamicPricingManager dynamicPricingManager;
    private TransactionLogger     transactionLogger;

    private ItemsAdderHook itemsAdderHook;
    private NexoHook       nexoHook;

    private MainMenuGUI          mainMenuGUI;
    private ShopGUI              shopGUI;
    private AmountSelectionGUI   amountSelectionGUI;
    private TransactionProcessor transactionProcessor;


    private boolean enabled = false;

    private static ShopModule activeInstance;
    public  static ShopModule getActive() { return activeInstance; }


    public ShopModule(OEssentials plugin) {
        this.plugin = plugin;
        load();
    }


    private void load() {
        this.shopConfig = new ShopConfig(plugin);

        if (!shopConfig.isEnabled()) {
            plugin.getLogger().info("[Shop] Disabled by config.");
            enabled = false;
            return;
        }

        this.itemsAdderHook = new ItemsAdderHook(plugin);
        this.nexoHook       = new NexoHook(plugin);

        this.economy              = new ShopEconomy(plugin);
        this.priceModifierManager = new PriceModifierManager(this);
        this.dynamicPricingManager = new DynamicPricingManager(this);
        this.transactionLogger    = new TransactionLogger(this);
        this.shopManager          = new ShopManager(this);
        this.rotationManager      = new ShopRotationManager(this);
        this.rotationManager.loadAll(shopManager.getAllShops());

        this.mainMenuGUI          = new MainMenuGUI(this);
        this.shopGUI              = new ShopGUI(this);
        this.amountSelectionGUI   = new AmountSelectionGUI(this);
        this.transactionProcessor = new TransactionProcessor(this);

        plugin.getServer().getPluginManager()
                .registerEvents(new AntiDupeListener(this), plugin);

        registerCommands();

        enabled = true;
        activeInstance = this;

        plugin.getLogger().info("[Shop] Module loaded — " +
                shopManager.getShopCount() + " shop(s), economy=" +
                economy.getEconomyName() + ", dynamic-pricing=" +
                (dynamicPricingManager.isEnabled() ? "on" : "off"));
    }


    public void reload() {
        shutdown();
        load();
    }


    public void shutdown() {
        if (dynamicPricingManager != null) dynamicPricingManager.shutdown();
        if (transactionLogger     != null) transactionLogger.close();
        activeInstance = null;
        enabled = false;
    }


    private void registerCommands() {
        ShopCommand shopCmd = new ShopCommand(this);
        plugin.getCommands().registerLegacy("shop", shopCmd, shopCmd);

        SellCommand sellCmd = new SellCommand(this);
        plugin.getCommands().registerLegacy("sell", sellCmd, sellCmd);
    }


    public boolean isEnabled()                              { return enabled; }
    public OEssentials getPlugin()                       { return plugin; }
    public ShopConfig getShopConfig()                       { return shopConfig; }
    public ShopEconomy getEconomy()                         { return economy; }
    public ShopManager getShopManager()                     { return shopManager; }
    public ShopRotationManager getRotationManager()         { return rotationManager; }
    public PriceModifierManager getPriceModifierManager()   { return priceModifierManager; }
    public DynamicPricingManager getDynamicPricingManager() { return dynamicPricingManager; }
    public TransactionLogger getTransactionLogger()         { return transactionLogger; }
    public MainMenuGUI getMainMenuGUI()                     { return mainMenuGUI; }
    public ShopGUI getShopGUI()                             { return shopGUI; }
    public AmountSelectionGUI getAmountSelectionGUI()       { return amountSelectionGUI; }
    public TransactionProcessor getTransactionProcessor()   { return transactionProcessor; }



    public ItemsAdderHook getItemsAdderHook() { return itemsAdderHook; }
    public NexoHook       getNexoHook()       { return nexoHook; }

    public boolean hasItemsAdder() {
        return itemsAdderHook != null && itemsAdderHook.isEnabled();
    }

    public boolean hasNexo() {
        return nexoHook != null && nexoHook.isEnabled();
    }
}