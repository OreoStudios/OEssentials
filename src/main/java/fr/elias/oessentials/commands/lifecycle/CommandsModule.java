package fr.elias.oessentials.commands.lifecycle;

import dev.oreo.modulith.core.PluginModule;
import fr.elias.oessentials.platform.modularity.ManagedModule;
import fr.elias.oessentials.configuration.lifecycle.ConfigurationServices;
import fr.elias.oessentials.inventory.lifecycle.InventoriesServices;
import fr.elias.oessentials.proxy.lifecycle.ProxyMessagingServices;
import fr.elias.oessentials.commands.internal.commands.OeCommand;
import fr.elias.oessentials.commands.internal.commands.completion.ClearTabCompleter;
import fr.elias.oessentials.commands.internal.commands.completion.KickTabCompleter;
import fr.elias.oessentials.commands.internal.commands.core.admins.ClearCommand;
import fr.elias.oessentials.commands.internal.commands.core.admins.CloneCommand;
import fr.elias.oessentials.commands.internal.commands.core.admins.DisenchantCommand;
import fr.elias.oessentials.commands.internal.commands.core.admins.EnchantCommand;
import fr.elias.oessentials.commands.internal.commands.core.admins.FlyCommand;
import fr.elias.oessentials.commands.internal.commands.core.admins.HeadCommand;
import fr.elias.oessentials.commands.internal.commands.core.admins.KillCommand;
import fr.elias.oessentials.commands.internal.commands.core.moderation.HealCommand;
import fr.elias.oessentials.commands.internal.commands.core.moderation.KickCommand;
import fr.elias.oessentials.commands.internal.commands.core.playercommands.FeedCommand;
import fr.elias.oessentials.commands.internal.commands.core.playercommands.SitCommand;
import fr.elias.oessentials.commands.internal.listeners.SitListener;
import fr.elias.oessentials.commands.internal.anvil.AnvilCommand;
import fr.elias.oessentials.commands.internal.cook.CookCommand;
import fr.elias.oessentials.commands.internal.furnace.FurnaceCommand;
import fr.elias.oessentials.commands.internal.near.NearCommand;
import fr.elias.oessentials.commands.internal.ping.PingCommand;

@PluginModule(value = "commands", dependencies = {"afk::services", "chat::services", "command-toggle::services", "configuration::services", "ender-chest::services", "ignore::services", "inventories::services", "mute-system::services", "player-services::services", "proxy-messaging::services", "punishment-logger::services", "storage::services", "warnings::services"}, after = {"custom-worlds"})
public final class CommandsModule extends ManagedModule implements CommandsServices {
    private fr.elias.oessentials.commands.internal.ic.ICManager icManager;

    @Override
    protected void start() {
        initCommands();
    }

    @Override public fr.elias.oessentials.commands.internal.ic.ICManager getIcManager() { return icManager; }

    private void initCommands() {


        final boolean homes = services(ConfigurationServices.class).getFeatureSettings().homesEnabled();
        services(ConfigurationServices.class).getCommands()
                .register(new FlyCommand())
                .register(new HealCommand())
                .register(new FeedCommand())
                .register(new KickCommand())
                .register(new EnchantCommand())
                .register(new DisenchantCommand())
                .register(new OeCommand())
                .register(new CloneCommand())
                .register(new HeadCommand())
                .register(new fr.elias.oessentials.commands.internal.commands.core.playercommands.TrashCommand())
                .register(new fr.elias.oessentials.commands.internal.commands.core.playercommands.WorkbenchCommand())
                .register(new AnvilCommand())
                .register(new ClearCommand())
                .register(new fr.elias.oessentials.commands.internal.commands.core.playercommands.SeenCommand())
                .register(new PingCommand())
                .register(new fr.elias.oessentials.commands.internal.commands.core.playercommands.HatCommand())
                .register(new fr.elias.oessentials.commands.internal.commands.core.playercommands.TopCommand())
                .register(new fr.elias.oessentials.commands.internal.commands.core.playercommands.BottomCommand())
                .register(new fr.elias.oessentials.commands.internal.commands.core.admins.DayCommand())
                .register(new fr.elias.oessentials.commands.internal.commands.core.admins.NightCommand())
                .register(new FurnaceCommand(plugin))
                .register(new NearCommand(plugin))
                .register(new fr.elias.oessentials.commands.internal.commands.core.playercommands.PriceCommand(plugin))
                .register(new fr.elias.oessentials.commands.internal.commands.core.playercommands.RecipeCommand(plugin))
                .register(new KillCommand())
                .register(new CookCommand())
                .register(new fr.elias.oessentials.commands.internal.commands.core.admins.ReloadAllCommand())
                .register(new fr.elias.oessentials.commands.internal.commands.core.playercommands.UuidCommand());


                plugin.getServer().getPluginManager().registerEvents(new fr.elias.oessentials.commands.internal.furnace.VirtualFurnaceListener(), plugin);

        if (services(ConfigurationServices.class).getFeatureSettings().sitEnabled()) {
            services(ConfigurationServices.class).getCommands().register(new SitCommand());
            plugin.getServer().getPluginManager().registerEvents(new SitListener(), plugin);
            plugin.getLogger().info("[Sit] Enabled.");
        } else {
            plugin.getLogger().info("[Sit] Disabled by settings.yml.");
        }
        var visitorService = new fr.elias.oessentials.guards.internal.services.VisitorService();
        plugin.getServer().getPluginManager().registerEvents(new fr.elias.oessentials.guards.internal.listeners.VisitorGuardListener(visitorService), plugin);

        var gmCmd = new fr.elias.oessentials.commands.internal.commands.core.admins.GamemodeCommand(visitorService);
        plugin.getCommands().register(gmCmd);
        services(ConfigurationServices.class).getCommands().rewireTab("gamemode", gmCmd);


        initICModule();
        initMiscCommandsAndTabCompleters();
    }

