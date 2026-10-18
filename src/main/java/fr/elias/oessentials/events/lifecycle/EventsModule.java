package fr.elias.oessentials.events.lifecycle;

import dev.oreo.modulith.core.PluginModule;
import fr.elias.oessentials.platform.modularity.ManagedModule;
import fr.elias.oessentials.configuration.lifecycle.ConfigurationServices;

@PluginModule(value = "events", dependencies = {"configuration::services"}, after = {"web-panel"})
public final class EventsModule extends ManagedModule implements EventsServices {
    private fr.elias.oessentials.events.internal.EventConfig eventConfig;
    private fr.elias.oessentials.events.internal.DeathMessageService deathMessages;

    @Override
    protected void start() {
        initEvents();
    }

    @Override public fr.elias.oessentials.events.internal.EventConfig getEventConfig() { return eventConfig; }
    @Override public fr.elias.oessentials.events.internal.DeathMessageService getDeathMessages() { return deathMessages; }

    private void initEvents() {
        this.eventConfig   = new fr.elias.oessentials.events.internal.EventConfig(plugin.getDataFolder());
        this.deathMessages = new fr.elias.oessentials.events.internal.DeathMessageService(plugin.getDataFolder());

        var eventEngine = new fr.elias.oessentials.events.internal.EventEngine(eventConfig, deathMessages);
        plugin.getServer().getPluginManager().registerEvents(eventEngine, plugin);

        var eventCmd = new fr.elias.oessentials.events.internal.EventCommands(eventConfig, deathMessages);
        services(ConfigurationServices.class).getCommands().registerLegacy("oevents", eventCmd);
    }
}
