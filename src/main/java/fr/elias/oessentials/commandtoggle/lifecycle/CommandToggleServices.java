package fr.elias.oessentials.commandtoggle.lifecycle;

import dev.oreo.modulith.core.ModuleApi;
import fr.elias.oessentials.commandtoggle.internal.CommandToggleConfig;
import fr.elias.oessentials.commandtoggle.internal.CommandToggleService;

@ModuleApi("services")
public interface CommandToggleServices {
    CommandToggleConfig getCommandToggleConfig();
    CommandToggleService getCommandToggleService();

    /** The public OEssentials API for this module, or {@code null} while it is not available. */
    default fr.elias.oessentials.api.ICommandToggleAPI getCommandToggleApi() {
        var s = getCommandToggleService();
        return s == null ? null : new fr.elias.oessentials.commandtoggle.internal.api.CommandToggleAPIImpl(s);
    }

    default boolean isCommandEnabled(String commandNameOrAlias) {
        var config = getCommandToggleConfig();
        return config == null || config.isCommandEnabled(commandNameOrAlias);
    }

    /** Runs {@code onToggle} when the command is switched in commands-toggle.yml; false when toggles are unavailable. */
    default boolean onCommandToggled(String commandName, Runnable onToggle) {
        var config = getCommandToggleConfig();
        if (config == null) return false;
        config.registerModuleCallback(commandName, onToggle);
        return true;
    }

    default void applyToggles() {
        var service = getCommandToggleService();
        if (service != null) service.applyToggles();
    }
}
