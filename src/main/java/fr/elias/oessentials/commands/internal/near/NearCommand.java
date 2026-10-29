package fr.elias.oessentials.commands.internal.near;

import fr.elias.oessentials.OEssentials;
import fr.elias.oessentials.platform.commands.OreoCommand;
import fr.elias.oessentials.shared.Lang;
import fr.minuskube.inv.SmartInventory;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

public class NearCommand implements OreoCommand {

    private final OEssentials plugin;

    public NearCommand(OEssentials plugin) {
        this.plugin = plugin;
    }

    @Override public String name() { return "near"; }
    @Override public List<String> aliases() { return List.of(); }
    @Override public String permission() { return "oreo.near"; }
    @Override public String usage() { return "[radius]"; }
    @Override public boolean playerOnly() { return true; }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player p)) return true;

        int radius = 100;
        if (args.length >= 1) {
            try {
                radius = Math.max(1, Math.min(1000, Integer.parseInt(args[0])));
            } catch (NumberFormatException e) {
                Lang.send(p, "near.radius-not-number",
                        "<red>Radius must be a number. You entered: <yellow>%input%</yellow></red>",
                        Map.of("input", args[0]));
                return true;
            }
        }

        int finalRadius = radius;
        if (fr.elias.oessentials.platform.ui.Dialogs.show(plugin, "near", "Near dialog", () -> NearDialogHandler.open(plugin, p, finalRadius))) {
            return true;
        }
        SmartInventory.builder()
                .manager(plugin.getInvManager())
                .provider(new NearGuiProvider(plugin, finalRadius))
                .title("§6Nearby Players §7(radius: " + finalRadius + ")")
                .size(6, 9)
                .build()
                .open(p);
        return true;
    }
}