package fr.elias.oessentials.commands.lifecycle;

import dev.oreo.modulith.core.PluginModule;
import fr.elias.oessentials.platform.modularity.ManagedModule;
import fr.elias.oessentials.afk.lifecycle.AfkServices;
import fr.elias.oessentials.chat.lifecycle.ChatServices;
import fr.elias.oessentials.commandtoggle.lifecycle.CommandToggleServices;
import fr.elias.oessentials.configuration.lifecycle.ConfigurationServices;
import fr.elias.oessentials.enderchest.lifecycle.EnderChestServices;
import fr.elias.oessentials.ignore.lifecycle.IgnoreServices;
import fr.elias.oessentials.inventory.lifecycle.InventoriesServices;
import fr.elias.oessentials.mute.lifecycle.MuteSystemServices;
import fr.elias.oessentials.player.lifecycle.PlayerServices;
import fr.elias.oessentials.proxy.lifecycle.ProxyMessagingServices;
import fr.elias.oessentials.punishment.lifecycle.PunishmentLoggerServices;
import fr.elias.oessentials.storage.lifecycle.StorageServices;
import fr.elias.oessentials.warnings.lifecycle.WarningsServices;
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
import fr.elias.oessentials.commands.internal.commands.core.admins.MoveCommand;
import fr.elias.oessentials.punishment.internal.commands.core.moderation.BanCommand;
import fr.elias.oessentials.player.internal.commands.core.moderation.GodCommand;
import fr.elias.oessentials.commands.internal.commands.core.moderation.HealCommand;
import fr.elias.oessentials.commands.internal.commands.core.moderation.KickCommand;
import fr.elias.oessentials.punishment.internal.commands.core.moderation.UnbanCommand;
import fr.elias.oessentials.player.internal.commands.core.moderation.VanishCommand;
import fr.elias.oessentials.commands.internal.commands.core.playercommands.FeedCommand;
import fr.elias.oessentials.proxy.internal.commands.core.playercommands.ServerProxyCommand;
import fr.elias.oessentials.commands.internal.commands.core.playercommands.SitCommand;
import fr.elias.oessentials.commands.internal.listeners.SitListener;
import fr.elias.oessentials.afk.internal.AfkCommand;
import fr.elias.oessentials.commands.internal.anvil.AnvilCommand;
import fr.elias.oessentials.player.internal.back.command.BackCommand;
import fr.elias.oessentials.chat.internal.AfeliusReloadCommand;
import fr.elias.oessentials.chat.internal.BroadcastCommand;
import fr.elias.oessentials.chat.internal.MuteCommand;
import fr.elias.oessentials.chat.internal.UnmuteCommand;
import fr.elias.oessentials.player.internal.msg.MsgCommand;
import fr.elias.oessentials.player.internal.msg.ReplyCommand;
import fr.elias.oessentials.commandtoggle.internal.CommandToggleCommand;
import fr.elias.oessentials.commands.internal.cook.CookCommand;
import fr.elias.oessentials.player.internal.deathback.DeathBackCommand;
import fr.elias.oessentials.economy.internal.ecocommands.BalTopCommand;
import fr.elias.oessentials.economy.internal.ecocommands.BalanceCommand;
import fr.elias.oessentials.economy.internal.ecocommands.EcoMigrateCommand;
import fr.elias.oessentials.enderchest.internal.EcCommand;
import fr.elias.oessentials.enderchest.internal.EcSeeCommand;
import fr.elias.oessentials.commands.internal.furnace.FurnaceCommand;
import fr.elias.oessentials.sync.internal.invsee.command.InvseeCommand;
import fr.elias.oessentials.commands.internal.near.NearCommand;
import fr.elias.oessentials.chat.internal.nick.NickCommand;
import fr.elias.oessentials.chat.internal.nick.RealNameCommand;
import fr.elias.oessentials.commands.internal.ping.PingCommand;
import fr.elias.oessentials.inventory.internal.sellgui.command.SellGuiCommand;
import fr.elias.oessentials.skins.internal.skin.SkinCommand;
import fr.elias.oessentials.player.internal.tp.command.TpAcceptCommand;
import fr.elias.oessentials.player.internal.tp.command.TpCommand;
import fr.elias.oessentials.player.internal.tp.command.TpDenyCommand;
import fr.elias.oessentials.player.internal.tp.command.TpaCommand;
import fr.elias.oessentials.player.internal.tp.command.TphereCommand;
import fr.elias.oessentials.player.internal.tp.completer.TpTabCompleter;
import fr.elias.oessentials.player.internal.tp.completer.TpaTabCompleter;

@PluginModule(value = "commands", dependencies = {"afk::services", "chat::services", "command-toggle::services", "configuration::services", "ender-chest::services", "ignore::services", "inventories::services", "mute-system::services", "player-services::services", "proxy-messaging::services", "punishment-logger::services", "storage::services", "warnings::services"}, after = {"custom-worlds"})
public final class CommandsModule extends ManagedModule implements CommandsServices {
    private fr.elias.oessentials.commands.internal.ic.ICManager icManager;

