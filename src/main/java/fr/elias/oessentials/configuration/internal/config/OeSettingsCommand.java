package fr.elias.oessentials.configuration.internal.config;

import fr.elias.oessentials.OEssentials;
import fr.elias.oessentials.platform.commands.OreoCommand;
import fr.elias.oessentials.configuration.api.BossBarToggles;
import java.util.function.Supplier;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class OeSettingsCommand implements OreoCommand {

    private final OEssentials plugin;
    private final Supplier<BossBarToggles> bossBars;

    public OeSettingsCommand(OEssentials plugin, Supplier<BossBarToggles> bossBars) {
        this.plugin = plugin;
        this.bossBars = bossBars;
    }

    @Override
    public String name() {
        return "oesettings";
    }

    @Override
    public List<String> aliases() {
        return List.of("oeconfig", "oecfg");
    }

    @Override
    public String permission() {
        return "oreo.admin.settings";
    }

    @Override
    public String usage() {
        return "";
    }

    @Override
    public boolean playerOnly() {
        return true;
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be used by players.");
            return true;
        }

        if (!player.hasPermission("oreo.admin.settings")) {
            player.sendMessage("§cYou don't have permission to use this command.");
            return true;
        }

        OeSettingsMenu.getInventory(plugin, bossBars).open(player);
        return true;
    }
}