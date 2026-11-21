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
        int chunkSize = (players + cols - 1) / cols;
        int lastColumnPlayers = Math.max(0, players - (cols - 1) * chunkSize);
        int previousColumnCapacity = (cols - 1) * Math.max(0, ROWS_PER_COLUMN - decorations);
        int lastColumnMinimum = Math.max(0, minimum - previousColumnCapacity);
        int requested = mode == Mode.FIXED
                ? Math.max(minimum, decorations + lastColumnPlayers)
                : decorations + Math.max(lastColumnPlayers, lastColumnMinimum);

        // Earlier columns stay full (20 slots). The last column contains
        // its own decoration and players and has at least one entry.
        return Math.min(ROWS_PER_COLUMN, Math.max(1, requested));
    }
}