    @Override
    protected void start() {
        initCommands();
    }

    @Override public fr.elias.oessentials.commands.internal.ic.ICManager getIcManager() { return icManager; }

    private void initCommands() {
        var muteCmd   = new MuteCommand(services(MuteSystemServices.class).getMuteService(), services(ChatServices.class).getChatSyncManager());
        var unmuteCmd = new UnmuteCommand(services(MuteSystemServices.class).getMuteService(), services(ChatServices.class).getChatSyncManager());
        var nickCmd   = new NickCommand();

        var tphere = new TphereCommand(plugin);
        services(ConfigurationServices.class).getCommands().register(tphere);
        services(ConfigurationServices.class).getCommands().rewireTab("tphere", tphere);
        services(ConfigurationServices.class).getCommands().register(nickCmd);

        final boolean homes = services(ConfigurationServices.class).getFeatureSettings().homesEnabled();
        services(ConfigurationServices.class).getCommands()
                .register(new BackCommand(services(PlayerServices.class).getBackService(), services(PlayerServices.class).getTeleportService(), plugin))
                .register(new TpaCommand(services(PlayerServices.class).getTeleportService()))
                .register(new TpAcceptCommand(services(PlayerServices.class).getTeleportService()))
                .register(new TpDenyCommand(services(PlayerServices.class).getTeleportService()))
                .register(new FlyCommand())
                .register(new HealCommand())
                .register(new FeedCommand())
                .register(buildMsgCommand(services(PlayerServices.class).getMessageService()))
                .register(new ReplyCommand(services(PlayerServices.class).getMessageService(), plugin))
                .register(new BroadcastCommand())
                .register(new DeathBackCommand(services(PlayerServices.class).getDeathBackService()))
                .register(new GodCommand(services(PlayerServices.class).getGodService()))
                .register(new AfeliusReloadCommand(plugin, services(ChatServices.class).getChatConfig()))
                .register(new VanishCommand(services(PlayerServices.class).getVanishService()))
                .register(new BanCommand())
                .register(new KickCommand())
                .register(new EnchantCommand())
                .register(new DisenchantCommand())
                .register(muteCmd)
                .register(new UnbanCommand())
                .register(unmuteCmd)
                .register(new OeCommand())
                .register(new ServerProxyCommand(services(ProxyMessagingServices.class).getProxyMessenger()))
                .register(new SkinCommand())
                .register(new CloneCommand())
                .register(new EcCommand(services(EnderChestServices.class).getEcService(), services(ConfigurationServices.class).getFeatureSettings().featureOption("cross-server", "enderchest", true)))
                .register(new HeadCommand())
                .register(new SellGuiCommand(plugin))
                .register(new AfkCommand(services(AfkServices.class).getAfkService()))
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
                .register(new RealNameCommand())
                .register(new FurnaceCommand(plugin))
                .register(new NearCommand(plugin))
                .register(new fr.elias.oessentials.commands.internal.commands.core.playercommands.PriceCommand(plugin))
                .register(new fr.elias.oessentials.commands.internal.commands.core.playercommands.RecipeCommand(plugin))
                .register(new fr.elias.oessentials.ignore.internal.IgnoreCommand(services(IgnoreServices.class).getIgnoreService()))
                .register(new fr.elias.oessentials.warnings.internal.WarnCommand(services(WarningsServices.class).getWarnService()))
                .register(new fr.elias.oessentials.warnings.internal.WarningsCommand(services(WarningsServices.class).getWarnService()))
                .register(new fr.elias.oessentials.warnings.internal.UnwarnCommand(services(WarningsServices.class).getWarnService()))
                .register(new fr.elias.oessentials.punishment.internal.HistoryCommand(services(PunishmentLoggerServices.class).getPunishmentLogger()))
                .register(new KillCommand())
                .register(new InvseeCommand())
                .register(new CookCommand())
                .register(new BalanceCommand(plugin))
                .register(new BalTopCommand(plugin))
                .register(new EcSeeCommand())
                .register(new fr.elias.oessentials.commands.internal.commands.core.admins.ReloadAllCommand())
                .registerIf(services(ConfigurationServices.class).getFeatureSettings().playerVaultsEnabled(), new fr.elias.oessentials.playervaults.internal.commands.core.playercommands.VaultsCommand())
                .register(new fr.elias.oessentials.commands.internal.commands.core.playercommands.UuidCommand())
                .register(new TpCommand(services(PlayerServices.class).getTeleportService()))
                .register(new MoveCommand(services(PlayerServices.class).getTeleportService()))
                .register(new EcoMigrateCommand(plugin))
                .register(new fr.elias.oessentials.currency.internal.commands.CurrencyAdminCommand(plugin));

        // Orders / Market command
        if (services(InventoriesServices.class).getOrdersModule() != null) {
            services(ConfigurationServices.class).getCommands().register(new fr.elias.oessentials.inventory.internal.orders.command.OrderCommand(services(InventoriesServices.class).getOrdersModule()));
        }

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

        services(ConfigurationServices.class).getCommands().register(new fr.elias.oessentials.worlds.internal.commands.core.admins.OeWorldCommand(plugin));

        initICModule();
        initMiscCommandsAndTabCompleters(muteCmd, unmuteCmd, nickCmd);
    }

