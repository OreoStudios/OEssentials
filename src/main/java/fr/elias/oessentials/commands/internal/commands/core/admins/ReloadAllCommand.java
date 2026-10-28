package fr.elias.oessentials.commands.internal.commands.core.admins;

import fr.elias.oessentials.OEssentials;
import fr.elias.oessentials.configuration.lifecycle.ConfigurationServices;
import fr.elias.oessentials.platform.commands.OreoCommand;
import fr.elias.oessentials.shared.Lang;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Map;

public final class ReloadAllCommand implements OreoCommand {

    @Override public String name() { return "oereload"; }
    @Override public List<String> aliases() { return List.of("oereloadall", "oer"); }
    @Override public String permission() { return "oreo.reload"; }
    @Override public String usage() { return ""; }
    @Override public boolean playerOnly() { return false; }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        final OEssentials plugin = OEssentials.get();
        final long start = System.currentTimeMillis();

        int ok = 0, skip = 0;

        // 0) settings.yml
        try {
            plugin.getModuleRegistry().require(ConfigurationServices.class).reloadSettings();

            Lang.send(sender, "admin.reload.settings",
                    "<green>✔ Reloaded <white>settings.yml</white> <gray>(+ cross-server settings)</gray></green>");
            ok++;
        } catch (Throwable t) {
            Lang.send(sender, "admin.reload.settings-skip",
                    "<yellow>• Skipped settings.yml / cross-server: <gray>%error%</gray></yellow>",
                    Map.of("error", t.getMessage()));
            skip++;
        }
        try {
            var ar = plugin.getModuleRegistry().read(ConfigurationServices.class, ConfigurationServices::getAutoReboot);
            if (ar != null) {
                ar.reload();
                Lang.send(sender, "admin.reload.autoreboot",
                        "<green>✔ Reloaded <white>auto-reboot</white> <gray>(settings.yml)</gray></green>");
                ok++;
            } else {
                Lang.send(sender, "admin.reload.autoreboot-skip",
                        "<yellow>• Skipped auto-reboot (service unavailable)</yellow>");
                skip++;
            }
        } catch (Throwable t) {
            Lang.send(sender, "admin.reload.autoreboot-fail",
                    "<red>✘ Failed reloading auto-reboot: <gray>%error%</gray></red>",
                    Map.of("error", t.getMessage()));
        }

        // 1) Main config.yml
        try {
            plugin.reloadConfig();
            Lang.send(sender, "admin.reload.config",
                    "<green>✔ Reloaded <white>config.yml</white></green>");
            ok++;
        } catch (Throwable t) {
            Lang.send(sender, "admin.reload.config-fail",
                    "<red>✘ Failed reloading config.yml: <gray>%error%</gray></red>",
                    Map.of("error", t.getMessage()));
        }

        // 2) lang.yml
        try {
            Lang.init(plugin);
            Lang.send(sender, "admin.reload.lang",
                    "<green>✔ Reloaded <white>lang.yml</white></green>");
            ok++;
        } catch (Throwable t) {
            Lang.send(sender, "admin.reload.lang-skip",
                    "<yellow>• Skipped lang.yml: <gray>%error%</gray></yellow>",
                    Map.of("error", t.getMessage()));
            skip++;
        }

        // 3-15) Module files: each module registers its own reload step.
        var steps = plugin.getModuleRegistry().read(ConfigurationServices.class, ConfigurationServices::getReloadSteps);
        if (steps != null) {
            int[] counts = steps.runAll(sender);
            ok += counts[0];
            skip += counts[1];
        }

        try {
            for (var p : Bukkit.getOnlinePlayers()) {
                try {
                    p.updateCommands(); // Paper: refresh brigadier + command suggestions
                } catch (Throwable ignored) {}
            }
            Lang.send(sender, "admin.reload.commands-refresh",
                    "<green>✔ Refreshed <white>command suggestions</white> <gray>(updateCommands)</gray></green>");
        } catch (Throwable ignored) {}

        // Summary
        long took = System.currentTimeMillis() - start;
        Lang.send(sender, "admin.reload.summary",
                "<gray>Reload complete: <green>%ok% OK</green>, <yellow>%skip% skipped</yellow>. (<white>%ms% ms</white>)</gray>",
                Map.of("ok", String.valueOf(ok), "skip", String.valueOf(skip), "ms", String.valueOf(took)));
        Lang.send(sender, "admin.reload.note",
                "<dark_gray>(Some modules don't support hot reload yet; a server restart may be required.)</dark_gray>");

        return true;
    }
}
