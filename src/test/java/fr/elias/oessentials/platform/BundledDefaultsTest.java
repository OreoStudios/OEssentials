package fr.elias.oessentials.platform;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Default files that modules copy out of the jar on first start. The auction house, orders and
 * portals once looked for them at paths from before the resources moved under config/ and data/,
 * so they wrote empty or no files and every message showed as a missing key.
 */
class BundledDefaultsTest {
    private static final List<String> DEFAULTS = List.of(
            "/config/auctionhouse/config.yml",
            "/config/auctionhouse/messages.yml",
            "/config/auctionhouse/gui.yml",
            "/config/auctionhouse/categories.yml",
            "/data/orders/settings.yml",
            "/data/orders/lang_en.yml",
            "/data/orders/lang_fr.yml",
            "/data/orders/gui.yml",
            "/data/portals/config.yml");

    @Test
    void everyCopiedDefaultIsBundled() {
        var missing = new ArrayList<String>();
        for (String path : DEFAULTS) {
            if (BundledDefaultsTest.class.getResource(path) == null) missing.add(path);
        }
        assertEquals(List.of(), missing);
    }
}
