package fr.elias.oessentials.shared;

/**
 * Overflow-safe arithmetic for durations typed by staff or read from config.
 *
 * <p>Durations are capped at {@link #MAX_MILLIS} so that {@code System.currentTimeMillis() + duration}
 * can never overflow into a negative (and therefore "permanent") end time.</p>
 */
public final class Durations {
    /** The longest accepted duration: 100 years. */
    public static final long MAX_MILLIS = 36_500L * 86_400_000L;

    private Durations() {}

    /** {@code value * unitMillis}, or {@code -1} when either is negative or the result exceeds {@link #MAX_MILLIS}. */
    public static long times(long value, long unitMillis) {
        if (value < 0 || unitMillis < 0) return -1;
        if (unitMillis != 0 && value > MAX_MILLIS / unitMillis) return -1;
        return value * unitMillis;
    }

    /** {@code a + b}, or {@code -1} when either is negative or the sum exceeds {@link #MAX_MILLIS}. */
    public static long plus(long a, long b) {
        if (a < 0 || b < 0 || a > MAX_MILLIS - b) return -1;
        return a + b;
    }
}
