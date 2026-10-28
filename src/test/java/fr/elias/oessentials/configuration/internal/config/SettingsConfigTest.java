package fr.elias.oessentials.configuration.internal.config;

import fr.elias.oessentials.OEssentials;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SettingsConfigTest {
    @TempDir Path folder;

    @Test
    void updateWritesKeysMirrorsLegacyBannedWordsAndReloads() throws Exception {
        Path file = folder.resolve("config/settings.yml");
        Files.createDirectories(file.getParent());
        Files.writeString(file, """
                features:
                  rtp:
                    enabled: true
                chat:
                  banned-words:
                    enabled: false
                """);
        OEssentials plugin = mock(OEssentials.class);
        when(plugin.getDataFolder()).thenReturn(folder.toFile());
        when(plugin.getLogger()).thenReturn(Logger.getAnonymousLogger());
        SettingsConfig settings = new SettingsConfig(plugin);
        assertTrue(settings.rtpEnabled());
        assertFalse(settings.bannedWordsEnabled());

        Map<String, Object> values = new LinkedHashMap<>();
        values.put("features.rtp.enabled", false);
        values.put("features.chat.banned-words.enabled", true);
        settings.update(values);

        assertFalse(settings.rtpEnabled());
        assertTrue(settings.bannedWordsEnabled(), "the legacy chat.banned-words section takes precedence and is kept in sync");
        var saved = YamlConfiguration.loadConfiguration(file.toFile());
        assertFalse(saved.getBoolean("features.rtp.enabled"));
        assertTrue(saved.getBoolean("features.chat.banned-words.enabled"));
        assertTrue(saved.getBoolean("chat.banned-words.enabled"));
        verify(plugin, never()).saveResource(anyString(), anyBoolean());
    }
}
