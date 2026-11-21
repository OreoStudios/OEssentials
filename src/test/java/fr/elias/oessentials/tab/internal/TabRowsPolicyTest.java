package fr.elias.oessentials.tab.internal;

import org.junit.jupiter.api.Test;
import static fr.elias.oessentials.tab.internal.TabRowsPolicy.Mode.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TabRowsPolicyTest {
    @Test void zeroMinimumFollowsOnlinePlayers() {
        assertEquals(5, TabRowsPolicy.rows(DYNAMIC, 1, 1, 0, 2, 2));
        assertEquals(12, TabRowsPolicy.rows(DYNAMIC, 1, 8, 0, 2, 2));
    }

    @Test void configurableMinimumNeverHidesJoinedPlayers() {
        assertEquals(9, TabRowsPolicy.rows(DYNAMIC, 1, 1, 5, 2, 2));
        assertEquals(9, TabRowsPolicy.rows(DYNAMIC, 1, 5, 5, 2, 2));
        assertEquals(12, TabRowsPolicy.rows(DYNAMIC, 1, 8, 5, 2, 2));
    }

    @Test void dynamicallyFitsPlayersInActiveColumns() {
        assertEquals(13, TabRowsPolicy.rows(DYNAMIC, 2, 17, 0, 2, 2));
        assertEquals(17, TabRowsPolicy.rows(DYNAMIC, 2, 25, 0, 2, 2));
        assertEquals(19, TabRowsPolicy.rows(DYNAMIC, 3, 43, 0, 2, 2));
    }

    @Test void maintainsMinecraftColumnBreakpoints() {
        assertEquals(11, TabRowsPolicy.rows(DYNAMIC, 2, 1, 0, 2, 2));
        assertEquals(14, TabRowsPolicy.rows(DYNAMIC, 3, 1, 0, 2, 2));
    }

    @Test void fixedCountsDecorationRows() {
        assertEquals(7, TabRowsPolicy.rows(FIXED, 1, 1, 7, 2, 2));
        assertEquals(12, TabRowsPolicy.rows(FIXED, 1, 8, 7, 2, 2));
    }

    @Test void legacyFillAndInvalidMode() {
        assertEquals(20, TabRowsPolicy.rows(LEGACY, 1, 1, 0, 2, 2));
        assertEquals(20, TabRowsPolicy.rows(FILL, 3, 1, 5, 2, 2));
        assertEquals(LEGACY, TabRowsPolicy.Mode.parse("unknown"));
        assertEquals(DYNAMIC, TabRowsPolicy.Mode.parse("dynamic"));
    }

    @Test void clampsMinimumAndMinecraftLimit() {
        assertEquals(5, TabRowsPolicy.rows(DYNAMIC, 1, 1, -5, 2, 2));
        assertEquals(20, TabRowsPolicy.rows(DYNAMIC, 1, 30, 0, 2, 2));
    }
}
