package fr.elias.oessentials.chat.internal;

import fr.elias.oessentials.OEssentials;
import fr.elias.oessentials.platform.commands.OreoCommand;
import fr.elias.oessentials.chat.internal.chatservices.MuteService;
import fr.elias.oessentials.shared.Lang;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

import java.util.*;

public class UnmuteCommand implements OreoCommand, org.bukkit.command.TabCompleter {

    private final MuteService mutes;
    private final ChatSyncManager chatSync;

    public UnmuteCommand(MuteService mutes, ChatSyncManager chatSync) {
        this.mutes = mutes;
        this.chatSync = chatSync;
    }

    @Override public String name() { return "unmute"; }
    @Override public List<String> aliases() { return List.of(); }
    @Override public String permission() { return "oreo.unmute"; }
    @Override public String usage() { return "<player>"; }
    @Override public boolean playerOnly() { return false; }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (args.length < 1) {
            Lang.send(sender, "moderation.unmute.usage",
                    "<yellow>Usage: /%label% <player></yellow>",
                    Map.of("label", label));
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (target == null || (target.getName() == null && !target.hasPlayedBefore())) {
            Lang.send(sender, "moderation.unmute.player-not-found",
                    "<red>Player not found: %player%</red>",
                    Map.of("player", args[0]));
            return true;
        }

        boolean wasMuted = mutes.unmute(target.getUniqueId());
        if (!wasMuted) {
            Lang.send(sender, "moderation.unmute.not-muted",
                    "<gray>%player% is not muted.</gray>",
                    Map.of("player", target.getName()));
            return true;
        }

        Lang.send(sender, "moderation.unmute.success",
                "<green>Unmuted <aqua>%player%</aqua>.</green>",
                Map.of("player", target.getName()));

        // Tell other servers
        try {
            if (chatSync != null) {
                chatSync.broadcastUnmute(target.getUniqueId());
            }
        } catch (Throwable t) {
            Bukkit.getLogger().warning("[OEssentials] Failed to broadcast UNMUTE: " + t.getMessage());
        }

        // Discord notification
        fr.elias.oessentials.discord.api.ModerationNotifier mod = OEssentials.get().getModuleRegistry().read(fr.elias.oessentials.discord.lifecycle.DiscordIntegrationServices.class, fr.elias.oessentials.discord.lifecycle.DiscordIntegrationServices::getModerationNotifier);
        if (mod != null && mod.isEnabled()) {
            mod.notifyUnmute(target.getName(), target.getUniqueId(), sender.getName());
        }

        // Notify player if online
        var p = target.getPlayer();
        if (p != null && p.isOnline()) {
            Lang.send(p, "moderation.unmute.notified",
                    "<green>You have been unmuted.</green>");
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender,
                                      org.bukkit.command.Command command,
                                      String label,
                                      String[] args) {

        if (!sender.hasPermission(permission()))
            return Collections.emptyList();

        if (args.length == 1) {
            String want = args[0].toLowerCase(Locale.ROOT);
            Set<String> out = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);


            for (UUID uuid : mutes.allMuted()) {
                var op = Bukkit.getOfflinePlayer(uuid);
                String name = (op != null && op.getName() != null) ? op.getName() : uuid.toString();

                if (name.toLowerCase(Locale.ROOT).startsWith(want)) {
                    out.add(name);
                }
            }

            var dir = OEssentials.get().getPlayerLocator();
            if (dir != null) {
                try {
                    Collection<String> names = dir.suggestOnlineNames(want, 50);
                    if (names != null) {
                        for (String n : names) {
                            if (n != null && n.toLowerCase(Locale.ROOT).startsWith(want)) {
                                out.add(n);
                            }
                        }
                    }
                } catch (Throwable ignored) {}
            }

            return out.stream().limit(50).toList();
        }

        return Collections.emptyList();
    }
}