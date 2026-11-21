package fr.elias.oessentials.mobs.internal;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Optional UltimateChristmas bridge supporting both the old Santa manager
 * instance API and the new static entity check introduced by the modulith port.
 *
 * This intentionally does not link against a version-specific SantaManager class:
 * its package changed between UltimateChristmas 3.x and 4.x.
 */
public final class SantaHook {

    private static volatile ClassLoader resolvedLoader;
    private static volatile Method modernIsSanta;

    private SantaHook() {}

    public static boolean isSanta(Entity entity) {
        if (entity == null) return false;

        Plugin plugin = Bukkit.getPluginManager().getPlugin("UltimateChristmas");
        if (plugin == null || !plugin.isEnabled()) return false;

        ClassLoader loader = plugin.getClass().getClassLoader();
        if (resolvedLoader != loader) {
            synchronized (SantaHook.class) {
                if (resolvedLoader != loader) {
                    modernIsSanta = findModernIsSanta(loader);
                    resolvedLoader = loader;
                }
            }
        }

        // UltimateChristmas 4.x: static SantaManager.isSanta(Entity).
        Method modern = modernIsSanta;
        if (modern != null) {
            try {
                return Boolean.TRUE.equals(modern.invoke(null, entity));
            } catch (ReflectiveOperationException | LinkageError ignored) {
                // Fall back to the old plugin API below.
            }
        }

        // UltimateChristmas 3.x: plugin.getSantaManager().isSanta(Entity).
        try {
            Object manager = plugin.getClass().getMethod("getSantaManager").invoke(plugin);
            if (manager == null) return false;
            Object result = manager.getClass().getMethod("isSanta", Entity.class).invoke(manager, entity);
            return Boolean.TRUE.equals(result);
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return false;
        }
    }

    private static Method findModernIsSanta(ClassLoader loader) {
        try {
            Class<?> type = Class.forName(
                    "fr.elias.ultimateChristmas.modules.santa.infra.SantaManager", false, loader);
            Method method = type.getMethod("isSanta", Entity.class);
            return Modifier.isStatic(method.getModifiers()) ? method : null;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return null;
        }
    }
}
