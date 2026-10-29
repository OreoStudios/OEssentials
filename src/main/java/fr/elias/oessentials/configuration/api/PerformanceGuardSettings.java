package fr.elias.oessentials.configuration.api;

import dev.oreo.modulith.core.ModuleApi;

/** The {@code performance.*} section of {@code config/settings.yml}, with its defaults and bounds applied. */
@ModuleApi("services")
public interface PerformanceGuardSettings {
    boolean joinFloodProtectionEnabled();
    int joinFloodWindowMillis();
    int joinFloodUiSoftJoinsPerWindow();
    long joinFloodBaseUiDelayTicks();
    long joinFloodExtraUiDelayTicks();
    long joinFloodMaxUiDelayTicks();
    boolean joinFloodLoginRateLimitEnabled();
    int joinFloodMaxLoginsPerWindow();
    String joinFloodKickMessage();

    boolean containerSpamGuardEnabled();
    int containerSpamGuardWindowMillis();
    int containerSpamGuardMaxScore();
    int containerSpamGuardShiftWeight();
    int containerSpamGuardTransferWeight();
    int containerSpamGuardDragSlotWeight();
    boolean containerSpamGuardCloseInventory();
    int containerSpamGuardKickAfterStrikes();
    int containerSpamGuardWarnCooldownMillis();
    String containerSpamGuardWarnMessage();
    String containerSpamGuardKickMessage();
}
