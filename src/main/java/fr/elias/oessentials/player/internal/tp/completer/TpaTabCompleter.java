package fr.elias.oessentials.player.internal.tp.completer;

import fr.elias.oessentials.OEssentials;
import fr.elias.oessentials.storage.api.PlayerLocator;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.TabCompleter;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

public class TpaTabCompleter implements TabCompleter {
    private final OEssentials plugin;

    public TpaTabCompleter(OEssentials plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        String name = cmd.getName().toLowerCase(Locale.ROOT);

        // ONLY handle /tpa, not /tp
        if (!name.equals("tpa")) return Collections.emptyList();

        if (args.length != 1) return Collections.emptyList();

        final String want = args[0].toLowerCase(Locale.ROOT);
        Set<String> out = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

        for (Player p : Bukkit.getOnlinePlayers()) {
            String n = p.getName();
            if (n != null && n.toLowerCase(Locale.ROOT).startsWith(want)) {
                out.add(n);
            }
        }

        PlayerLocator dir = plugin.getPlayerLocator();
        if (dir != null) {
            try {
                var names = dir.suggestOnlineNames(want, 50);
                if (names != null) {
                    out.addAll(names);
                }
            } catch (Throwable ignored) {}
        }

        return out.stream().limit(50).toList();
    }
}