    private void initICModule() {
        this.icManager = new fr.elias.oessentials.commands.internal.ic.ICManager(plugin.getDataFolder());
        services(ConfigurationServices.class).getCommands().registerLegacy("ic", new fr.elias.oessentials.commands.internal.ic.ICCommand(icManager));
        plugin.getServer().getPluginManager().registerEvents(new fr.elias.oessentials.commands.internal.ic.ICListener(icManager, plugin), plugin);

        services(ConfigurationServices.class).getCommands().registerLegacy("oetime", new fr.elias.oessentials.commands.internal.commands.core.admins.OeTimeCommand());

        final var weatherCmd = new fr.elias.oessentials.commands.internal.commands.core.admins.WeatherCommand();
        for (String alias : new String[]{"weather", "sun", "rain", "storm"}) {
            services(ConfigurationServices.class).getCommands().registerLegacy(alias, weatherCmd);
        }

        services(ConfigurationServices.class).getCommands().registerLegacy("flyspeed", new fr.elias.oessentials.commands.internal.commands.core.admins.FlySpeedCommand());
        services(ConfigurationServices.class).getCommands().registerLegacy("walkspeed", new fr.elias.oessentials.commands.internal.commands.core.admins.WalkSpeedCommand());

        final var effectCmd = new fr.elias.oessentials.commands.internal.effects.EffectCommands();
        services(ConfigurationServices.class).getCommands().registerLegacy("effectme", effectCmd);
        services(ConfigurationServices.class).getCommands().registerLegacy("effectto", effectCmd);
    }

    private void initMiscCommandsAndTabCompleters() {
        // Tab-completer overrides for OreoCommands already registered via commands.register().
        // rewireTab() looks up the command in the CommandMap (no plugin.yml needed).
        services(ConfigurationServices.class).getCommands().rewireTab("kick",     new KickTabCompleter(plugin));
        services(ConfigurationServices.class).getCommands().rewireTab("clear",    new ClearTabCompleter(plugin));



        // Commands that have no OreoCommand registration — use registerLegacy.
        var aliasCmd = new fr.elias.oessentials.commandcontrol.internal.aliases.AliasEditorCommand(services(ProxyMessagingServices.class).getAliasService(), services(InventoriesServices.class).getInvManager());
        services(ConfigurationServices.class).getCommands().registerLegacy("aliaseditor", aliasCmd, aliasCmd);


        // Tab-completer overrides for remaining OreoCommands.
        services(ConfigurationServices.class).getCommands().rewireTab("clone",      new CloneCommand());
        services(ConfigurationServices.class).getCommands().rewireTab("head",       new HeadCommand());
        services(ConfigurationServices.class).getCommands().rewireTab("enchant",    new fr.elias.oessentials.commands.internal.commands.completion.EnchantTabCompleter());
        services(ConfigurationServices.class).getCommands().rewireTab("disenchant", new fr.elias.oessentials.commands.internal.commands.completion.EnchantTabCompleter());
    }
}
