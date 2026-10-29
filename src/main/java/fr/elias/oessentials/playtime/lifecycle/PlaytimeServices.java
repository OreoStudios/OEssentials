package fr.elias.oessentials.playtime.lifecycle;

import dev.oreo.modulith.core.ModuleApi;

@ModuleApi("services")
public interface PlaytimeServices {
    fr.elias.oessentials.playtime.internal.PlaytimeRewardsService getPlaytimeRewards();
    fr.elias.oessentials.playtime.internal.PlaytimeTracker getPlaytimeTracker();

    /** The public OEssentials API for this module, or {@code null} while it is not available. */
    default fr.elias.oessentials.api.IPlaytimeAPI getPlaytimeApi() {
        var tracker = getPlaytimeTracker();
        var rewards = getPlaytimeRewards();
        return (tracker == null || rewards == null) ? null : new fr.elias.oessentials.playtime.internal.api.PlaytimeAPIImpl(tracker, rewards);
    }
}
