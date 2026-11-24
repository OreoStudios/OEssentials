package fr.elias.oessentials.mobs.internal;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataType;

import java.util.Locale;

final class HealthBarMobName {
    // CustomiZer writes this tag and the formatted custom name on spawn and restore.
    private static final NamespacedKey CUSTOMIZER_DEFINITION = new NamespacedKey("customizer", "entity_definition");

    private HealthBarMobName() {}

    static String resolve(LivingEntity mob, String mythicName) {
        if (mythicName != null && !mythicName.isEmpty()) return mythicName;

        if (mob.getPersistentDataContainer().has(CUSTOMIZER_DEFINITION, PersistentDataType.STRING)) {
            Component name = mob.customName();
            if (name != null) {
                String formatted = MiniMessage.miniMessage().serialize(name);
                if (!formatted.isBlank()) return formatted;
            }
        }

        String base = mob.getType().name().toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(base.charAt(0)) + base.substring(1);
    }
}
