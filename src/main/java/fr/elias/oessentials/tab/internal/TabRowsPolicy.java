package fr.elias.oessentials.tab.internal;

/** Determines TAB height while respecting Minecraft's 20-rows-per-column layout. */
final class TabRowsPolicy {
    static final int ROWS_PER_COLUMN = 20;

    enum Mode {
        LEGACY, DYNAMIC, FIXED, FILL;

        static Mode parse(String value) {
            if (value == null) return LEGACY;
            try {
                return Mode.valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                return LEGACY;
            }
        }
    }

    private TabRowsPolicy() {}

    /**
     * DYNAMIC: min-rows counts player positions across all columns.
     * FIXED: min-rows counts total TAB lines in each column.
     * FILL/LEGACY: 20 rows per column (existing multi-column behavior).
     */
    static int rows(Mode mode, int columns, int playerCount, int configuredMinimum,
                    int topRows, int bottomRows) {
        if (mode == Mode.LEGACY || mode == Mode.FILL) return ROWS_PER_COLUMN;
        int cols = Math.max(1, columns);
        int players = Math.max(0, playerCount);
        int minimum = Math.max(0, configuredMinimum);
        int decorations = Math.max(0, topRows) + Math.max(0, bottomRows);
        int perColumnPlayers = (players + cols - 1) / cols;
        int requested = mode == Mode.FIXED
                ? Math.max(minimum, decorations + perColumnPlayers)
                : decorations + (Math.max(players, minimum) + cols - 1) / cols;

        // For 2 or 3 columns Minecraft needs at least 21 or 41 entries.
        int keepColumns = ((cols - 1) * ROWS_PER_COLUMN + cols) / cols;
        return Math.min(ROWS_PER_COLUMN, Math.max(requested, keepColumns));
    }
}
