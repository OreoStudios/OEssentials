package fr.elias.oessentials.economy.internal.ecocommands.completion;

import fr.elias.oessentials.OEssentials;
import fr.elias.oessentials.storage.api.PlayerLocator;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

public class MoneyTabCompleter implements TabCompleter {

    private final OEssentials plugin;

    public MoneyTabCompleter(OEssentials plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {


        if (args.length == 1) {
            List<String> completions = new ArrayList<>();

            if (sender.hasPermission("OEssentials.money.give")) {
                completions.add("give");
            }
            if (sender.hasPermission("OEssentials.money.take")) {
                completions.add("take");
            }
            if (sender.hasPermission("OEssentials.money.set")) {
                completions.add("set");
            }

            String prefix = args[0].toLowerCase(Locale.ROOT);
            return completions.stream()
                    .filter(s -> s.toLowerCase(Locale.ROOT).startsWith(prefix))
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .toList();
        }

        if (args.length == 2) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            Set<String> names = new HashSet<>();

            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName() != null) {
                    names.add(p.getName());
                }
            }

            PlayerLocator dir = plugin.getPlayerLocator(); // adjust getter name if needed
            if (dir != null) {
                names.addAll(dir.suggestOnlineNames(prefix, 80));
            }

            return names.stream()
                    .filter(n -> n.toLowerCase(Locale.ROOT).startsWith(prefix))
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .toList();
        }

        if (args.length == 3) {
            List<String> amounts = List.of("100", "500", "1000", "5000", "10000");
            String prefix = args[2];
            return amounts.stream()
                    .filter(a -> a.startsWith(prefix))
                    .toList();
        }

        return List.of();
    }
}
