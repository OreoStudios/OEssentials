package fr.elias.oessentials.configuration.internal.config;

import fr.elias.oessentials.configuration.api.ConfigFiles;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ConfigFileRegistry implements ConfigFiles {
    private final Map<String, Runnable> reloaders = new ConcurrentHashMap<>();

    @Override
    public void register(String fileName, Runnable reload) {
        if (reloaders.putIfAbsent(fileName, reload) != null) {
            throw new IllegalStateException(fileName + " already has a reloader");
        }
    }

    @Override
    public void unregister(String fileName) {
        reloaders.remove(fileName);
    }

    @Override
    public boolean reload(String fileName) {
        Runnable reload = reloaders.get(fileName);
        if (reload == null) return false;
        reload.run();
        return true;
    }
}
