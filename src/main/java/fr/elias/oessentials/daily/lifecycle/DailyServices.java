package fr.elias.oessentials.daily.lifecycle;

import dev.oreo.modulith.core.ModuleApi;

@ModuleApi("services")
public interface DailyServices {
    fr.elias.oessentials.daily.internal.DailyConfig getDailyConfig();
    fr.elias.oessentials.daily.internal.RewardsConfig getDailyRewardsConfig();
    fr.elias.oessentials.daily.internal.DailyService getDailyService();
    fr.elias.oessentials.daily.internal.DailyMongoStore getDailyStore();

    /** The public OEssentials API for this module, or {@code null} while it is not available. */
    default fr.elias.oessentials.api.IDailyAPI getDailyApi() {
        var s = getDailyService();
        return s == null ? null : new fr.elias.oessentials.daily.internal.api.DailyAPIImpl(s);
    }
}
