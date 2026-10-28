package fr.elias.oessentials.configuration.internal.config;

import fr.elias.oessentials.api.IAfkAPI;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class PublicApiRegistryTest {
    @Test
    void providesUntilWithdrawnAndRejectsASecondProvider() {
        var registry = new PublicApiRegistry();
        assertNull(registry.get(IAfkAPI.class), "absent module -> null API");

        IAfkAPI afk = mock(IAfkAPI.class);
        registry.provide(IAfkAPI.class, () -> afk);
        assertSame(afk, registry.get(IAfkAPI.class));
        assertThrows(IllegalStateException.class, () -> registry.provide(IAfkAPI.class, () -> afk));

        registry.withdraw(IAfkAPI.class);
        assertNull(registry.get(IAfkAPI.class));
    }

    @Test
    void providerIsAskedOnEveryLookup() {
        var registry = new PublicApiRegistry();
        IAfkAPI[] current = {null};
        registry.provide(IAfkAPI.class, () -> current[0]);
        assertNull(registry.get(IAfkAPI.class), "service not created yet");
        current[0] = mock(IAfkAPI.class);
        assertSame(current[0], registry.get(IAfkAPI.class));
    }
}
