package fr.elias.oessentials.inventory.internal.auctionhouse;

import fr.elias.oessentials.api.IAuctionHouseAPI;
import java.util.*;

/** Pure aggregation of completed sales; one auction counted once across network copies. */
final class AuctionFinance {
    record Sale(String id, UUID seller, UUID buyer, String currencyId, double price, Double proceeds) {}
    private AuctionFinance() {}

    static Map<String, IAuctionHouseAPI.FinancialTotals> summarize(UUID player, List<Sale> sales) {
        if (player == null || sales == null || sales.isEmpty()) return Map.of();
        Map<String, double[]> sums = new HashMap<>();
        Set<String> visited = new HashSet<>();
        for (Sale sale : sales) {
            if (sale == null || sale.id() == null || !visited.add(sale.id())
                    || !Double.isFinite(sale.price()) || sale.price() < 0) continue;
            boolean sold = player.equals(sale.seller()), bought = player.equals(sale.buyer());
            if (!sold && !bought) continue;
            String currency = sale.currencyId() == null || sale.currencyId().isBlank()
                    ? "vault" : sale.currencyId().toLowerCase(Locale.ROOT);
            double[] amounts = sums.computeIfAbsent(currency, ignored -> new double[2]);
            if (sold) {
                // Old sold records did not store net proceeds; use gross price.
                double payout = sale.proceeds() == null ? sale.price() : sale.proceeds();
                if (Double.isFinite(payout) && payout >= 0) amounts[0] += payout;
            }
            if (bought) amounts[1] += sale.price();
        }
        Map<String, IAuctionHouseAPI.FinancialTotals> result = new HashMap<>();
        sums.forEach((currency, amounts) ->
                result.put(currency, new IAuctionHouseAPI.FinancialTotals(amounts[0], amounts[1])));
        return Map.copyOf(result);
    }
}
