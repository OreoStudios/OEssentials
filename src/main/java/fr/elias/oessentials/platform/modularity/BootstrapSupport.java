package fr.elias.oessentials.platform.modularity;

import fr.elias.oessentials.OEssentials;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;

public final class BootstrapSupport {
    private BootstrapSupport() {}

    @SuppressWarnings("unchecked")
    private static boolean isOreoCommand(OEssentials plugin, org.bukkit.command.Command command) {
        if (command instanceof org.bukkit.command.PluginIdentifiableCommand owned) {
            try {
                return owned.getPlugin() == plugin;
            } catch (Throwable ignored) {
                return false;
            }
        }
        return command != null && command.getClass().getName().startsWith("fr.elias.oessentials.");
    }

    public static void unregisterCommandHard(OEssentials plugin, String label) {
        try {
            Object craftServer = plugin.getServer();
            org.bukkit.command.CommandMap commandMap = null;
            try {
                var m = craftServer.getClass().getMethod("getCommandMap");
                Object res = m.invoke(craftServer);
                if (res instanceof org.bukkit.command.CommandMap cm) commandMap = cm;
            } catch (Throwable ignored) {}
            if (commandMap == null) {
                var f = craftServer.getClass().getDeclaredField("commandMap");
                f.setAccessible(true);
                Object res = f.get(craftServer);
                if (res instanceof org.bukkit.command.CommandMap cm) commandMap = cm;
            }
            if (!(commandMap instanceof org.bukkit.command.SimpleCommandMap map)) return;
            var f2 = org.bukkit.command.SimpleCommandMap.class.getDeclaredField("knownCommands");
            f2.setAccessible(true);
            Map<String, org.bukkit.command.Command> known = (Map<String, Command>) f2.get(map);
            String lower = label.toLowerCase(java.util.Locale.ROOT);
            // Only Oreo's own commands: vanilla /bossbar, /scoreboard or another plugin's
            // /kit, /hologram, /rtp... share these labels and must survive a disabled feature.
            java.util.List<String> keys = new java.util.ArrayList<>();
            for (Map.Entry<String, org.bukkit.command.Command> e : new java.util.ArrayList<>(known.entrySet())) {
                String k = e.getKey().toLowerCase(java.util.Locale.ROOT);
                if ((k.equals(lower) || k.endsWith(":" + lower)) && isOreoCommand(plugin, e.getValue())) {
                    keys.add(e.getKey());
                }
            }
            keys.forEach(known::remove);
        } catch (Throwable ignored) {}
    }

    public static boolean uiModuleAllowed(OEssentials plugin, String moduleKey) {
        String server = plugin.getConfigService() != null ? plugin.getConfigService().serverName() : Bukkit.getServer().getName();
        boolean disabled = plugin.getSettingsConfig() != null && plugin.getSettingsConfig().uiModuleDisabledForServer(server, moduleKey);
        if (disabled) {
            plugin.getLogger().info("[UI Safeguard] Disabled " + moduleKey + " on server " + server + ".");
        }
        return !disabled;
    }
}
