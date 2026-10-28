package fr.elias.oessentials.scoreboard.lifecycle;

import dev.oreo.modulith.core.ModuleApi;
import fr.elias.oessentials.scoreboard.internal.ScoreboardService;

@ModuleApi("services")
public interface ScoreboardServices {
    ScoreboardService getScoreboardService();

    /** The public OEssentials API for this module, or {@code null} while it is not available. */
    default fr.elias.oessentials.api.IScoreboardAPI getScoreboardApi() {
        var s = getScoreboardService();
        return s == null ? null : new fr.elias.oessentials.scoreboard.internal.api.ScoreboardAPIImpl(s);
    }
}
