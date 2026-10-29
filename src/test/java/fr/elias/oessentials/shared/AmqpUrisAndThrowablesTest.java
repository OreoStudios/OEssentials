package fr.elias.oessentials.shared;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AmqpUrisAndThrowablesTest {
    @Test
    void bareTrailingSlashMeansTheDefaultVhost() {
        assertEquals("amqp://guest:guest@localhost:5672", AmqpUris.normalize("amqp://guest:guest@localhost:5672/"));
        assertEquals("amqp://guest:guest@localhost:5672", AmqpUris.normalize(" amqp://guest:guest@localhost:5672 "));
    }

    @Test
    void explicitVhostsAreKept() {
        assertEquals("amqps://u:p@host/prod", AmqpUris.normalize("amqps://u:p@host/prod"));
        assertEquals("amqp://u:p@host/%2F", AmqpUris.normalize("amqp://u:p@host/%2F"));
        assertEquals("amqp://u:p@host/prod/", AmqpUris.normalize("amqp://u:p@host/prod/"));
    }

    @Test
    void describesTheCauseWhenTheExceptionHasNoMessage() {
        var failure = new IOException(null, new IllegalStateException("NOT_ALLOWED - vhost  not found"));
        assertEquals("IOException <- IllegalStateException: NOT_ALLOWED - vhost  not found", Throwables.describe(failure));
        assertEquals("unknown error", Throwables.describe(null));
    }
}
