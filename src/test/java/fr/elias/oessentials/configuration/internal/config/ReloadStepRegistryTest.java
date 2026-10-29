package fr.elias.oessentials.configuration.internal.config;

import fr.elias.oessentials.configuration.api.ReloadSteps.Outcome;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class ReloadStepRegistryTest {
    @Test
    void runsStepsInOrderAndCountsReloadedAndSkippedOnly() {
        var registry = new ReloadStepRegistry();
        List<String> ran = new ArrayList<>();
        registry.register(9, "scoreboard", sender -> { ran.add("scoreboard"); return Outcome.FAILED; });
        registry.register(3, "chat", sender -> { ran.add("chat"); return Outcome.RELOADED; });
        registry.register(4, "tab", sender -> { ran.add("tab"); return Outcome.SKIPPED; });
        registry.register(5, "kits", sender -> { ran.add("kits"); return Outcome.RELOADED; });

        int[] counts = registry.runAll(mock(CommandSender.class));

        assertEquals(List.of("chat", "tab", "kits", "scoreboard"), ran);
        assertArrayEquals(new int[]{2, 1}, counts);
    }

    @Test
    void aStepIsRegisteredOnceAndCanBeRemoved() {
        var registry = new ReloadStepRegistry();
        registry.register(1, "chat", sender -> Outcome.RELOADED);
        assertThrows(IllegalStateException.class, () -> registry.register(2, "chat", sender -> Outcome.RELOADED));
        registry.unregister("chat");
        assertArrayEquals(new int[]{0, 0}, registry.runAll(mock(CommandSender.class)));
    }
}
