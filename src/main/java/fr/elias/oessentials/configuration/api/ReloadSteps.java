package fr.elias.oessentials.configuration.api;

import dev.oreo.modulith.core.ModuleApi;
import org.bukkit.command.CommandSender;

/**
 * The steps of {@code /oereload}. Each module registers how to reload its own files and reports
 * the outcome to the sender, so the command does not reach into other modules.
 */
@ModuleApi("services")
public interface ReloadSteps {
    /** How a step counts towards the "x OK, y skipped" summary. */
    @ModuleApi("services")
    enum Outcome { RELOADED, SKIPPED, FAILED }

    @ModuleApi("services")
    @FunctionalInterface
    interface Step {
        Outcome reload(CommandSender sender);
    }

    /** Steps run by ascending {@code order}; a name can be registered once. */
    void register(int order, String name, Step step);

    void unregister(String name);

    /** Runs every registered step and returns {@code {reloaded, skipped}}. */
    int[] runAll(CommandSender sender);
}
