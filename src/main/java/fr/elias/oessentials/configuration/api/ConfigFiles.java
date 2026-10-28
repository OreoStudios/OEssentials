package fr.elias.oessentials.configuration.api;

import dev.oreo.modulith.core.ModuleApi;

/**
 * Who reloads which configuration file, keyed by its canonical name (e.g. {@code tab.yml}).
 * Used when the web panel pushes a file; each module registers the files it owns.
 */
@ModuleApi("services")
public interface ConfigFiles {
    void register(String fileName, Runnable reload);

    void unregister(String fileName);

    /** Reloads the file in memory; {@code false} when no running module owns it. */
    boolean reload(String fileName);
}
