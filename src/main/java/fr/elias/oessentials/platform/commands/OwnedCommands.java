package fr.elias.oessentials.platform.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Tracks actual command objects so cleanup never removes another plugin's replacement. */
final class OwnedCommands {
    private final Set<Command> commands = Collections.newSetFromMap(new IdentityHashMap<>());

    void add(Command command) { commands.add(command); }

    void removeFrom(CommandMap map) {
        remove(new ArrayList<>(commands), map);
    }

    /** Unregisters some owned commands; entries now pointing at another plugin's command are kept. */
    void remove(Collection<Command> subset, CommandMap map) {
        Set<Command> targets = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Command command : subset) {
            if (commands.contains(command)) targets.add(command);
        }
        Map<String, Command> known = map.getKnownCommands();
        // Some server forks (e.g. Purpur 1.21.8) expose a knownCommands view whose
        // iterator does not support remove(), so entrySet().removeIf() throws
        // UnsupportedOperationException — fall back to removing by key, fail-soft.
        try {
            known.entrySet().removeIf(entry -> targets.contains(entry.getValue()));
        } catch (UnsupportedOperationException iteratorImmutable) {
            List<String> keys = new ArrayList<>();
            for (Map.Entry<String, Command> entry : known.entrySet()) {
                if (targets.contains(entry.getValue())) keys.add(entry.getKey());
            }
            for (String key : keys) {
                try {
                    known.remove(key);
                } catch (UnsupportedOperationException fullyImmutable) {
                    break;
                }
            }
        }
        targets.forEach(command -> command.unregister(map));
        commands.removeAll(targets);
    }
}
