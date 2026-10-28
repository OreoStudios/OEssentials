package fr.elias.oessentials.autotest;

import com.oreo.lib.autotest.AdversarialInput;
import com.oreo.lib.autotest.AutoTest;
import com.oreo.lib.autotest.Metamorphic;
import com.oreo.lib.autotest.MetamorphicRule;
import com.oreo.lib.autotest.NotNullResult;
import com.oreo.lib.autotest.NullableInput;
import com.oreo.lib.autotest.ResultRange;
import com.oreo.lib.autotest.RoundTrip;
import com.oreo.lib.autotest.TestRange;
import fr.elias.oessentials.commandtoggle.internal.CommandToggleConfig;
import fr.elias.oessentials.maintenance.internal.MaintenanceService;
import fr.elias.oessentials.mute.api.MuteService;
import fr.elias.oessentials.platform.ui.ConfirmDialogHandler;
import fr.elias.oessentials.shared.Durations;
import fr.elias.oessentials.shared.TimeText;

/**
 * Contracts of pure text helpers, checked on generated and hostile inputs by OreoLib's autotest
 * (run from {@link TextHelperContractsTest}). Each method states one claim about production code.
 */
public class TextHelperContracts {
    public TextHelperContracts() {}

    // --- durations typed by staff (/mute, /ban, maintenance timers) ---------------------------

    /** Maintenance durations from 1s to under a day are printed to the second and read back unchanged
     *  (0 prints as "0s", which parseDuration rejects on purpose: a zero timer is invalid). */
    @AutoTest(samples = 500)
    @RoundTrip("maintenanceSeconds")
    public String maintenanceText(@TestRange(min = 1, max = 86_399) long seconds) {
        return MaintenanceService.formatDuration(seconds * 1000L);
    }

    public long maintenanceSeconds(String text) {
        return MaintenanceService.parseDuration(text) / 1000L;
    }

    /** /mute durations never throw; garbage reads as -1 (invalid), never as a negative duration. */
    @AutoTest(samples = 300)
    @ResultRange(min = -1, max = Long.MAX_VALUE)
    public long muteDuration(@AdversarialInput String token) {
        return MuteService.parseDurationToMillis(token);
    }

    /** Shared duration parser never throws and never yields a negative duration. */
    @AutoTest(samples = 300)
    @ResultRange(min = 0, max = Long.MAX_VALUE)
    public long timeTextDuration(@NullableInput @AdversarialInput String text) {
        return TimeText.parseToMillis(text);
    }

    @AutoTest(samples = 300)
    @NotNullResult
    public String timeTextFormat(@TestRange(min = 0, max = 1.0E15) long millis) {
        return TimeText.format(millis);
    }

    @AutoTest(samples = 300)
    @NotNullResult
    public String muteRemaining(@TestRange(min = -1, max = 1.0E15) long millis) {
        return MuteService.friendlyRemaining(millis);
    }

    /** Duration arithmetic is either in [0, 100 years] or -1; it never wraps. */
    @AutoTest(samples = 500)
    @ResultRange(min = -1, max = Durations.MAX_MILLIS)
    public long durationTimes(@TestRange(min = -10, max = 9.0E18) long value,
                              @TestRange(min = 0, max = 604_800_000) long unitMillis) {
        return Durations.times(value, unitMillis);
    }

    @AutoTest(samples = 500)
    @ResultRange(min = -1, max = Durations.MAX_MILLIS)
    public long durationPlus(@TestRange(min = -10, max = 9.0E18) long a,
                             @TestRange(min = -10, max = 9.0E18) long b) {
        return Durations.plus(a, b);
    }

    // --- command arguments --------------------------------------------------------------------

    /** Confirm-dialog arguments keep only [A-Za-z0-9_.-]; sanitising twice changes nothing. */
    @AutoTest(samples = 300)
    @Metamorphic({MetamorphicRule.IDEMPOTENT, MetamorphicRule.DETERMINISTIC})
    public String sanitizeCommandArg(@NullableInput @AdversarialInput String raw) {
        return ConfirmDialogHandler.sanitizeCommandArg(raw);
    }

    @AutoTest(samples = 300)
    @ResultRange(min = 0, max = 0)
    public int unsafeCharsAfterSanitizing(@NullableInput @AdversarialInput String raw) {
        return ConfirmDialogHandler.sanitizeCommandArg(raw).replaceAll("[A-Za-z0-9_.\\-]", "").length();
    }

    /** '/foo', 'plugin:foo' and 'FOO' all normalise to the same label, and normalising is stable. */
    @AutoTest(samples = 300)
    @Metamorphic({MetamorphicRule.IDEMPOTENT, MetamorphicRule.DETERMINISTIC})
    public String commandLabel(@NullableInput @AdversarialInput String raw) {
        return CommandToggleConfig.normalizeLabel(raw);
    }
}
