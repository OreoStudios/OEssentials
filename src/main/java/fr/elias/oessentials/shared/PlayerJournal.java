package fr.elias.oessentials.shared;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Receipts stored in a player's own saved data (and carried by inventory sync), written in the
 * same save as an inventory change. After a crash, a receipt present in the restored player data
 * proves the inventory change was saved; a missing receipt proves it was not. Cross-server trades
 * use this to never hand out, or take, an item twice.
 */
public final class PlayerJournal {
    public static final NamespacedKey KEY = Objects.requireNonNull(NamespacedKey.fromString("oessentials:journal"));

    private PlayerJournal() {}

    /** The raw value, as copied by inventory sync. */
    public static String raw(Player p) {
        String v = p.getPersistentDataContainer().get(KEY, PersistentDataType.STRING);
        return v == null ? "" : v;
    }

    public static void setRaw(Player p, String value) {
        if (value == null || value.isEmpty()) p.getPersistentDataContainer().remove(KEY);
        else p.getPersistentDataContainer().set(KEY, PersistentDataType.STRING, value);
    }

    public static Set<String> tokens(Player p) {
        String v = raw(p);
        return v.isEmpty() ? new LinkedHashSet<>() : Arrays.stream(v.split(",")).filter(s -> !s.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public static boolean has(Player p, String token) {
        return tokens(p).contains(token);
    }

    public static void add(Player p, String token) {
        Set<String> t = tokens(p);
        t.add(token);
        setRaw(p, String.join(",", t));
    }

    public static void remove(Player p, String token) {
        Set<String> t = tokens(p);
        if (t.remove(token)) setRaw(p, String.join(",", t));
    }
}
