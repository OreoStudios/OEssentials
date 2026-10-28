package fr.elias.oessentials.proxy.lifecycle;

import dev.oreo.modulith.core.ModuleApi;
import fr.elias.oessentials.proxy.internal.transport.ProxyMessenger;

@ModuleApi("services")
public interface ProxyMessagingServices {
    ProxyMessenger getProxyMessenger();
    fr.elias.oessentials.commandcontrol.internal.aliases.AliasService getAliasService();

    /** The public OEssentials API for this module, or {@code null} while it is not available. */
    default fr.elias.oessentials.api.IAliasesAPI getAliasesApi() {
        var s = getAliasService();
        return s == null ? null : new fr.elias.oessentials.commandcontrol.internal.api.AliasesAPIImpl(s);
    }
}
