package fr.elias.oessentials.economy.lifecycle;

import dev.oreo.modulith.core.PluginModule;
import fr.elias.oessentials.platform.modularity.BootstrapSupport;
import fr.elias.oessentials.platform.modularity.ManagedModule;
import fr.elias.oessentials.configuration.lifecycle.ConfigurationServices;
import fr.elias.oessentials.redis.lifecycle.RedisServices;
import fr.elias.oessentials.economy.internal.persistence.database.JsonEconomyDatabase;
import fr.elias.oessentials.economy.internal.persistence.database.MongoDBManager;
import fr.elias.oessentials.economy.internal.persistence.database.PlayerEconomyDatabase;
import fr.elias.oessentials.storage.api.BalanceImportTarget;
import fr.elias.oessentials.storage.lifecycle.StorageServices;
import fr.elias.oessentials.messaging.lifecycle.MessagingServices;
import fr.elias.oessentials.economy.internal.cross.rabbit.PlayerJoinPacket;
import fr.elias.oessentials.economy.internal.cross.rabbit.PlayerJoinPacketHandler;
import fr.elias.oessentials.economy.internal.cross.rabbit.PlayerPresencePacketNamespace;
import fr.elias.oessentials.economy.internal.cross.rabbit.PlayerQuitPacket;
import fr.elias.oessentials.economy.internal.cross.rabbit.PlayerQuitPacketHandler;
import java.util.UUID;
import fr.elias.oessentials.economy.internal.persistence.database.PostgreSQLManager;
import fr.elias.oessentials.economy.internal.cross.PlayerDataListener;
import fr.elias.oessentials.economy.internal.cross.PlayerListener;
import fr.elias.oessentials.economy.internal.EconomyBootstrap;
import fr.elias.oessentials.economy.internal.ecocommands.ChequeCommand;
import fr.elias.oessentials.economy.internal.ecocommands.MoneyCommand;
import fr.elias.oessentials.economy.internal.ecocommands.PayCommand;
import fr.elias.oessentials.economy.internal.ecocommands.completion.ChequeTabCompleter;
import fr.elias.oessentials.economy.internal.ecocommands.completion.MoneyTabCompleter;
import fr.elias.oessentials.economy.internal.ecocommands.completion.PayTabCompleter;
import fr.elias.oessentials.economy.internal.offline.OfflinePlayerCache;
import fr.elias.oessentials.platform.scheduling.OreScheduler;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;

@PluginModule(value = "economy", dependencies = {"configuration::services", "messaging::services", "redis::services", "storage::services"})
public final class EconomyModule extends ManagedModule implements EconomyServices {
    private EconomyBootstrap ecoBootstrap;
    private PlayerEconomyDatabase database;
    private OfflinePlayerCache offlinePlayerCache;
    private Economy vaultEconomy;

    @Override
    protected void start() {
        cleanup("database", () -> { if (database != null) database.close(); });
        cleanup("ecoBootstrap", () -> { if (ecoBootstrap != null) ecoBootstrap.disable(); });
        // Other servers announce joins and quits so the offline player cache stays current.
        var messaging = services(MessagingServices.class);
        messaging.registerNamespace(new PlayerPresencePacketNamespace());
        messaging.subscribe(PlayerJoinPacket.class, new PlayerJoinPacketHandler(plugin));
        messaging.subscribe(PlayerQuitPacket.class, new PlayerQuitPacketHandler(plugin));
        initEconomy();
    }

    @Override public EconomyBootstrap getEcoBootstrap() { return ecoBootstrap; }
    @Override public PlayerEconomyDatabase getDatabase() { return database; }
    @Override public Economy getVaultEconomy() { return vaultEconomy; }

