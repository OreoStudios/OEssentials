package fr.elias.oessentials.chat.internal;

import fr.elias.oessentials.platform.config.CustomConfig;

import fr.elias.oessentials.OEssentials;
import fr.elias.oessentials.platform.scheduling.OreScheduler;
import fr.elias.oessentials.chat.internal.channels.ChatChannel;
import fr.elias.oessentials.chat.internal.channels.ChatChannelHandler;
import fr.elias.oessentials.chat.internal.channels.ChatChannelManager;
import fr.elias.oessentials.mute.api.MuteService;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;


@SuppressWarnings("UnstableApiUsage")
public class AsyncChatListenerWithChannels implements Listener {

    private final OEssentials plugin;
    private final ChatChannelManager channelManager;
    private final ChatChannelHandler channelHandler;
    private final MuteService muteService;

    public AsyncChatListenerWithChannels(
            OEssentials plugin,
            FormatManager formatManager,
            CustomConfig chatConfig,
            ChatSyncManager syncManager,
            boolean discordEnabled,
            String discordWebhookUrl,
            MuteService muteService,
            ChatChannelManager channelManager
    ) {
        this.plugin = plugin;
        this.channelManager = channelManager;
        this.muteService = muteService;
        this.channelHandler = new ChatChannelHandler(
                plugin, channelManager, syncManager, formatManager, chatConfig,
                discordEnabled, discordWebhookUrl
        );

        Bukkit.getLogger().info("[Chat] AsyncChatListenerWithChannels initialized (Paper AsyncChatEvent)");
    }


    /**
     * Intercepts orders/AH chat input at LOW priority — BEFORE OreoFactions' HIGHEST handler.
     * If the player is waiting for chat input (qty, price, etc.), we cancel the event here
     * so OreoFactions' ignoreCancelled=true handler never sees the message.
     * For normal messages we do nothing, letting OreoFactions handle them as usual.
     */
    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = false)
    public void onLegacyChatInput(AsyncPlayerChatEvent event) {
        final Player player = event.getPlayer();
        final String raw = event.getMessage();

        var inventories = plugin.getModuleRegistry().read(fr.elias.oessentials.inventory.lifecycle.InventoriesServices.class, i -> i);
        if (inventories != null && inventories.consumeChatInput(player, raw)) {
            event.setCancelled(true);
            event.getRecipients().clear();
        }
    }

    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onLegacyChat(AsyncPlayerChatEvent event) {
        if (!plugin.getFeatureSettings().chatEnabled()) return;
        event.setCancelled(true);
        event.getRecipients().clear();
    }

    /**
     * Handles AH/Orders chat input at LOW priority with ignoreCancelled=false so it always fires,
     * even when OreoFactions has already cancelled the event (e.g. player is in faction chat mode).
     */
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = false)
    public void onChatInput(AsyncChatEvent event) {
        final Player player = event.getPlayer();
        var inventories = plugin.getModuleRegistry().read(fr.elias.oessentials.inventory.lifecycle.InventoriesServices.class, i -> i);
        if (inventories != null && inventories.consumeChatInput(player,
                PlainTextComponentSerializer.plainText().serialize(event.message()))) {
            event.viewers().clear();
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onChat(AsyncChatEvent event) {
        // Chat module disabled — leave the event alone so CHC (or other chat plugins) can handle it
        if (!plugin.getFeatureSettings().chatEnabled()) return;

        // OreoFactions signals it handled a faction channel by cancelling the event AND clearing
        // viewers. If both are true, skip — faction chat already delivered the message.
        // If cancelled but viewers are still present, this is Paper's compat pre-cancel from
        // AsyncPlayerChatEvent being cancelled — we still own global chat in that case.
        if (event.isCancelled() && event.viewers().isEmpty()) return;

        event.viewers().clear();
        event.setCancelled(true);

        final Player player = event.getPlayer();
        final fr.elias.oessentials.modgui.api.ChatControls gui = plugin.getModuleRegistry().read(fr.elias.oessentials.modgui.lifecycle.ModGuiServices.class, fr.elias.oessentials.modgui.lifecycle.ModGuiServices::getChatControls);

        if (gui != null && gui.chatMuted()) {
            player.sendMessage("§cChat is currently muted.");
            return;
        }

        if (gui != null && gui.getSlowmodeSeconds() > 0) {
            if (!gui.canSendMessage(player.getUniqueId())) {
                long left = gui.getRemainingSlowmode(player.getUniqueId());
                player.sendMessage("§cYou must wait §e" + left + "s §cbefore chatting again.");
                return;
            }
            gui.recordMessage(player.getUniqueId());
        }

        if (muteService != null && muteService.isMuted(player.getUniqueId())) {
            player.sendMessage("§cYou are muted and cannot send messages.");
            return;
        }

        if (gui != null && gui.isStaffChatEnabled(player.getUniqueId())) {
            final String staffMsg = PlainTextComponentSerializer.plainText().serialize(event.message());
            OreScheduler.run(plugin, () -> {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.hasPermission("oreo.staffchat")) {
                        p.sendMessage("§b[StaffChat] §f" + player.getName() + ": §7" + staffMsg);
                    }
                }
            });
            return;
        }

        var notes = plugin.getModuleRegistry().read(fr.elias.oessentials.notes.lifecycle.NotesServices.class, n -> n);
        if (notes != null && notes.isWaitingForNote(player.getUniqueId())) return;

        if (channelManager != null && channelManager.isEnabled()) {
            final String message = PlainTextComponentSerializer.plainText().serialize(event.message());
            final ChatChannel channel = channelManager.getPlayerChannel(player);
            OreScheduler.run(plugin, () ->
                    channelHandler.sendChannelMessage(player, message, channel));
        }
    }
}
