package fr.elias.oessentials.platform.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OwnedCommandsTest {
    @Test
    void removesOwnedAliasesButPreservesAnotherPluginsReplacement() {
        Command own = mock(Command.class);
        Command other = mock(Command.class);
        CommandMap map = mock(CommandMap.class);
        var entries = new HashMap<String, Command>();
        entries.put("home", other);
        entries.put("oessentials:home", own);
        entries.put("homes-alias", own);
        entries.put("other:home", other);
        when(map.getKnownCommands()).thenReturn(entries);
        var owned = new OwnedCommands();
        owned.add(own);
        owned.removeFrom(map);
        assertEquals(2, entries.size());
        assertSame(other, entries.get("home"));
        verify(own).unregister(map);
        verify(other, never()).unregister(map);
        owned.removeFrom(map);
        verify(own, times(1)).unregister(map);
    }

    /** Purpur/Paper 1.21.8 exposes a knownCommands view whose iterator rejects remove(). */
    @Test
    void fallsBackToKeyRemovalWhenEntrySetIteratorIsImmutable() {
        Command own = mock(Command.class);
        Command other = mock(Command.class);
        CommandMap map = mock(CommandMap.class);
        var backing = new HashMap<String, Command>();
        backing.put("home", other);
        backing.put("oessentials:home", own);
        backing.put("homes-alias", own);
        // A map that supports remove(key) but whose entrySet().iterator().remove() throws,
        // reproducing the UnsupportedOperationException seen on some server forks.
        Map<String, Command> immutableIterator = new HashMap<>(backing) {
            @Override public Set<Map.Entry<String, Command>> entrySet() {
                return java.util.Collections.unmodifiableSet(super.entrySet());
            }
        };
        when(map.getKnownCommands()).thenReturn(immutableIterator);
        var owned = new OwnedCommands();
        owned.add(own);
        assertDoesNotThrow(() -> owned.removeFrom(map));
        assertFalse(immutableIterator.containsKey("oessentials:home"));
        assertFalse(immutableIterator.containsKey("homes-alias"));
        assertSame(other, immutableIterator.get("home"));
        verify(own).unregister(map);
        verify(other, never()).unregister(map);
    }

    @Test
    void removesOnlyTheGivenCommands() {
        Command disabled = mock(Command.class);
        Command kept = mock(Command.class);
        CommandMap map = mock(CommandMap.class);
        var entries = new HashMap<String, Command>();
        entries.put("spawn", disabled);
        entries.put("oessentials:spawn", disabled);
        entries.put("warp", kept);
        when(map.getKnownCommands()).thenReturn(entries);
        var owned = new OwnedCommands();
        owned.add(disabled);
        owned.add(kept);
        owned.remove(java.util.List.of(disabled), map);
        assertEquals(Map.of("warp", kept), entries);
        verify(disabled).unregister(map);
        owned.removeFrom(map);
        assertTrue(entries.isEmpty());
        verify(disabled, times(1)).unregister(map);
    }
}
