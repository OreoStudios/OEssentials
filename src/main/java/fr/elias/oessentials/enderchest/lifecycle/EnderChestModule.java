package fr.elias.oessentials.enderchest.lifecycle;

import dev.oreo.modulith.core.PluginModule;
import fr.elias.oessentials.platform.modularity.ManagedModule;
import fr.elias.oessentials.configuration.lifecycle.ConfigurationServices;
import fr.elias.oessentials.storage.lifecycle.StorageServices;
import org.bukkit.Bukkit;

@PluginModule(value = "ender-chest", dependencies = {"configuration::services", "storage::services"}, after = {"chat"})
public final class EnderChestModule extends ManagedModule implements EnderChestServices {
    private fr.elias.oessentials.enderchest.internal.EnderChestConfig ecConfig;
    private fr.elias.oessentials.enderchest.internal.EnderChestService ecService;

    @Override
    protected void start() {
        initEnderChest();
        registerCommands();
    }

    @Override public fr.elias.oessentials.enderchest.internal.EnderChestConfig getEcConfig() { return ecConfig; }
    @Override public fr.elias.oessentials.enderchest.internal.EnderChestService getEcService() { return ecService; }

    private void initEnderChest() {
        this.ecConfig = new fr.elias.oessentials.enderchest.internal.EnderChestConfig(plugin);
        final boolean crossServerEc = services(ConfigurationServices.class).getFeatureSettings().featureOption("cross-server", "enderchest", true);
        final boolean mongoStorage  = "mongodb".equalsIgnoreCase(plugin.getConfig().getString("essentials.storage", "yaml"));

        fr.elias.oessentials.enderchest.internal.EnderChestStorage ecStorage;
        if (mongoStorage && crossServerEc && services(StorageServices.class).getHomesMongoClient() != null) {
            String dbName = plugin.getConfig().getString("storage.mongo.database", "oreo");
            String prefix = plugin.getConfig().getString("storage.mongo.collectionPrefix", "oreo_");
            ecStorage = new fr.elias.oessentials.enderchest.internal.MongoEnderChestStorage(services(StorageServices.class).getHomesMongoClient(), dbName, prefix, plugin.getLogger());
            plugin.getLogger().info("[EC] Using MongoDB cross-server ender chest storage.");
        } else {
            ecStorage = new fr.elias.oessentials.enderchest.internal.YamlEnderChestStorage(plugin);
            plugin.getLogger().info("[EC] Using local YAML ender chest storage.");
        }

        this.ecService = new fr.elias.oessentials.enderchest.internal.EnderChestService(plugin, this.ecConfig, ecStorage);
        Bukkit.getServicesManager().register(fr.elias.oessentials.enderchest.internal.EnderChestService.class, this.ecService, plugin, org.bukkit.plugin.ServicePriority.Normal);
        plugin.getServer().getPluginManager().registerEvents(new fr.elias.oessentials.enderchest.internal.EnderChestListener(plugin, ecService, crossServerEc), plugin);
    }

    /** Commands this module owns (registered by the commands module until 7.1.6). */
    private void registerCommands() {
        var commands = services(fr.elias.oessentials.configuration.lifecycle.ConfigurationServices.class).getCommands();
        commands.register(new fr.elias.oessentials.enderchest.internal.EcCommand(ecService, services(fr.elias.oessentials.configuration.lifecycle.ConfigurationServices.class).getFeatureSettings().featureOption("cross-server", "enderchest", true)))
                .register(new fr.elias.oessentials.enderchest.internal.EcSeeCommand());
        commands.rewireTab("ecsee", new fr.elias.oessentials.enderchest.internal.EcSeeCommand());
    }
}
