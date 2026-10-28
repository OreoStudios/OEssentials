package fr.elias.oessentials.configuration.internal.config;

import org.bukkit.OfflinePlayer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class PlaceholderRegistryTest {
    @Test
    void firstResolverThatHandlesTheIdAnswers() {
        var registry = new PlaceholderRegistry();
        var player = mock(OfflinePlayer.class);
        registry.register("kits", (p, id, raw) -> id.startsWith("kit") ? "kits:" + id : null);
        registry.register("playtime", (p, id, raw) -> id.startsWith("playtime") ? "playtime:" + id : null);

        assertEquals("kits:kits_count", registry.resolve(player, "kits_count", "kits_count"));
        assertEquals("playtime:playtime_total_hours", registry.resolve(player, "playtime_total_hours", "playtime_total_hours"));
        assertNull(registry.resolve(player, "world_name", "world_name"));

        registry.unregister("kits");
        assertNull(registry.resolve(player, "kits_count", "kits_count"), "a stopped module answers nothing");
        assertThrows(IllegalStateException.class, () -> registry.register("playtime", (p, id, raw) -> null));
    }
}
