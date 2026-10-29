package fr.elias.oessentials.platform.modularity;

import fr.elias.oessentials.OEssentials;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import static org.junit.jupiter.api.Assertions.*;

class LegacyPluginApiTest {
    @Test
    void releasedFacadeMethodsRemainAvailableWithExplicitlyRelocatedTypes() throws Exception {
        Map<String, String> relocations;
        try (var input = getClass().getResourceAsStream("/bootstrap/class-relocations.json")) {
            assertNotNull(input);
            relocations = new Gson().fromJson(new InputStreamReader(input, StandardCharsets.UTF_8),
                    new TypeToken<Map<String, String>>() {}.getType());
        }
        try (var input = getClass().getResourceAsStream("/bootstrap/legacy-plugin-api.txt")) {
            assertNotNull(input);
            var lines = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8)).lines()
                    .filter(line -> !line.isBlank() && !line.startsWith("#")).toList();
            // 118: getTradeBroker() was retired in 7.1.6 with the packet-based cross-server trade broker.
            assertEquals(118, lines.size());
            for (String line : lines) {
                String[] parts = line.split("\\|", -1);
                Class<?>[] parameters = parts[2].isEmpty() ? new Class<?>[0]
                        : Arrays.stream(parts[2].split(",")).map(name -> type(relocations.getOrDefault(name, name))).toArray(Class<?>[]::new);
                var method = OEssentials.class.getDeclaredMethod(parts[0], parameters);
                assertEquals(relocations.getOrDefault(parts[1], parts[1]), method.getReturnType().getTypeName(), line);
                assertTrue(java.lang.reflect.Modifier.isPublic(method.getModifiers()), line);
            }
        }
    }

    private static Class<?> type(String name) {
        return switch (name) {
            case "long" -> long.class;
            case "int" -> int.class;
            case "boolean" -> boolean.class;
            default -> {
                try { yield Class.forName(name); }
                catch (ClassNotFoundException e) { throw new AssertionError(name, e); }
            }
        };
    }
}
