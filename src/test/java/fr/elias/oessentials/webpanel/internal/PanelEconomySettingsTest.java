package fr.elias.oessentials.webpanel.internal;

import fr.elias.oessentials.economy.internal.persistence.database.JsonEconomyDatabase;
import fr.elias.oessentials.redis.internal.persistence.database.RedisManager;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PanelEconomySettingsTest {
    @TempDir Path folder;

    @Test void configuredStartingBalanceAndBoundsSurviveRestart() {
        JavaPlugin plugin = mock(JavaPlugin.class);
        when(plugin.getDataFolder()).thenReturn(folder.toFile());
        var config = new YamlConfiguration();
        config.set("economy.starting-balance", 275.0);
        config.set("economy.min-balance", 10.0);
        config.set("economy.max-balance", 500.0);
        when(plugin.getConfig()).thenReturn(config);
        RedisManager redis = mock(RedisManager.class);
        when(redis.getBalance(any(UUID.class))).thenReturn(null);
        var database = new JsonEconomyDatabase(plugin, redis);
        assertTrue(database.connect("", "", ""));
        UUID id = UUID.randomUUID();
        assertEquals(275.0, database.getOrCreateBalance(id, "Alice"));
        database.setBalance(id, "Alice", 999.0);
        assertEquals(500.0, database.getBalance(id));
        database.setBalance(id, "Alice", -50.0);
        assertEquals(10.0, database.getBalance(id));
        database.close();
        config.set("economy.allow-negative", true);
        database = new JsonEconomyDatabase(plugin, redis);
        assertTrue(database.connect("", "", ""));
        assertEquals(10.0, database.getBalance(id));
        database.setBalance(id, "Alice", -50.0);
        assertEquals(-50.0, database.getBalance(id));
        database.close();
    }
}
