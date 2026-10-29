package fr.elias.oessentials.punishment.lifecycle;

import dev.oreo.modulith.core.PluginModule;
import fr.elias.oessentials.platform.modularity.ManagedModule;

@PluginModule(value = "punishment-logger", dependencies = {"configuration::services"}, uses = {"discord-integration::services"}, after = {"warnings"})
public final class PunishmentLoggerModule extends ManagedModule implements PunishmentLoggerServices {
    private fr.elias.oessentials.punishment.internal.PunishmentLogger punishmentLogger;

    @Override
    protected void start() {
        initPunishmentLogger();
        registerCommands();
        providePublicApi(fr.elias.oessentials.api.IPunishmentAPI.class, this::getPunishmentApi);
    }

    @Override public fr.elias.oessentials.punishment.internal.PunishmentLogger getPunishmentLogger() { return punishmentLogger; }

    private void initPunishmentLogger() {
        try {
            this.punishmentLogger = new fr.elias.oessentials.punishment.internal.PunishmentLogger(plugin);
            plugin.getLogger().info("[PunishmentLogger] Initialized.");
        } catch (Throwable t) {
            plugin.getLogger().severe("[PunishmentLogger] Failed to initialize: " + t.getMessage());
        }
    }

    /** Commands this module owns (registered by the commands module until 7.1.6). */
    private void registerCommands() {
        var commands = services(fr.elias.oessentials.configuration.lifecycle.ConfigurationServices.class).getCommands();
        commands.register(new fr.elias.oessentials.punishment.internal.commands.core.moderation.BanCommand())
                .register(new fr.elias.oessentials.punishment.internal.commands.core.moderation.UnbanCommand())
                .register(new fr.elias.oessentials.punishment.internal.HistoryCommand(punishmentLogger));
        commands.rewireTab("unban", new fr.elias.oessentials.punishment.internal.commands.core.moderation.UnbanCommand());
    }
}
