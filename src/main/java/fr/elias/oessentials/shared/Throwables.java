package fr.elias.oessentials.shared;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** Readable one-line descriptions of failures for console logs. */
public final class Throwables {
    private Throwables() {}

    /**
     * The messages of {@code t} and its causes, e.g. {@code "IOException <- ShutdownSignalException:
     * NOT_ALLOWED - vhost  not found"}. Libraries such as the RabbitMQ client throw exceptions whose
     * own message is {@code null} and keep the reason in the cause, which used to be logged as "null".
     */
    public static String describe(Throwable t) {
        if (t == null) return "unknown error";
        List<String> parts = new ArrayList<>();
        Map<Throwable, Boolean> seen = new IdentityHashMap<>();
        for (Throwable c = t; c != null && seen.put(c, true) == null && parts.size() < 5; c = c.getCause()) {
            String msg = c.getMessage();
            parts.add(c.getClass().getSimpleName() + (msg == null || msg.isBlank() ? "" : ": " + msg.trim()));
        }
        return String.join(" <- ", parts);
    }
}