    private MsgCommand buildMsgCommand(fr.elias.oessentials.player.internal.services.MessageService messageService) {
        MsgCommand cmd = new MsgCommand(messageService, plugin);
        if (services(IgnoreServices.class).getIgnoreService() != null) cmd.setIgnoreService(services(IgnoreServices.class).getIgnoreService());
        return cmd;
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
        services(ConfigurationServices.class).getCommands().registerLegacy("world", new fr.elias.oessentials.worlds.internal.commands.core.admins.WorldTeleportCommand());

        final var effectCmd = new fr.elias.oessentials.commands.internal.effects.EffectCommands();
        services(ConfigurationServices.class).getCommands().registerLegacy("effectme", effectCmd);
        services(ConfigurationServices.class).getCommands().registerLegacy("effectto", effectCmd);
    }

    private void initMiscCommandsAndTabCompleters(MuteCommand muteCmd, UnmuteCommand unmuteCmd, NickCommand nickCmd) {
        // Tab-completer overrides for OreoCommands already registered via commands.register().
        // rewireTab() looks up the command in the CommandMap (no plugin.yml needed).
        services(ConfigurationServices.class).getCommands().rewireTab("oeserver", new ServerProxyCommand(services(ProxyMessagingServices.class).getProxyMessenger()));
        services(ConfigurationServices.class).getCommands().rewireTab("kick",     new KickTabCompleter(plugin));
        services(ConfigurationServices.class).getCommands().rewireTab("clear",    new ClearTabCompleter(plugin));

        TpaTabCompleter tpaCompleter = new TpaTabCompleter(plugin);
        services(ConfigurationServices.class).getCommands().rewireTab("tpa",  tpaCompleter);
        services(ConfigurationServices.class).getCommands().rewireTab("move", tpaCompleter);
        services(ConfigurationServices.class).getCommands().rewireTab("tp",   new TpTabCompleter(plugin));

        services(ConfigurationServices.class).getCommands().rewireTab("balance", (sender, cmd, alias, args) -> {
            if (args.length == 1 && sender.hasPermission("oreo.balance.others")) {
                String partial = args[0].toLowerCase(java.util.Locale.ROOT);
                return org.bukkit.Bukkit.getOnlinePlayers().stream()
                        .map(org.bukkit.entity.Player::getName)
                        .filter(n -> n.toLowerCase(java.util.Locale.ROOT).startsWith(partial))
                        .sorted(String.CASE_INSENSITIVE_ORDER).toList();
            }
            return java.util.List.of();
        });

        // Commands that have no OreoCommand registration — use registerLegacy.
        var aliasCmd = new fr.elias.oessentials.commandcontrol.internal.aliases.AliasEditorCommand(services(ProxyMessagingServices.class).getAliasService(), services(InventoriesServices.class).getInvManager());
        services(ConfigurationServices.class).getCommands().registerLegacy("aliaseditor", aliasCmd, aliasCmd);

        if (services(CommandToggleServices.class).getCommandToggleConfig() != null && services(CommandToggleServices.class).getCommandToggleService() != null) {
            var cmdToggleCmd = new CommandToggleCommand(plugin, services(CommandToggleServices.class).getCommandToggleConfig(), services(CommandToggleServices.class).getCommandToggleService());
            services(ConfigurationServices.class).getCommands().registerLegacy("commandtoggle", cmdToggleCmd, cmdToggleCmd);
            plugin.getLogger().info("[CommandToggle] /commandtoggle command registered");
        }

        // Tab-completer overrides for remaining OreoCommands.
        services(ConfigurationServices.class).getCommands().rewireTab("skin",       new SkinCommand());
        services(ConfigurationServices.class).getCommands().rewireTab("clone",      new CloneCommand());
        services(ConfigurationServices.class).getCommands().rewireTab("head",       new HeadCommand());
        services(ConfigurationServices.class).getCommands().rewireTab("enchant",    new fr.elias.oessentials.commands.internal.commands.completion.EnchantTabCompleter());
        services(ConfigurationServices.class).getCommands().rewireTab("disenchant", new fr.elias.oessentials.commands.internal.commands.completion.EnchantTabCompleter());
        services(ConfigurationServices.class).getCommands().rewireTab("mute",   muteCmd);
        services(ConfigurationServices.class).getCommands().rewireTab("unmute", unmuteCmd);
        services(ConfigurationServices.class).getCommands().rewireTab("unban",  new UnbanCommand());
        services(ConfigurationServices.class).getCommands().rewireTab("nick",   nickCmd);
        services(ConfigurationServices.class).getCommands().rewireTab("invsee", new InvseeCommand());
        services(ConfigurationServices.class).getCommands().rewireTab("ecsee",  new EcSeeCommand());
    }
}
