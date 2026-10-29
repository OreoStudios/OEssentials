package fr.elias.oessentials.commands.internal.commands.core.moderation;

import fr.elias.oessentials.OEssentials;
import fr.elias.oessentials.platform.commands.OreoCommand;
import fr.elias.oessentials.shared.Lang;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class KickCommand implements OreoCommand {

    @Override public String name() { return "kick"; }
    @Override public List<String> aliases() { return List.of(); }
    @Override public String permission() { return "oreo.kick"; }
    @Override public String usage() { return "<player> [reason...]"; }
    @Override public boolean playerOnly() { return false; }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (args.length < 1) {
            Lang.send(sender, "moderation.kick.usage",
                    "<yellow>Usage: /%label% <player> [reason...]</yellow>",
                    Map.of("label", label));
            return true;
        }

        String arg = args[0];
        String reason = args.length >= 2
                ? String.join(" ", Arrays.copyOfRange(args, 1, args.length))
                : Lang.msgWithDefault(
                "moderation.kick.default-reason",
                "Kicked by an operator.",
                sender instanceof Player ? (Player) sender : null
        );

        OEssentials plugin = OEssentials.get();
        var dir = plugin.getPlayerLocator();
        var bridge = plugin.getModuleRegistry().read(fr.elias.oessentials.modgui.lifecycle.ModGuiServices.class, fr.elias.oessentials.modgui.lifecycle.ModGuiServices::getCrossServerApi);

        Player local = Bukkit.getPlayerExact(arg);
        if (local != null && local.isOnline()) {
            String kickMsg = Lang.msgWithDefault(
                    "moderation.kick.kick-message",
                    "<red>%reason%</red>",
                    Map.of("reason", reason),
                    local
            );
            local.kickPlayer(kickMsg);

            Lang.send(sender, "moderation.kick.success",
                    "<green>Kicked <aqua>%player%</aqua>. Reason: <yellow>%reason%</yellow></green>",
                    Map.of("player", local.getName(), "reason", reason));

            notifyDiscord(local.getName(), local.getUniqueId(), reason, sender.getName());
            return true;
        }

        UUID uuid = null;

        try {
            if (dir != null) uuid = dir.lookupUuidByName(arg);
        } catch (Throwable ignored) { }

        if (uuid == null) {
            try {
                uuid = UUID.fromString(arg);
            } catch (Throwable ignored) { }
        }

        if (uuid == null) {
            Lang.send(sender, "moderation.kick.player-not-found",
                    "<red>Player not found.</red>");
            return true;
        }

        String targetName = arg;
        try {
            if (dir != null) {
                String n = dir.lookupNameByUuid(uuid);
                if (n != null && !n.isBlank()) targetName = n;
            }
        } catch (Throwable ignored) { }

        if (bridge != null) {
            bridge.kick(uuid, targetName, reason);

            Lang.send(sender, "moderation.kick.success-cross-server",
                    "<green>Kick request sent for <aqua>%player%</aqua> (cross-server).</green>",
                    Map.of("player", targetName));

            notifyDiscord(targetName, uuid, reason, sender.getName());
            return true;
        }

        Lang.send(sender, "moderation.kick.no-bridge",
                "<red>Target is not on this server and cross-server mod bridge is unavailable.</red>");
        return true;
    }

    private void notifyDiscord(String name, UUID uuid, String reason, String actor) {
        fr.elias.oessentials.discord.api.ModerationNotifier mod = OEssentials.get().getModuleRegistry().read(fr.elias.oessentials.discord.lifecycle.DiscordIntegrationServices.class, fr.elias.oessentials.discord.lifecycle.DiscordIntegrationServices::getModerationNotifier);
        if (mod != null && mod.isEnabled()) {
            mod.notifyKick(name, uuid, reason, actor);
        }
    }
}