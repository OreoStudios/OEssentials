package fr.elias.oessentials.inventory.internal.auctionhouse;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class AuctionFinanceTest {
    private final UUID seller = UUID.randomUUID();
    private final UUID buyer = UUID.randomUUID();
    private final UUID third = UUID.randomUUID();

    @Test void sellerNetAndBuyerGross() {
        var sale = new AuctionFinance.Sale("a", seller, buyer, null, 100.0, 95.0);
        assertEquals(95.0, AuctionFinance.summarize(seller, List.of(sale)).get("vault").profit());
        assertEquals(0.0, AuctionFinance.summarize(seller, List.of(sale)).get("vault").expend());
        assertEquals(100.0, AuctionFinance.summarize(buyer, List.of(sale)).get("vault").expend());
    }

    @Test void separateCurrenciesAndDeduplicate() {
        var a = new AuctionFinance.Sale("a", seller, buyer, null, 100.0, 90.0);
        var b = new AuctionFinance.Sale("b", third, seller, "GEMS", 4.5, 4.0);
        var totals = AuctionFinance.summarize(seller, List.of(a, a, b));
        assertEquals(90.0, totals.get("vault").profit());
        assertEquals(4.5, totals.get("gems").expend());
        assertEquals(2, totals.size());
    }

    @Test void historicalSalesAndInvalidAmounts() {
        var old = new AuctionFinance.Sale("old", seller, buyer, "", 25.0, null);
        var bad = new AuctionFinance.Sale("bad", seller, buyer, null, Double.NaN, null);
        assertEquals(25.0, AuctionFinance.summarize(seller, List.of(old, bad)).get("vault").profit());
        assertTrue(AuctionFinance.summarize(third, List.of(old)).isEmpty());
    }
}
