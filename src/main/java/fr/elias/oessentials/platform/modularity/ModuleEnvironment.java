package fr.elias.oessentials.platform.modularity;

import dev.oreo.modulith.core.ModuleContext;
import fr.elias.oessentials.OEssentials;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.Objects;
import java.util.function.Supplier;

/** Typed composition-root parameters shared by OEssentials feature factories. */
public record ModuleEnvironment(OEssentials plugin, OreoModuleRegistry registry) {
    public ModuleEnvironment {
        Objects.requireNonNull(plugin, "plugin");
        Objects.requireNonNull(registry, "registry");
    }

    public static ModuleEnvironment of(OEssentials plugin) {
        return new ModuleEnvironment(plugin, plugin.getModuleRegistry());
    }

    public <M extends ManagedModule> M create(Supplier<M> constructor, ModuleContext context) {
        if (context.platform(JavaPlugin.class) != plugin) {
            throw new IllegalArgumentException("Module environment belongs to another plugin");
        }
        M module = Objects.requireNonNull(constructor.get(), "module factory returned null");
        module.configure(this);
        return module;
    }
}
