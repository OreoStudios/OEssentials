package fr.elias.oessentials.platform;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Prints the OEssentials startup banner: a big ASCII logo with the live plugin
 * version, a loading subtitle, a checklist of every loaded module, and a checklist
 * of every optional integration the plugin can hook into.
 */
public final class StartupBanner {

    private StartupBanner() {}

    /** Minimal 5-row block font for the letters used in "OEssentials". */
    private static final Map<Character, String[]> FONT = new LinkedHashMap<>();
    static {
        FONT.put('O', new String[]{" ### ", "#   #", "#   #", "#   #", " ### "});
        FONT.put('E', new String[]{"#####", "#    ", "#### ", "#    ", "#####"});
        FONT.put('S', new String[]{" ####", "#    ", " ### ", "    #", "#### "});
        FONT.put('N', new String[]{"#   #", "##  #", "# # #", "#  ##", "#   #"});
        FONT.put('T', new String[]{"#####", "  #  ", "  #  ", "  #  ", "  #  "});
        FONT.put('I', new String[]{"###", " # ", " # ", " # ", "###"});
        FONT.put('A', new String[]{" ### ", "#   #", "#####", "#   #", "#   #"});
        FONT.put('L', new String[]{"#    ", "#    ", "#    ", "#    ", "#####"});
        FONT.put(' ', new String[]{"  ", "  ", "  ", "  ", "  "});
    }

    private static final String OK = "✔";  // ✔
    private static final String NO = "✖";  // ✖

    /** Bundled (shaded) core libraries: display name -> a class that must be present at runtime. */
    private static final Map<String, String> CORE_LIBRARIES = new LinkedHashMap<>();
    static {
        CORE_LIBRARIES.put("MinecraftModulith", "fr.elias.oessentials.libs.modulith.paper.PaperModulith");
        CORE_LIBRARIES.put("CommandsAPI", "fr.elias.oessentials.libs.commands.spigot.CommandManager");
        CORE_LIBRARIES.put("Libby", "fr.elias.oessentials.libs.libby.BukkitLibraryManager");
        CORE_LIBRARIES.put("bStats", "fr.elias.oessentials.libs.bstats.bukkit.Metrics");
        CORE_LIBRARIES.put("ClassGraph", "fr.elias.oessentials.libs.classgraph.ClassGraph");
        CORE_LIBRARIES.put("Gson", "com.google.gson.Gson");
        CORE_LIBRARIES.put("SmartInvs", "fr.minuskube.inv.SmartInvsPlugin");
        CORE_LIBRARIES.put("Triumph-GUI", "dev.triumphteam.gui.guis.Gui");
    }

    /** Optional plugins OEssentials integrates with: display name -> Bukkit plugin name. */
    private static final Map<String, String> INTEGRATIONS = new LinkedHashMap<>();
    static {
        INTEGRATIONS.put("Vault", "Vault");
        INTEGRATIONS.put("PlaceholderAPI", "PlaceholderAPI");
        INTEGRATIONS.put("PacketEvents", "packetevents");
        INTEGRATIONS.put("WorldGuard", "WorldGuard");
        INTEGRATIONS.put("LuckPerms", "LuckPerms");
        INTEGRATIONS.put("FastAsyncWorldEdit", "FastAsyncWorldEdit");
        INTEGRATIONS.put("MythicMobs", "MythicMobs");
        INTEGRATIONS.put("ItemsAdder", "ItemsAdder");
        INTEGRATIONS.put("Nexo", "Nexo");
        INTEGRATIONS.put("FancyNpcs", "FancyNpcs");
        INTEGRATIONS.put("Floodgate", "floodgate");
    }

    private static List<String> renderLogo(String text) {
        String[] rows = {"", "", "", "", ""};
        for (char c : text.toUpperCase(Locale.ROOT).toCharArray()) {
            String[] glyph = FONT.getOrDefault(c, FONT.get(' '));
            for (int i = 0; i < rows.length; i++) rows[i] += glyph[i] + " ";
        }
        return List.of(rows);
    }

    public static void print(JavaPlugin plugin, List<String> moduleIds, java.util.Set<String> unavailable) {
        ConsoleCommandSender console = Bukkit.getConsoleSender();
        String version = plugin.getDescription().getVersion();

        send(console, "");
        for (String line : renderLogo("OEssentials")) send(console, ChatColor.AQUA + line);
        send(console, ChatColor.DARK_AQUA + "  v" + version
                + ChatColor.DARK_GRAY + "  -  " + ChatColor.GRAY + "loading your core");
        send(console, "");

        // ---- Modules checklist -------------------------------------------------
        send(console, ChatColor.DARK_GRAY + ">> " + ChatColor.WHITE + "Modules "
                + ChatColor.GRAY + "(" + (moduleIds.size() - unavailable.size()) + " loaded"
                + (unavailable.isEmpty() ? "" : ", " + ChatColor.RED + unavailable.size() + " unavailable"
                        + ChatColor.GRAY) + ")");
        List<String> sorted = new ArrayList<>(moduleIds);
        sorted.sort(String::compareTo);
        int columns = 3, cellWidth = 22;
        for (int i = 0; i < sorted.size(); i += columns) {
            StringBuilder row = new StringBuilder("   ");
            for (int j = i; j < Math.min(i + columns, sorted.size()); j++) {
                boolean down = unavailable.contains(sorted.get(j));
                row.append(down ? ChatColor.RED + NO : ChatColor.GREEN + OK).append(' ')
                   .append(down ? ChatColor.RED : ChatColor.GRAY).append(pad(sorted.get(j), cellWidth));
            }
            send(console, row.toString());
        }
        send(console, "");

        // ---- Core libraries checklist -----------------------------------------
        send(console, ChatColor.DARK_GRAY + ">> " + ChatColor.WHITE + "Core libraries "
                + ChatColor.GRAY + "(bundled)");
        for (Map.Entry<String, String> e : CORE_LIBRARIES.entrySet()) {
            boolean present = classPresent(e.getValue());
            String mark = present ? ChatColor.GREEN + OK : ChatColor.RED + NO;
            String name = (present ? ChatColor.WHITE : ChatColor.RED) + e.getKey();
            send(console, "   " + mark + " " + name);
        }
        send(console, "");

        // ---- Integrations checklist -------------------------------------------
        send(console, ChatColor.DARK_GRAY + ">> " + ChatColor.WHITE + "Integrations "
                + ChatColor.GRAY + "(optional)");
        for (Map.Entry<String, String> e : INTEGRATIONS.entrySet()) {
            boolean present = Bukkit.getPluginManager().getPlugin(e.getValue()) != null;
            String mark = present ? ChatColor.GREEN + OK : ChatColor.DARK_GRAY + NO;
            String name = (present ? ChatColor.WHITE : ChatColor.DARK_GRAY) + e.getKey();
            String note = present ? "" : ChatColor.DARK_GRAY + " (not installed)";
            send(console, "   " + mark + " " + name + note);
        }
        send(console, "");
        send(console, ChatColor.AQUA + "OEssentials " + ChatColor.DARK_GRAY + "v" + version
                + ChatColor.GREEN + "  ready." + ChatColor.RESET);
        send(console, "");
    }

    private static String pad(String s, int width) {
        if (s.length() >= width) return s.substring(0, width - 1) + " ";
        StringBuilder b = new StringBuilder(s);
        while (b.length() < width) b.append(' ');
        return b.toString();
    }

    private static boolean classPresent(String className) {
        try {
            Class.forName(className, false, StartupBanner.class.getClassLoader());
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private static void send(ConsoleCommandSender console, String message) {
        console.sendMessage(message);
    }
}
