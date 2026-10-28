package fr.elias.oessentials.chat.internal;

import fr.elias.oessentials.platform.config.CustomConfig;

import fr.elias.oessentials.OEssentials;
import fr.elias.oessentials.platform.commands.OreoCommand;
import fr.elias.oessentials.shared.Lang;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Map;

public class AfeliusReloadCommand implements OreoCommand {
    private final OEssentials plugin;
    private final CustomConfig chatCfg;

    public AfeliusReloadCommand(OEssentials plugin, CustomConfig chatCfg) {
        this.plugin = plugin;
        this.chatCfg = chatCfg;
    }

    @Override public String name() { return "afelius"; }
    @Override public List<String> aliases() { return List.of("af"); }
    @Override public String permission() { return "oreo.afelius.reload"; }
    @Override public String usage() { return "reload <all|config|chat-format>"; }
    @Override public boolean playerOnly() { return false; }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (args.length < 1 || !args[0].equalsIgnoreCase("reload")) {
            Lang.send(sender, "admin.afelius.usage", null, Map.of("label", label));
            return true;
        }

        String target = (args.length >= 2) ? args[1].toLowerCase() : "all";
        switch (target) {
            case "all" -> {
                plugin.reloadConfig();
                chatCfg.reloadCustomConfig();
                Lang.send(sender, "admin.afelius.reloaded-all", null, null);
            }
            case "config" -> {
                plugin.reloadConfig();
                Lang.send(sender, "admin.afelius.reloaded-config", null, null);
            }
            case "chat-format" -> {
                chatCfg.reloadCustomConfig();
                Lang.send(sender, "admin.afelius.reloaded-chat", null, null);
            }
            default -> Lang.send(sender, "admin.afelius.unknown-section", null,
                    Map.of("sections", "all, config, chat-format"));
        }
        return true;
    }
}
