package fr.elias.oessentials.commandtoggle.internal;

import fr.elias.oessentials.platform.commands.CommandManager;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.FormattedCommandAlias;
import org.bukkit.command.PluginIdentifiableCommand;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

class CommandToggleServiceTest {
    @TempDir
    Path dataFolder;

    private JavaPlugin plugin;
    private Map<String, Command> known;
    private MockedStatic<Bukkit> bukkit;

    @BeforeEach
    void setUp() {
        plugin = mock(JavaPlugin.class);
        when(plugin.getName()).thenReturn("OEssentials");
        when(plugin.getLogger()).thenReturn(Logger.getLogger("CommandToggleServiceTest"));
        when(plugin.getDataFolder()).thenReturn(dataFolder.toFile());
        when(plugin.isEnabled()).thenReturn(true);

        Server server = mock(Server.class);
        known = new HashMap<>();
        SimpleCommandMap map = new SimpleCommandMap(server, known);
        when(server.getCommandMap()).thenReturn(map);

        bukkit = mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getServer).thenReturn(server);
        bukkit.when(Bukkit::getCommandMap).thenReturn(map);
    }

    @AfterEach
    void tearDown() {
        bukkit.close();
    }

    @Test
    void disablingAnOverrideHandsTheLabelBackToTheDisplacedCommand() throws IOException {
        // State after CommandsAPI's override: vanilla renamed to "minecraft:tp", Oreo owns "tp".
        Command vanilla = new FormattedCommandAlias("tp", new String[0]);
        vanilla.setLabel("minecraft:tp");
        known.put("minecraft:tp", vanilla);
        Command oreo = new OreoCommand("tp");
        known.put("tp", oreo);
        known.put("oessentials:tp", oreo);
        known.put("teleport", oreo);

        CommandToggleService service = service("tp", false);
        service.applyToggles();

        assertSame(vanilla, known.get("tp"), "vanilla /tp must answer again");
        assertEquals("tp", vanilla.getLabel());
        assertNull(known.get("oessentials:tp"), "no Oreo binding may remain");
        assertNull(known.get("teleport"));

        writeConfig("tp", true);
        service.reload();

        assertSame(oreo, known.get("tp"));
        assertSame(oreo, known.get("oessentials:tp"));
        assertEquals("minecraft:tp", vanilla.getLabel(), "vanilla is displaced again like an override does");
    }

    @Test
    void disablingACommandNobodyElseOwnsRemovesTheLabel() throws IOException {
        Command oreo = new OreoCommand("spawn");
        known.put("spawn", oreo);
        known.put("oessentials:spawn", oreo);

        service("spawn", false).applyToggles();

        assertNull(known.get("spawn"));
        assertNull(known.get("oessentials:spawn"));
    }

    @Test
    void enablingACommandThatWasNeverRegisteredRegistersItThroughTheGate() throws IOException {
        CommandManager commands = mock(CommandManager.class);
        when(commands.registerDeferred("spawn")).thenReturn(true);
        writeConfig("spawn", true);
        CommandToggleService service = new CommandToggleService(
                plugin, new CommandToggleConfig(plugin), commands);

        service.applyToggles();

        verify(commands, atLeastOnce()).registerDeferred("spawn");
    }

    private CommandToggleService service(String command, boolean enabled) throws IOException {
        writeConfig(command, enabled);
        return new CommandToggleService(plugin, new CommandToggleConfig(plugin));
    }

    private void writeConfig(String command, boolean enabled) throws IOException {
        File file = dataFolder.resolve("config/commandsmodule/commands-toggle.yml").toFile();
        file.getParentFile().mkdirs();
        Files.writeString(file.toPath(), "commands:\n  " + command + ":\n    enabled: " + enabled + "\n");
    }

    /** Stands in for a CommandsAPI PluginCommand owned by OEssentials. */
    private final class OreoCommand extends Command implements PluginIdentifiableCommand {
        OreoCommand(String name) {
            super(name);
        }

        @Override
        public boolean execute(CommandSender sender, String label, String[] args) {
            return true;
        }

        @Override
        public Plugin getPlugin() {
            return plugin;
        }
    }
}
