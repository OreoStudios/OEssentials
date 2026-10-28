package fr.elias.oessentials.notes.lifecycle;

import dev.oreo.modulith.core.ModuleApi;
import fr.elias.oessentials.notes.internal.notes.NotesChatListener;
import fr.elias.oessentials.notes.internal.notes.PlayerNotesManager;

@ModuleApi("services")
public interface NotesServices {
    PlayerNotesManager getNotesManager();
    NotesChatListener getNotesChat();

    /** Whether {@code playerId}'s next chat line is being captured as a note. */
    default boolean isWaitingForNote(java.util.UUID playerId) {
        var chat = getNotesChat();
        return chat != null && chat.isWaitingForNote(playerId);
    }

    /** Opens the staff notes of {@code target}. */
    default void openNotes(org.bukkit.entity.Player viewer, java.util.UUID target, String targetName) {
        var plugin = fr.elias.oessentials.OEssentials.get();
        fr.minuskube.inv.SmartInventory.builder()
                .manager(plugin.getInvManager())
                .provider(new fr.elias.oessentials.notes.internal.notes.PlayerNotesMenu(plugin, getNotesManager(), getNotesChat(), target))
                .title(fr.elias.oessentials.shared.Lang.color(fr.elias.oessentials.shared.Lang.get("modgui.actions.notes-title", "&8Notes: %name%")
                        .replace("%name%", targetName)))
                .size(6, 9)
                .build()
                .open(viewer);
    }
}
