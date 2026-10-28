package fr.elias.oessentials.nametag.internal;

import fr.elias.oessentials.configuration.api.BossBarToggles;

import java.util.function.Supplier;
import java.util.logging.Logger;

/** Lets the /oesettings menu switch the multi-bossbars defined in the nameplates config. */
public final class BossBarSettingsToggles implements BossBarToggles {
    private final CustomNameplatesConfig config;
    private final Supplier<MultiBossBarService> bars;
    private final Logger logger;

    public BossBarSettingsToggles(CustomNameplatesConfig config, Supplier<MultiBossBarService> bars, Logger logger) {
        this.config = config;
        this.bars = bars;
        this.logger = logger;
    }

    @Override
    public boolean isBarEnabled(String barId) {
        MultiBossBarService service = bars.get();
        return service != null && service.isBarEnabled(barId);
    }

    @Override
    public void toggleBar(String barId) {
        var cfg = config.raw();
        String path = "bossbars.bars." + barId + ".enabled";
        boolean current = cfg.getBoolean(path, true);
        cfg.set(path, !current);
        config.save();

        // Reload the multi-bossbar service to apply the change immediately
        MultiBossBarService service = bars.get();
        if (service != null) {
            service.reload(cfg);
        }

        logger.info("[Settings] Toggled multi-bossbar '" + barId + "': " + !current);
    }
}
