// File: src/main/java/fr/elias/oessentials/commands/core/admins/SetSpawnCommand.java
package fr.elias.oessentials.storage.internal.spawn;

import fr.elias.oessentials.OEssentials;
import fr.elias.oessentials.platform.commands.OreoCommand;
import fr.elias.oessentials.platform.scheduling.Async;
import fr.elias.oessentials.shared.Lang;
import fr.elias.oessentials.platform.scheduling.OreScheduler;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

public class SetSpawnCommand implements OreoCommand {
    private final SpawnService spawn;

    public SetSpawnCommand(SpawnService spawn) {
        this.spawn = spawn;
    }

    @Override public String name() { return "setspawn"; }
    @Override public List<String> aliases() { return List.of(); }
    @Override public String permission() { return "oreo.setspawn"; }
    @Override public String usage() { return ""; }
    @Override public boolean playerOnly() { return true; }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        Player p = (Player) sender;
        OEssentials plugin = OEssentials.get();
        String local = plugin.getServerConfig().serverName();
        org.bukkit.Location loc = p.getLocation();

        Async.run(() -> {
            spawn.setLocalSpawn(loc);
            SpawnDirectory spawnDir = plugin.getSpawnDirectory();
            if (spawnDir != null) spawnDir.setSpawnServer(local);

            OreScheduler.runForEntity(plugin, p, () -> {
                Lang.send(p, "admin.setspawn.set", "<green>Spawn set.</green>");
                if (spawnDir != null) {
                    Lang.send(p, "admin.setspawn.cross-server-info",
                            "<gray>(Cross-server) Spawn owner set to <aqua>%server%</aqua>.</gray>",
                            Map.of("server", local));
                }
            });
        });

        return true;
    }

}
