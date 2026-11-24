package fr.elias.oessentials.mobs.internal;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class HealthBarMobNameTest {
    private final LivingEntity mob = mock(LivingEntity.class);
    private final PersistentDataContainer data = mock(PersistentDataContainer.class);
    private final NamespacedKey key = new NamespacedKey("customizer", "entity_definition");

    HealthBarMobNameTest() {
        when(mob.getPersistentDataContainer()).thenReturn(data);
        when(mob.getType()).thenReturn(EntityType.HUSK);
    }

    @Test void displaysSentinelNameAndColor() {
        when(data.has(key, PersistentDataType.STRING)).thenReturn(true);
        Component name = Component.text("Clockwork Sentinel", NamedTextColor.AQUA);
        when(mob.customName()).thenReturn(name);
        assertEquals(name, MiniMessage.miniMessage().deserialize(HealthBarMobName.resolve(mob, null)));
    }

    @Test void vanillaHuskKeepsItsTypeLabel() {
        when(mob.customName()).thenReturn(Component.text("Someone renamed this husk"));
        assertEquals("Husk", HealthBarMobName.resolve(mob, null));
    }

    @Test void taggedHuskWithoutANameFallsBackToType() {
        when(data.has(key, PersistentDataType.STRING)).thenReturn(true);
        assertEquals("Husk", HealthBarMobName.resolve(mob, ""));
        when(mob.customName()).thenReturn(Component.empty());
        assertEquals("Husk", HealthBarMobName.resolve(mob, null));
    }

    @Test void keepsMythicNamePrecedence() {
        assertEquals("Mythic Guardian", HealthBarMobName.resolve(mob, "Mythic Guardian"));
        verifyNoInteractions(data);
    }
}