    private void initEconomy() {
        final String economyType = plugin.getConfig().getString("economy.type", "none").toLowerCase();

        if (services(ConfigurationServices.class).getEconomyEnabled()) {
            this.database = null;
            switch (economyType) {
                case "mongodb" -> {
                    MongoDBManager mgr = new MongoDBManager(plugin, services(RedisServices.class).getRedis());
                    boolean ok = mgr.connect(
                            plugin.getConfig().getString("economy.mongodb.uri"),
                            plugin.getConfig().getString("economy.mongodb.database"),
                            plugin.getConfig().getString("economy.mongodb.collection")
                    );
                    if (!ok) { plugin.getLogger().severe("[ECON] MongoDB connect failed. Disabling plugin."); throw new IllegalStateException("Economy storage could not initialize"); }
                    this.database = mgr;
                }
                case "postgresql" -> {
                    PostgreSQLManager mgr = new PostgreSQLManager(plugin, services(RedisServices.class).getRedis());
                    boolean ok = mgr.connect(
                            plugin.getConfig().getString("economy.postgresql.url"),
                            plugin.getConfig().getString("economy.postgresql.user"),
                            plugin.getConfig().getString("economy.postgresql.password")
                    );
                    if (!ok) { plugin.getLogger().severe("[ECON] PostgreSQL connect failed. Disabling plugin."); throw new IllegalStateException("Economy storage could not initialize"); }
                    this.database = mgr;
                }
                case "json" -> {
                    JsonEconomyDatabase mgr = new JsonEconomyDatabase(plugin, services(RedisServices.class).getRedis());
                    boolean ok = mgr.connect("", "", "");
                    if (!ok) { plugin.getLogger().severe("[ECON] JSON init failed. Disabling plugin."); throw new IllegalStateException("Economy storage could not initialize"); }
                    this.database = mgr;
                }
                case "none" -> this.database = null;
            }
        }

        if (services(ConfigurationServices.class).getEconomyEnabled()) {
            this.ecoBootstrap = new EconomyBootstrap(plugin);
            this.ecoBootstrap.enable();
        }

        if (services(ConfigurationServices.class).getEconomyEnabled() && this.database != null) {
            if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
                plugin.getLogger().warning("[ECON] Vault not found — economy module active but Vault API bridge is unavailable. " +
                        "Other plugins cannot use economy through Vault. Install Vault to enable the bridge.");
            } else {
                var rsp = plugin.getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
                if (rsp == null) { plugin.getLogger().warning("[ECON] Vault present but no Economy provider registered yet."); }
                else { this.vaultEconomy = rsp.getProvider(); plugin.getLogger().info("[ECON] Vault economy integration enabled."); }
            }

            Bukkit.getPluginManager().registerEvents(new PlayerDataListener(plugin), plugin);
            Bukkit.getPluginManager().registerEvents(new PlayerListener(plugin), plugin);

            this.offlinePlayerCache = new OfflinePlayerCache();
            this.database.populateCache(offlinePlayerCache);
            OreScheduler.runAsyncTimer(plugin, () -> this.database.populateCache(offlinePlayerCache), 20L * 60, 20L * 300);

            BootstrapSupport.unregisterCommandHard(plugin, "money");
            BootstrapSupport.unregisterCommandHard(plugin, "balance");
            BootstrapSupport.unregisterCommandHard(plugin, "bal");

            var moneyCmd  = new MoneyCommand(plugin);
            var payCmd    = new PayCommand();
            var chequeCmd = new ChequeCommand(plugin);
            services(ConfigurationServices.class).getCommands().register(moneyCmd).register(payCmd).register(chequeCmd);
            services(ConfigurationServices.class).getCommands().rewireTab("money",  new MoneyTabCompleter(plugin));
            services(ConfigurationServices.class).getCommands().rewireTab("pay",    new PayTabCompleter(plugin));
            services(ConfigurationServices.class).getCommands().rewireTab("cheque", new ChequeTabCompleter());
        } else if (services(ConfigurationServices.class).getEconomyEnabled()) {
            plugin.getLogger().warning("[ECON] Enabled but no database selected/connected; economy commands unavailable.");
        } else {
            plugin.getLogger().info("[ECON] Disabled. Skipping Vault, DB, and economy commands.");
            this.database = null;
            this.vaultEconomy = null;
        }

        if (this.database != null) {
            final PlayerEconomyDatabase importTarget = this.database;
            final StorageServices storage = services(StorageServices.class);
            storage.setBalanceImportTarget(new BalanceImportTarget() {
                @Override public double getBalance(UUID playerId) { return importTarget.getBalance(playerId); }
                @Override public void setBalance(UUID playerId, String playerName, double balance) { importTarget.setBalance(playerId, playerName, balance); }
            });
            cleanup("balanceImportTarget", () -> storage.setBalanceImportTarget(null));
        }
    }

    public OfflinePlayerCache getOfflinePlayerCache() {
        if (offlinePlayerCache == null) offlinePlayerCache = new OfflinePlayerCache();
        return offlinePlayerCache;
    }
}
