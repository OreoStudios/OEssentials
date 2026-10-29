package fr.elias.oessentials.storage.internal.homes.home;

import fr.elias.oessentials.OEssentials;
import fr.elias.oessentials.platform.commands.OreoCommand;
import fr.elias.oessentials.configuration.api.ServerConfig;
import fr.elias.oessentials.platform.scheduling.Async;
import fr.elias.oessentials.shared.Lang;
import fr.elias.oessentials.platform.scheduling.OreScheduler;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SetHomeCommand implements OreoCommand {
    private final HomeService homes;
    private final ServerConfig config;

    public SetHomeCommand(HomeService homes, ServerConfig config) {
        this.homes = homes;
        this.config = config;
    }

    @Override public String name() { return "sethome"; }
    @Override public List<String> aliases() { return List.of(); }
    @Override public String permission() { return "oreo.sethome"; }
    @Override public String usage() { return "<name>"; }
    @Override public boolean playerOnly() { return true; }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (args.length < 1) return false;

        Player p = (Player) sender;
        String rawName = args[0];
        String key = rawName.toLowerCase(Locale.ROOT);
        Location loc = p.getLocation();

        if (loc.getY() < loc.getWorld().getMinHeight()) {
            Lang.send(p, "sethome.unsafe-void",
                    "<red>You cannot set a home in the void.</red>");
            return true;
        }
        org.bukkit.block.Block feet = loc.getBlock();
        org.bukkit.block.Block head = feet.getRelative(BlockFace.UP);
        if (feet.getType().isSolid() || head.getType().isSolid()) {
            Lang.send(p, "sethome.unsafe-block",
                    "<red>You cannot set a home inside a block.</red>");
            return true;
        }

        // Capture max homes on entity thread before going async.
        int max = config.getMaxHomesFor(p);

        Async.run(() -> {
            boolean ok = homes.setHome(p, rawName, loc);
            OEssentials plugin = OEssentials.get();
            OreScheduler.runForEntity(plugin, p, () -> {
                if (!ok) {
                    Lang.send(p, "sethome.limit",
                            "<red>You've reached your home limit of <yellow>%max%</yellow>.</red>",
                            Map.of("max", String.valueOf(max)));
                } else {
                    Lang.send(p, "sethome.set",
                            "<green>Home <yellow>%name%</yellow> has been set.</green>",
                            Map.of("name", key));
                }
            });
        });

        return true;
    }
}