package fr.elias.oessentials.warnings.lifecycle;

import dev.oreo.modulith.core.PluginModule;
import fr.elias.oessentials.platform.modularity.ManagedModule;

@PluginModule(value = "warnings", dependencies = {"configuration::services", "storage::services"}, after = {"ignore"})
public final class WarningsModule extends ManagedModule implements WarningsServices {
    private fr.elias.oessentials.warnings.internal.WarnService warnService;

    @Override
    protected void start() {
        initWarnings();
        registerCommands();
        providePublicApi(fr.elias.oessentials.api.IWarningsAPI.class, this::getWarningsApi);
    }

    @Override public fr.elias.oessentials.warnings.internal.WarnService getWarnService() { return warnService; }

    private void initWarnings() {
        try {
            this.warnService = new fr.elias.oessentials.warnings.internal.WarnService(plugin, sharedCollection());
            plugin.getLogger().info("[Warnings] Initialized" + (sharedCollection() != null ? " (shared across servers via MongoDB)." : "."));
        } catch (Throwable t) {
            plugin.getLogger().severe("[Warnings] Failed to initialize: " + t.getMessage());
        }
    }

    /** With essentials.storage: mongodb, warnings live in <prefix>warnings so every server shares them. */
    private com.mongodb.client.MongoCollection<org.bson.Document> sharedCollection() {
        var client = services(fr.elias.oessentials.storage.lifecycle.StorageServices.class).getHomesMongoClient();
        if (client == null) return null;
        var cfg = plugin.getConfig();
        return client.getDatabase(cfg.getString("storage.mongo.database", "oreo"))
                .getCollection(cfg.getString("storage.mongo.collectionPrefix", "oreo_") + "warnings");
    }

    /** Commands this module owns (registered by the commands module until 7.1.6). */
    private void registerCommands() {
        var commands = services(fr.elias.oessentials.configuration.lifecycle.ConfigurationServices.class).getCommands();
        commands.register(new fr.elias.oessentials.warnings.internal.WarnCommand(warnService))
                .register(new fr.elias.oessentials.warnings.internal.WarningsCommand(warnService))
                .register(new fr.elias.oessentials.warnings.internal.UnwarnCommand(warnService));
    }
}
