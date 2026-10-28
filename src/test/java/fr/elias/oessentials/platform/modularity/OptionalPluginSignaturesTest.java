package fr.elias.oessentials.platform.modularity;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Module lifecycle classes are reflected on every server (the module event bus calls
 * {@code getDeclaredMethods()}), which loads every type named in their method signatures. A type
 * from an optional plugin there makes OEssentials fail to start when that plugin is missing, as
 * happened with PlaceholderAPI's expansion type on the currency module.
 */
class OptionalPluginSignaturesTest {
    /** Packages of soft dependencies that are not shaded into the plugin jar. */
    private static final List<String> OPTIONAL = List.of(
            "me.clip.placeholderapi.",
            "net.milkbowl.vault.",
            "dev.lone.itemsadder.",
            "com.nexomc.",
            "com.github.retrooper.packetevents.",
            "io.github.retrooper.packetevents.",
            "net.luckperms.",
            "com.sk89q.");

    @Test
    void moduleMethodSignaturesNameNoOptionalPluginType() {
        var offenders = new ArrayList<String>();
        for (Class<?> module : OreoModules.TYPES) {
            for (Method method : module.getDeclaredMethods()) {
                String signature = method.toGenericString();
                for (String pkg : OPTIONAL) {
                    if (signature.contains(pkg)) offenders.add(module.getSimpleName() + ": " + signature);
                }
            }
        }
        assertEquals(List.of(), offenders,
                "Keep optional-plugin types out of module method signatures (use Object or a separate holder class)");
    }
}
