package fr.elias.oessentials.configuration.internal.config;

import fr.elias.oessentials.OEssentials;
import fr.elias.oessentials.configuration.api.BossBarToggles;
import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.SmartInventory;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.InventoryProvider;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class OeSettingsMenu implements InventoryProvider {

    private final OEssentials plugin;
    private final SettingsConfig settings;
    private final Supplier<BossBarToggles> bossBars;

    public OeSettingsMenu(OEssentials plugin, Supplier<BossBarToggles> bossBars) {
        this.plugin = plugin;
        this.bossBars = bossBars;
        this.settings = plugin.getSettingsConfig();
    }

    public static SmartInventory getInventory(OEssentials plugin, Supplier<BossBarToggles> bossBars) {
        String title = plugin.getSettingsConfig().raw().getString("settings-gui.title", "§8Settings");

        return SmartInventory.builder()
                .id("oe-settings")
                .provider(new OeSettingsMenu(plugin, bossBars))
                .manager(plugin.getInvManager())
                .size(6, 9)
                .title(title)
                .build();
    }

    @Override
    public void init(Player player, InventoryContents contents) {
        contents.fillBorders(ClickableItem.empty(createItem(Material.GRAY_STAINED_GLASS_PANE, " ", null)));

        int slot = 10;

        slot = addFeatureToggle(contents, slot, "chat", "Chat System", Material.WRITABLE_BOOK);
        slot = addFeatureToggle(contents, slot, "economy", "Economy", Material.GOLD_INGOT);
        slot = addFeatureToggle(contents, slot, "kits", "Kits", Material.CHEST);
        slot = addFeatureToggle(contents, slot, "trade", "Trading", Material.EMERALD);
        slot = addFeatureToggle(contents, slot, "tempfly", "Temporary Fly", Material.ELYTRA);
        slot = addFeatureToggle(contents, slot, "playervaults", "Player Vaults", Material.ENDER_CHEST);
        slot = addFeatureToggle(contents, slot, "rtp", "Random Teleport", Material.COMPASS);
        slot = addFeatureToggle(contents, slot, "bossbar", "Boss Bar", Material.ARMOR_STAND);
        slot = addMultiBarToggle(contents, slot, "welcome", "Welcome Bar", Material.NAME_TAG);
        slot = addMultiBarToggle(contents, slot, "staff-bar", "Staff Bar", Material.IRON_SWORD);
        slot = addFeatureToggle(contents, slot, "scoreboard", "Scoreboard", Material.ITEM_FRAME);
        slot = addFeatureToggle(contents, slot, "tab", "Tab List", Material.PLAYER_HEAD);
        slot = addFeatureToggle(contents, slot, "sit", "Sit Command", Material.OAK_STAIRS);
        slot = addFeatureToggle(contents, slot, "portals", "Custom Portals", Material.OBSIDIAN);
        slot = addFeatureToggle(contents, slot, "jumppads", "Jump Pads", Material.SLIME_BLOCK);
        slot = addFeatureToggle(contents, slot, "clearlag", "Clear Lag", Material.TNT);
        slot = addFeatureToggle(contents, slot, "mobs", "Mob Features", Material.ZOMBIE_HEAD);
        slot = addFeatureToggle(contents, slot, "oreoholograms", "Holograms", Material.ARMOR_STAND);
        slot = addFeatureToggle(contents, slot, "playtime-rewards", "Playtime Rewards", Material.CLOCK);
        slot = addFeatureToggle(contents, slot, "discord-moderation", "Discord Integration", Material.PAPER);

        contents.set(5, 4, ClickableItem.of(
                createItem(Material.BARRIER, "§cClose", List.of("§7Click to close")),
                e -> player.closeInventory()
        ));

        contents.set(5, 8, ClickableItem.of(
                createItem(Material.REDSTONE, "§aReload Plugin", List.of(
                        "§7Click to reload OEssentials",
                        "§7",
                        "§eThis will apply all changes"
                )),
                e -> {
                    player.closeInventory();
                    player.sendMessage("§aReloading OEssentials...");
                    fr.elias.oessentials.platform.scheduling.OreScheduler.run(plugin, () -> {
                        plugin.onDisable();
                        org.bukkit.event.HandlerList.unregisterAll(plugin);
                        plugin.onEnable();
                        player.sendMessage("§aOEssentials reloaded successfully!");
                    });
                }
        ));
    }

    private int addFeatureToggle(InventoryContents contents, int slot, String featureKey, String displayName, Material icon) {
        int row = slot / 9;
        int col = slot % 9;

        if (col >= 8) {
            row++;
            col = 1;
            slot = row * 9 + col;
        }

        boolean enabled = settings.isEnabled(featureKey);

        ItemStack item = createItem(
                enabled ? Material.LIME_DYE : Material.GRAY_DYE,
                (enabled ? "§a✔ " : "§c✘ ") + displayName,
                List.of(
                        "§7Status: " + (enabled ? "§aEnabled" : "§cDisabled"),
                        "§7Feature: §f" + featureKey,
                        "§7",
                        enabled ? "§eClick to disable" : "§eClick to enable"
                )
        );

        contents.set(row, col, ClickableItem.of(item, e -> {
            toggleFeature(featureKey);
            if (e.getWhoClicked() instanceof Player p) {
                getInventory(plugin, bossBars).open(p);
            }
        }));

        return slot + 1;
    }

    private int addMultiBarToggle(InventoryContents contents, int slot, String barId, String displayName, Material icon) {
        int row = slot / 9;
        int col = slot % 9;

        if (col >= 8) {
            row++;
            col = 1;
            slot = row * 9 + col;
        }

        BossBarToggles toggles = bossBars.get();
        boolean enabled = toggles != null && toggles.isBarEnabled(barId);

        ItemStack item = createItem(
                enabled ? Material.LIME_DYE : Material.GRAY_DYE,
                (enabled ? "§a✔ " : "§c✘ ") + displayName,
                List.of(
                        "§7Status: " + (enabled ? "§aEnabled" : "§cDisabled"),
                        "§7Bar: §f" + barId,
                        "§7",
                        enabled ? "§eClick to disable" : "§eClick to enable"
                )
        );

        contents.set(row, col, ClickableItem.of(item, e -> {
            toggleMultiBar(barId);
            if (e.getWhoClicked() instanceof Player p) {
                getInventory(plugin, bossBars).open(p);
            }
        }));

        return slot + 1;
    }

    private void toggleMultiBar(String barId) {
        BossBarToggles toggles = bossBars.get();
        if (toggles == null) return;
        toggles.toggleBar(barId);
    }

    private void toggleFeature(String featureKey) {
        boolean current = settings.isEnabled(featureKey);
        settings.getRoot().set("features." + featureKey + ".enabled", !current);

        try {
            settings.getRoot().save(new java.io.File(plugin.getDataFolder(), "config/settings.yml"));
            plugin.getLogger().info("[Settings] Toggled " + featureKey + ": " + !current);
        } catch (Exception ex) {
            plugin.getLogger().severe("[Settings] Failed to save: " + ex.getMessage());
        }
    }

    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null && !lore.isEmpty()) {
                meta.setLore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public void update(Player player, InventoryContents contents) {
    }
}