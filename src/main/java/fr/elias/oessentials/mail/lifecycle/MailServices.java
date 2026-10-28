package fr.elias.oessentials.mail.lifecycle;

import dev.oreo.modulith.core.ModuleApi;

@ModuleApi("services")
public interface MailServices {
    fr.elias.oessentials.mail.internal.MailService getMailService();
    fr.elias.oessentials.mail.internal.MailListener getMailListener();

    /** The public OEssentials API for this module, or {@code null} while it is not available. */
    default fr.elias.oessentials.api.IMailAPI getMailApi() {
        var s = getMailService();
        return s == null ? null : new fr.elias.oessentials.mail.internal.api.MailAPIImpl(s);
    }
}
