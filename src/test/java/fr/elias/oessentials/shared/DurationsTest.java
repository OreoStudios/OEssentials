package fr.elias.oessentials.shared;

import fr.elias.oessentials.maintenance.internal.MaintenanceService;
import fr.elias.oessentials.mute.api.MuteService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Huge typed durations used to overflow into negative ("permanent") or garbage end times. */
class DurationsTest {
    private static final long DAY = 86_400_000L;

    @Test
    void multipliesAndAddsWithinTheCap() {
        assertEquals(2 * DAY, Durations.times(2, DAY));
        assertEquals(Durations.MAX_MILLIS, Durations.times(36_500, DAY));
        assertEquals(3 * DAY, Durations.plus(DAY, 2 * DAY));
        assertEquals(0, Durations.times(0, DAY));
    }

    @Test
    void rejectsOverflowNegativesAndAnythingPastOneHundredYears() {
        assertEquals(-1, Durations.times(36_501, DAY));
        assertEquals(-1, Durations.times(200_000_000_000L, DAY));
        assertEquals(-1, Durations.times(Long.MAX_VALUE, 1000));
        assertEquals(-1, Durations.times(-5, DAY));
        assertEquals(-1, Durations.plus(Durations.MAX_MILLIS, 1));
        assertEquals(-1, Durations.plus(-1, DAY));
    }

    @Test
    void endTimeNeverOverflowsForAnAcceptedDuration() {
        assertTrue(System.currentTimeMillis() + Durations.MAX_MILLIS > 0);
    }

    @Test
    void muteRejectsAbsurdDurationsInsteadOfMutingForever() {
        assertEquals(-1, MuteService.parseDurationToMillis("200000000000d"));
        assertEquals(-1, MuteService.parseDurationToMillis("106751991167d"));
        assertEquals(-1, MuteService.parseDurationToMillis("99999999999999999999"));
        assertEquals(2 * DAY, MuteService.parseDurationToMillis("2d"));
        assertEquals(120_000, MuteService.parseDurationToMillis("120"));
    }

    @Test
    void timeTextTreatsAbsurdDurationsAsNotADuration() {
        assertEquals(0, TimeText.parseToMillis("200000000000d"));
        assertEquals(3 * 3_600_000L, TimeText.parseToMillis("3h"));
    }

    @Test
    void maintenanceRejectsAbsurdDurations() {
        assertThrows(IllegalArgumentException.class, () -> MaintenanceService.parseDuration("200000000000d"));
        assertThrows(IllegalArgumentException.class, () -> MaintenanceService.parseDuration("36500d 1d"));
        assertEquals(DAY + 30 * 60_000L, MaintenanceService.parseDuration("1d 30m"));
    }
}
