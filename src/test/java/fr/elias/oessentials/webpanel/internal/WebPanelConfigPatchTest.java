package fr.elias.oessentials.webpanel.internal;

import com.google.gson.JsonParser;
import fr.elias.oessentials.OEssentials;
import fr.elias.oessentials.configuration.lifecycle.ConfigurationServices;
import fr.elias.oessentials.configuration.api.AutoReboot;
import fr.elias.oessentials.maintenance.internal.MaintenanceConfig;
import fr.elias.oessentials.maintenance.internal.MaintenanceService;
import fr.elias.oessentials.platform.modularity.OreoModuleRegistry;
import fr.elias.oessentials.platform.scheduling.OreScheduler;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.logging.Logger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WebPanelConfigPatchTest {
    @TempDir Path folder;

    @Test void patchRoutesMaintenanceAndReloadsRebootWithoutLosingLargeNumbers() throws Exception {
        Path file = folder.resolve("config/server/maintenance.yml");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "maintenance:\n  enabled: false\n  timer:\n    format: keep-me\n");
        OEssentials plugin = mock(OEssentials.class);
        when(plugin.getDataFolder()).thenReturn(folder.toFile());
        when(plugin.getLogger()).thenReturn(Logger.getAnonymousLogger());
        YamlConfiguration config = new YamlConfiguration();
        when(plugin.getConfig()).thenReturn(config);
        AutoReboot reboot = mock(AutoReboot.class);
        ConfigurationServices configuration = mock(ConfigurationServices.class);
        when(configuration.getAutoReboot()).thenReturn(reboot);
        OreoModuleRegistry registry = new OreoModuleRegistry();
        registry.bind(ConfigurationServices.class, configuration);
        when(plugin.getModuleRegistry()).thenReturn(registry);
        MaintenanceConfig maintenance = new MaintenanceConfig(plugin);
        MaintenanceService service = mock(MaintenanceService.class);
        when(service.getConfig()).thenReturn(maintenance);
        when(plugin.getMaintenanceService()).thenReturn(service);
        var payload = JsonParser.parseString("""
                {"settings": {
                  "maintenance.enabled": true,
                  "maintenance.whitelist": ["Alice"],
                  "maintenance.motd.line1": "New MOTD",
                  "performance.join-flood-protection.max-logins-per-window": 35,
                  "features.chat.banned-words.enabled": true,
                  "auto-reboot.enabled": true,
                  "economy.max-balance": 5000000000
                }}
                """).getAsJsonObject();
        try (var scheduler = mockStatic(OreScheduler.class)) {
            scheduler.when(() -> OreScheduler.run(eq(plugin), any(Runnable.class))).thenAnswer(call -> {
                call.getArgument(1, Runnable.class).run();
                return null;
            });
            var sync = new WebPanelSyncService(plugin, mock(WebPanelClient.class));
            var method = WebPanelSyncService.class.getDeclaredMethod("handleConfigPatch", com.google.gson.JsonObject.class);
            method.setAccessible(true);
            method.invoke(sync, payload);
        }
        assertTrue(maintenance.isEnabled());
        assertEquals("New MOTD", maintenance.getMotdLine1());
        assertEquals(java.util.List.of("Alice"), maintenance.getWhitelist());
        assertEquals("keep-me", maintenance.getTimerFormat());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> settings = ArgumentCaptor.forClass(Map.class);
        verify(configuration).updateSettings(settings.capture());
        var written = settings.getValue();
        assertEquals(java.util.Set.of(
                "performance.join-flood-protection.login-rate-limit.max-logins-per-window",
                "features.chat.banned-words.enabled",
                "auto-reboot.enabled"), written.keySet());
        assertEquals(35, ((Number) written.get("performance.join-flood-protection.login-rate-limit.max-logins-per-window")).intValue());
        assertEquals(true, written.get("features.chat.banned-words.enabled"));
        assertFalse(config.contains("maintenance.enabled"));
        assertEquals(5000000000L, config.getLong("economy.max-balance"));
        verify(service).reload();
        verify(reboot).reload();
    }
}
