package fr.elias.oessentials.discord.lifecycle;

import dev.oreo.modulith.core.PluginModule;
import fr.elias.oessentials.platform.modularity.ManagedModule;
import fr.elias.oessentials.configuration.lifecycle.ConfigurationServices;
import fr.elias.oessentials.economy.lifecycle.EconomyServices;
import fr.elias.oessentials.storage.lifecycle.StorageServices;

@PluginModule(value = "discord-integration", dependencies = {"configuration::services", "economy::services", "storage::services"}, after = {"mute-system"}, optional = true)
public final class DiscordIntegrationModule extends ManagedModule implements DiscordIntegrationServices {
    private fr.elias.oessentials.discord.internal.DiscordModerationNotifier discordMod;
    private fr.elias.oessentials.discord.internal.discordbot.DiscordLinkManager discordLinkManager;

    @Override
    protected void start() {
        initDiscordIntegration();
        registerReloadStep(10, "discord", this::reloadDiscord);
    }

    @Override public fr.elias.oessentials.discord.internal.DiscordModerationNotifier getDiscordMod() { return discordMod; }
    @Override public fr.elias.oessentials.discord.internal.discordbot.DiscordLinkManager getDiscordLinkManager() { return discordLinkManager; }

    private void initDiscordIntegration() {
        if (services(ConfigurationServices.class).getFeatureSettings().discordModerationEnabled()) {
            this.discordMod = new fr.elias.oessentials.discord.internal.DiscordModerationNotifier(plugin);
            plugin.getLogger().info("[DiscordMod] Discord moderation integration enabled.");
        } else {
            this.discordMod = null;
            plugin.getLogger().info("[DiscordMod] Disabled by settings.yml.");
        }

        String linkBackend = plugin.getConfig().getString("discord.link_backend", "file").toLowerCase();

        if ("mongodb".equals(linkBackend) && services(StorageServices.class).getHomesMongoClient() != null) {
            String dbName = plugin.getConfig().getString("storage.mongo.database", "oreo");
            com.mongodb.client.MongoDatabase linkDb = services(StorageServices.class).getHomesMongoClient().getDatabase(dbName);
            this.discordLinkManager = new fr.elias.oessentials.discord.internal.discordbot.DiscordLinkManager(linkDb, plugin.getLogger());
            plugin.getLogger().info("[DiscordLink] Account linking: mongodb backend.");
        } else {
            this.discordLinkManager = new fr.elias.oessentials.discord.internal.discordbot.DiscordLinkManager(plugin.getDataFolder(), plugin.getLogger());
            if ("mongodb".equals(linkBackend)) {
                plugin.getLogger().warning("[DiscordLink] link_backend=mongodb but MongoDB is not connected; falling back to file.");
            } else {
                plugin.getLogger().info("[DiscordLink] Account linking: file backend.");
            }
        }

        var linkCmd = new fr.elias.oessentials.discord.internal.discordbot.DiscordLinkCommand(discordLinkManager);
        services(ConfigurationServices.class).getCommands().registerLegacy("discord", linkCmd, linkCmd);
        services(ConfigurationServices.class).getCommands().registerLegacy("oe-discord",
                new fr.elias.oessentials.discord.internal.discordbot.DiscordRconExtension(services(StorageServices.class).getHomes(), services(EconomyServices.class).getDatabase()));
    }

    /** /oereload step (moved from ReloadAllCommand). */
    private fr.elias.oessentials.configuration.api.ReloadSteps.Outcome reloadDiscord(org.bukkit.command.CommandSender sender) {
        // The Discord moderation notifier has no reload hook.
        fr.elias.oessentials.shared.Lang.send(sender, "admin.reload.discord-skip",
                "<yellow>• Skipped discord integration (no reload hook)</yellow>");
        return fr.elias.oessentials.configuration.api.ReloadSteps.Outcome.SKIPPED;
    }
}
