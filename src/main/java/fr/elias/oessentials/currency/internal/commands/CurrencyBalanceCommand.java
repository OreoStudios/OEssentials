package fr.elias.oessentials.currency.internal.commands;

import fr.elias.oessentials.OEssentials;
import fr.elias.oessentials.platform.commands.OreoCommand;
import fr.elias.oessentials.currency.api.Currency;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

/**
 * Check currency balances
 * Usage: /currencybalance [currency]
 * Aliases: /cbal, /cbalance
 */
public class CurrencyBalanceCommand implements OreoCommand {

    private final OEssentials plugin;

    public CurrencyBalanceCommand(OEssentials plugin) {
        this.plugin = plugin;
    }

    @Override
    public String name() {
        return "currencybalance";
    }

    @Override
    public List<String> aliases() {
        return Arrays.asList("cbal", "cbalance", "currbal");
    }

    @Override
    public String permission() {
        return "oreo.currency.balance";
    }

    @Override
    public String usage() {
        return "[currency]";
    }

    @Override
    public boolean playerOnly() {
        return true;
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        Player player = (Player) sender;

        if (args.length == 0) {
            showAllBalances(player);
        } else {
            String currencyId = args[0].toLowerCase();
            showSingleBalance(player, currencyId);
        }

        return true;
    }

    private void showAllBalances(Player player) {
        plugin.getCurrencyService().getAllBalances(player.getUniqueId()).thenAccept(balances -> {
            List<Currency> currencies = plugin.getCurrencyService().getAllCurrencies();

            if (currencies.isEmpty()) {
                player.sendMessage("§cNo currencies available");
                return;
            }

            player.sendMessage("§6§l════════════════════════════════");
            player.sendMessage("§6§l    Your Balances");
            player.sendMessage("§6§l════════════════════════════════");

            for (Currency currency : currencies) {
                double balance = balances.getOrDefault(currency.getId(), currency.getDefaultBalance());
                player.sendMessage("§e▪ §f" + currency.getName() + ": §a" + currency.format(balance));
            }

            player.sendMessage("§6§l════════════════════════════════");
        });
    }

    private void showSingleBalance(Player player, String currencyId) {
        Currency currency = plugin.getCurrencyService().getCurrency(currencyId);

        if (currency == null) {
            player.sendMessage("§c✖ Currency not found: " + currencyId);
            player.sendMessage("§7Use §e/currencies §7to see all available currencies");
            return;
        }

        plugin.getCurrencyService().getBalance(player.getUniqueId(), currencyId).thenAccept(balance -> {
            player.sendMessage("§6§l[Balance] §f" + currency.getName() + ": §a" + currency.format(balance));
        });
    }
}