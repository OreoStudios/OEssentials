package fr.elias.oessentials.configuration.internal.config;

import fr.elias.oessentials.configuration.api.ReloadSteps;
import org.bukkit.command.CommandSender;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ReloadStepRegistry implements ReloadSteps {
    private record Entry(int order, String name, Step step) {}

    private final Map<String, Entry> steps = new ConcurrentHashMap<>();

    @Override
    public void register(int order, String name, Step step) {
        if (steps.putIfAbsent(name, new Entry(order, name, step)) != null) {
            throw new IllegalStateException("Reload step '" + name + "' is already registered");
        }
    }

    @Override
    public void unregister(String name) {
        steps.remove(name);
    }

    @Override
    public int[] runAll(CommandSender sender) {
        int reloaded = 0;
        int skipped = 0;
        List<Entry> ordered = steps.values().stream()
                .sorted(Comparator.comparingInt(Entry::order).thenComparing(Entry::name))
                .toList();
        for (Entry entry : ordered) {
            switch (entry.step().reload(sender)) {
                case RELOADED -> reloaded++;
                case SKIPPED -> skipped++;
                case FAILED -> { }
            }
        }
        return new int[]{reloaded, skipped};
    }
}
