package fr.elias.oessentials.mobs.internal;

import fr.elias.ultimateChristmas.UltimateChristmas;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;


public final class GrinchHook {
    private static boolean lookedUp = false;
    private static UltimateChristmas uc = null;
    private static Object grinchManager = null;

    // UltimateChristmas stamps every boss it spawns with this persistent-data
    // key (NamespacedKey(plugin, "boss_id")), and its festive NPCs with
    // "npc_role". Reading those tags off the entity is version-proof: it needs
    // no method on the UltimateChristmas plugin, so it keeps working across its
    // refactors. These entities draw their own boss bar / nameplate, so the
    // health bar must leave them alone.
    private static final NamespacedKey UC_BOSS = NamespacedKey.fromString("ultimatechristmas:boss_id");
    private static final NamespacedKey UC_NPC  = NamespacedKey.fromString("ultimatechristmas:npc_role");

    private GrinchHook() {}

    /**
     * True for any entity UltimateChristmas owns: a boss (incl. the standalone
     * Sneaky/Strong/Crazy minion bosses, all tagged "boss_id"), a boss's
     * summoned minion (Bukkit metadata "uc_boss_minion"), or a festive NPC.
     * All of these draw their own boss bar / nameplate, so the health bar
     * must leave them alone.
     */
    public static boolean isChristmasManaged(Entity e) {
        if (e == null) return false;
        try {
            if (e.hasMetadata("uc_boss_minion")) return true;
            var pdc = e.getPersistentDataContainer();
            return (UC_BOSS != null && pdc.has(UC_BOSS))
                    || (UC_NPC != null && pdc.has(UC_NPC));
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void ensure() {
        if (lookedUp) return;
        lookedUp = true;
        var p = Bukkit.getPluginManager().getPlugin("UltimateChristmas");
        if (p instanceof UltimateChristmas u) {
            uc = u;
            try {
                grinchManager = uc.getClass().getMethod("getGrinchManager").invoke(uc);
            } catch (Throwable ignored) {
                grinchManager = null;
            }
        }
    }


    public static boolean isGrinch(Entity e) {
        ensure();
        if (uc == null || e == null) return false;


        try {
            var m = uc.getClass().getMethod("isGrinchEntity", Entity.class);
            Object out = m.invoke(uc, e);
            if (out instanceof Boolean b) return b;
        } catch (Throwable ignored) { }

        if (grinchManager != null) {
            try {
                var m = grinchManager.getClass().getMethod("isGrinch", Entity.class);
                Object out = m.invoke(grinchManager, e);
                if (out instanceof Boolean b) return b;
            } catch (Throwable ignored) { }
            try {
                var m = grinchManager.getClass().getMethod("isGrinchEntity", Entity.class);
                Object out = m.invoke(grinchManager, e);
                if (out instanceof Boolean b) return b;
            } catch (Throwable ignored) { }
        }

        return false;
    }
}
