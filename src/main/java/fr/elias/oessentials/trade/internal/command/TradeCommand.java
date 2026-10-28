package fr.elias.oessentials.trade.internal.command;

import fr.elias.oessentials.OEssentials;
import fr.elias.oessentials.trade.internal.service.TradeService;
import fr.elias.oessentials.shared.Lang;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class TradeCommand implements TabExecutor {

    private final OEssentials plugin;
    private final TradeService service;
    /** Trades with players on other servers; yields null while that is unavailable. */
    private final java.util.function.Supplier<fr.elias.oessentials.trade.internal.cross.CrossTradeService> crossTrades;

    public TradeCommand(OEssentials plugin, TradeService service,
                        java.util.function.Supplier<fr.elias.oessentials.trade.internal.cross.CrossTradeService> crossTrades) {
        this.plugin = plugin;
        this.service = service;
        this.crossTrades = crossTrades;
    }


    private boolean dbg() {
        try {
            return service != null && service.getConfig() != null && service.getConfig().debugDeep;
        } catch (Throwable t) {
            return false;
        }
    }

    private void log(String msg) {
        if (dbg()) plugin.getLogger().info(msg);
    }


    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String lbl, String[] args) {
        if (!(sender instanceof Player p)) {
            Lang.send(sender, "trade.player-only",
                    "<red>Players only.</red>");
            return true;
        }

        if (plugin.getTradeService() == null) {
            Lang.send(p, "trade.disabled",
                    "<red>Trading is currently disabled.</red>");
            log("[TRADE] /trade denied: service null");
            return true;
        }

        if (!p.hasPermission("oreo.trade")) {
            Lang.send(p, "trade.no-permission",
                    "<red>You don't have permission.</red>");
            log("[TRADE] /trade denied: no permission for " + p.getName());
            return true;
        }

        if (args.length != 1) {
            Lang.send(p, "trade.usage",
                    "<yellow>Usage: /trade <player></yellow>");
            log("[TRADE] /trade bad usage by " + p.getName());
            return true;
        }

        final String targetName = args[0];
        log("[TRADE] /trade invoked by=" + p.getName() + " arg=" + targetName);

        boolean accepted = service.tryAcceptInvite(p, targetName);
        if (!accepted && service.tryAcceptInviteAny(p)) return true;

        log("[TRADE] tryAcceptInvite result=" + accepted + " acceptor=" + p.getName() + " requesterName=" + targetName);
        if (accepted) return true;

        Player localTarget = Bukkit.getPlayerExact(targetName);
        log("[TRADE] resolve local target -> " + (localTarget != null ? ("FOUND uuid=" + localTarget.getUniqueId()) : "not found"));

        if (localTarget != null && localTarget.getUniqueId().equals(p.getUniqueId())) {
            Lang.send(p, "trade.self",
                    "<red>You cannot trade with yourself.</red>");
            log("[TRADE] /trade self-invite blocked for " + p.getName());
            return true;
        }

        if (localTarget != null && localTarget.isOnline()) {
            log("[TRADE] sending LOCAL invite: " + p.getName() + " -> " + localTarget.getName());
            service.sendInvite(p, localTarget);
            return true;
        }

        var cross = crossTrades.get();
        log("[TRADE] cross-server path: available=" + (cross != null));
        if (cross != null && cross.handleCommand(p, targetName)) {
            return true;
        }

        Lang.send(p, "trade.not-found-cross-server",
                "<red>Player not found here.</red> <gray>If this is a cross-server trade, type <aqua>/trade %player%</aqua> on the server where you received the invite.</gray>",
                Map.of("player", targetName));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) return List.of();
        if (!(sender instanceof Player player)) return List.of();
        if (!player.hasPermission("oreo.trade")) return List.of();

        String prefix = args[0].toLowerCase(Locale.ROOT);
        List<String> matches = new ArrayList<>();

        // Keep tab completion fast and Folia-safe: use Bukkit's already-loaded local
        // player list only. Do not perform synchronous MongoDB queries from tab completion.
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getUniqueId().equals(player.getUniqueId())) continue;

            String name = online.getName();
            if (name != null && name.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                matches.add(name);
            }
        }

        matches.sort(String.CASE_INSENSITIVE_ORDER);
        return matches;
    }
}
