package fr.elias.oessentials.shared;

/** RabbitMQ connection addresses as typed in config files. */
public final class AmqpUris {
    private AmqpUris() {}

    /**
     * {@code amqp://user:pass@host:5672/} means the empty vhost to the RabbitMQ client, which
     * the broker rejects ("vhost not found"). A bare trailing slash is meant as the default vhost,
     * so it is dropped; any other path (e.g. {@code /prod} or {@code /%2F}) is kept as written.
     */
    public static String normalize(String uri) {
        if (uri == null) return null;
        String u = uri.trim();
        int scheme = u.indexOf("://");
        if (scheme >= 0 && u.endsWith("/") && u.indexOf('/', scheme + 3) == u.length() - 1) {
            return u.substring(0, u.length() - 1);
        }
        return u;
    }
}
