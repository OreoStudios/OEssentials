package fr.elias.oessentials.currency.lifecycle;

import dev.oreo.modulith.core.ModuleListener;
import dev.oreo.modulith.core.PluginModule;
import fr.elias.oessentials.platform.modularity.ManagedModule;
import fr.elias.oessentials.configuration.lifecycle.ConfigurationServices;
import fr.elias.oessentials.messaging.lifecycle.MessagingReady;
import fr.elias.oessentials.messaging.lifecycle.MessagingServices;
import fr.elias.oessentials.storage.lifecycle.StorageServices;
import fr.elias.oessentials.currency.internal.CurrencyConfig;
import fr.elias.oessentials.currency.internal.CurrencyService;
import fr.elias.oessentials.currency.internal.commands.CurrencyBalanceCommand;
import fr.elias.oessentials.currency.internal.commands.CurrencyBalanceTabCompleter;
import fr.elias.oessentials.currency.internal.commands.CurrencyCommand;
import fr.elias.oessentials.currency.internal.commands.CurrencyCommandTabCompleter;
import fr.elias.oessentials.currency.internal.commands.CurrencySendCommand;
import fr.elias.oessentials.currency.internal.commands.CurrencySendTabCompleter;
import fr.elias.oessentials.currency.internal.commands.CurrencyTopCommand;
import fr.elias.oessentials.currency.internal.commands.CurrencyTopTabCompleter;
import fr.elias.oessentials.currency.internal.storage.CurrencyStorage;
import fr.elias.oessentials.currency.internal.storage.JsonCurrencyStorage;
import fr.elias.oessentials.currency.internal.storage.MongoCurrencyStorage;
import org.bukkit.Bukkit;

@PluginModule(value = "currency-system", dependencies = {"configuration::services", "messaging::events", "messaging::services", "storage::services"})
public final class CurrencySystemModule extends ManagedModule implements CurrencySystemServices {
    private volatile fr.elias.oessentials.currency.internal.placeholders.CurrencyPlaceholderExpansion currencyPlaceholders;
    private CurrencyService currencyService;
    private CurrencyConfig currencyConfig;

    @Override
    protected void start() {
        services(MessagingServices.class).registerNamespace(new fr.elias.oessentials.currency.internal.rabbitmq.CurrencyPacketNamespace());
        initCurrencySystem();
        listenForModuleEvents(this);
        registerCommands();
        registerCurrencyPlaceholders();
    }

    @Override public CurrencyService getCurrencyService() { return currencyService; }
    @Override public fr.elias.oessentials.currency.internal.placeholders.CurrencyPlaceholderExpansion getCurrencyPlaceholders() { return currencyPlaceholders; }
    @Override public CurrencyConfig getCurrencyConfig() { return currencyConfig; }

    // -------------------------------------------------------------------------
    // Currency
    // -------------------------------------------------------------------------

    private void initCurrencySystem() {
        if (!services(ConfigurationServices.class).getFeatureSettings().currencyEnabled()) { plugin.getLogger().info("[Currency] Disabled by settings.yml"); return; }
        try {
            this.currencyConfig = new CurrencyConfig(plugin);
            final CurrencyStorage currencyStorage;
            if (currencyConfig.useMongoStorage() && services(StorageServices.class).getHomesMongoClient() != null) {
                String dbName = plugin.getConfig().getString("storage.mongo.database", "oreo");
                String prefix = plugin.getConfig().getString("storage.mongo.collectionPrefix", "oreo_");
                currencyStorage = new MongoCurrencyStorage(services(StorageServices.class).getHomesMongoClient(), dbName, prefix);
                plugin.getLogger().info("[Currency] Using MongoDB storage");
            } else {
                currencyStorage = new JsonCurrencyStorage(plugin);
                plugin.getLogger().info("[Currency] Using JSON storage");
            }
            this.currencyService = new CurrencyService(plugin, currencyStorage, currencyConfig);
            providePublicApi(fr.elias.oessentials.configuration.api.CurrencyApis.class, () -> new fr.elias.oessentials.configuration.api.CurrencyApis() {
                @Override public fr.elias.oessentials.api.ICurrencyAPI currency(String currencyId) { return getCurrencyApi(currencyId); }
                @Override public java.util.List<fr.elias.oessentials.api.ICurrencyAPI> currencies() { return getCurrencyApis(); }
            });
            services(ConfigurationServices.class).getCommands().register(new CurrencyCommand(plugin)).register(new CurrencyBalanceCommand(plugin)).register(new CurrencySendCommand(plugin)).register(new CurrencyTopCommand(plugin));
            services(ConfigurationServices.class).getCommands().rewireTab("currency",        new CurrencyCommandTabCompleter(plugin));
            services(ConfigurationServices.class).getCommands().rewireTab("currencybalance", new CurrencyBalanceTabCompleter(plugin));
            services(ConfigurationServices.class).getCommands().rewireTab("currencysend",    new CurrencySendTabCompleter(plugin));
            services(ConfigurationServices.class).getCommands().rewireTab("currencytop",     new CurrencyTopTabCompleter(plugin));
            plugin.getLogger().info("[Currency] Enabled");
        } catch (Throwable t) {
            plugin.getLogger().severe("[Currency] Failed to initialize: " + t.getMessage());
            t.printStackTrace();
        }
    }

    @ModuleListener
    public void onMessagingReady(MessagingReady event) {
        if (!isActive() || currencyService == null || !currencyConfig.isCrossServerEnabled()) return;
        event.packets().subscribe(fr.elias.oessentials.currency.internal.rabbitmq.CurrencySyncPacket.class,
                (channel, packet) -> currencyService.handleCurrencySync(packet));
        plugin.getLogger().info("[Currency] Cross-server sync subscribed.");
    }

    /** Commands this module owns (registered by the commands module until 7.1.6). */
    private void registerCommands() {
        var commands = services(fr.elias.oessentials.configuration.lifecycle.ConfigurationServices.class).getCommands();
        commands.register(new fr.elias.oessentials.currency.internal.commands.CurrencyAdminCommand(plugin));
    }

    /** %oreocurrency_...% placeholders (registered by integrations until 7.1.6, with the same delay). */
    private void registerCurrencyPlaceholders() {
        if (currencyService == null || plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") == null) return;
        fr.elias.oessentials.platform.scheduling.OreScheduler.runLater(plugin, () -> {
            if (!isActive()) return;
            try {
                var expansion = new fr.elias.oessentials.currency.internal.placeholders.CurrencyPlaceholderExpansion(plugin);
                if (expansion.register()) {
                    this.currencyPlaceholders = expansion;
                    plugin.getLogger().info("[OK] PlaceholderAPI currency expansion registered!");
                } else {
                    plugin.getLogger().warning("[FAIL] Failed to register PlaceholderAPI currency expansion!");
                }
            } catch (Throwable t) {
                plugin.getLogger().warning("[Currency] PlaceholderAPI expansion failed: " + t.getMessage());
            }
        }, 60L);
        cleanup("currencyPlaceholders", () -> { if (currencyPlaceholders != null) currencyPlaceholders.unregister(); });
    }
}
