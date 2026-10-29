package fr.elias.oessentials.shards.lifecycle;

import dev.oreo.modulith.core.ModuleApi;

@ModuleApi("services")
public interface ShardingServices {
    fr.elias.oessentials.shards.internal.OreoShardsModule getShardsModule();

    /** The public OEssentials API for this module, or {@code null} while it is not available. */
    default fr.elias.oessentials.api.IShardsAPI getShardsApi() {
        var s = getShardsModule();
        return s == null ? null : new fr.elias.oessentials.shards.internal.api.ShardsAPIImpl(s);
    }
}
