package fr.elias.oessentials.currency.internal.commands;

import fr.elias.oessentials.OEssentials;
import fr.elias.oessentials.platform.commands.OreoCommand;
import fr.elias.oessentials.currency.internal.Currency;
import fr.elias.oessentials.storage.api.PlayerLocator;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Main currency management command
 * Usage: /oecurrency <create|delete|list|info|give|take|set|reload>
 *
 * Console-safe + cross-server/offline safe for give/take/set:
 * - resolves player UUID via:
 *   1) local online
 *   2) OfflinePlayerCache (if enabled)
 *   3) PlayerLocator (Mongo)
 *   4) Bukkit.getOfflinePlayer(name) fallback
 */
public class CurrencyCommand implements OreoCommand {

    private final OEssentials plugin;

    public CurrencyCommand(OEssentials plugin) {
        this.plugin = plugin;
    }

    @Override
    public String name() {
        return "oecurrency";
    }

    @Override
    public List<String> aliases() {
        return Arrays.asList("currency", "currencies", "curr");
    }

    @Override
    public String permission() {
        return "oreo.currency.admin";
    }

    @Override
    public String usage() {
        return "<create|delete|list|info|give|take|set|reload> [args...]";
    }

    @Override
    public boolean playerOnly() {
        return false;
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase(Locale.ROOT);

        switch (subCommand) {
            case "create" -> handleCreate(sender, args);
            case "delete", "remove" -> handleDelete(sender, args);
            case "list" -> handleList(sender);
            case "info" -> handleInfo(sender, args);
            case "give", "add" -> handleGive(sender, args);
            case "take", "subtract" -> handleTake(sender, args);
            case "set" -> handleSet(sender, args);
            case "reload" -> handleReload(sender);
            default -> {
                sender.sendMessage("§cUnknown subcommand: " + subCommand);
                sendHelp(sender);
            }
        }

        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6§l════════════════════════════════");
        sender.sendMessage("§6§l    Currency Management");
        sender.sendMessage("§6§l════════════════════════════════");
        sender.sendMessage("");
        sender.sendMessage("§e/oecurrency create <id> <name> <symbol> [tradeable=true] [defaultBalance=0]");
        sender.sendMessage("  §7Create a new currency");
        sender.sendMessage("");
        sender.sendMessage("§e/oecurrency delete <id>");
        sender.sendMessage("  §7Delete a currency");
        sender.sendMessage("");
        sender.sendMessage("§e/oecurrency list");
        sender.sendMessage("  §7List all currencies");
        sender.sendMessage("");
        sender.sendMessage("§e/oecurrency info <id>");
        sender.sendMessage("  §7View currency information");
        sender.sendMessage("");
        sender.sendMessage("§e/oecurrency give <player|uuid> <currency> <amount>");
        sender.sendMessage("  §7Give currency to a player (cross-server/offline supported)");
        sender.sendMessage("");
        sender.sendMessage("§e/oecurrency take <player|uuid> <currency> <amount>");
        sender.sendMessage("  §7Take currency from a player (cross-server/offline supported)");
        sender.sendMessage("");
        sender.sendMessage("§e/oecurrency set <player|uuid> <currency> <amount>");
        sender.sendMessage("  §7Set a player's balance (cross-server/offline supported)");
        sender.sendMessage("");
        sender.sendMessage("§e/oecurrency reload");
        sender.sendMessage("  §7Reload currency configuration");
        sender.sendMessage("§6§l════════════════════════════════");
    }

    private void handleCreate(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage("§cUsage: /oecurrency create <id> <name> <symbol> [tradeable=true] [defaultBalance=0]");
            sender.sendMessage("§7Example: /oecurrency create gems Gems 💎 true 0");
            return;
        }

        String id = args[1].toLowerCase(Locale.ROOT);
        String name = args[2];
        String symbol = args[3];

        if (plugin.getCurrencyService().getCurrency(id) != null) {
            sender.sendMessage("§cCurrency already exists: " + id);
            return;
        }

        boolean tradeable = args.length < 5 || Boolean.parseBoolean(args[4]);
        double defaultBalance = args.length >= 6 ? parseDouble(args[5], 0.0) : 0.0;

        Currency currency = Currency.builder()
                .id(id)
                .name(name)
                .symbol(symbol)
                .displayName(name)
                .tradeable(tradeable)
                .defaultBalance(defaultBalance)
                .crossServer(plugin.getCurrencyConfig().isCrossServerEnabled())
                .build();

        plugin.getCurrencyService().createCurrency(currency).thenAccept(success -> {
            if (success) {
                sender.sendMessage("§a✔ Created currency: §f" + name + " " + symbol + " §7(ID: " + id + ")");
                sender.sendMessage("§7Tradeable: §f" + tradeable);
                sender.sendMessage("§7Default Balance: §f" + currency.format(defaultBalance));
            } else {
                sender.sendMessage("§c✖ Failed to create currency");
            }
        });
    }

    private void handleDelete(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /oecurrency delete <id>");
            return;
        }

        String id = args[1].toLowerCase(Locale.ROOT);

        Currency currency = plugin.getCurrencyService().getCurrency(id);
        if (currency == null) {
            sender.sendMessage("§c✖ Currency not found: " + id);
            return;
        }

        plugin.getCurrencyService().deleteCurrency(id).thenAccept(success -> {
            if (success) {
                sender.sendMessage("§a✔ Deleted currency: §f" + currency.getName() + " §7(ID: " + id + ")");
            } else {
                sender.sendMessage("§c✖ Failed to delete currency");
            }
        });
    }

    private void handleList(CommandSender sender) {
        List<Currency> currencies = plugin.getCurrencyService().getAllCurrencies();

        if (currencies.isEmpty()) {
            sender.sendMessage("§6§l[Currencies] §eNo currencies found");
            return;
        }

        sender.sendMessage("§6§l════════════════════════════════");
        sender.sendMessage("§6§l    Currencies §7(" + currencies.size() + ")");
        sender.sendMessage("§6§l════════════════════════════════");

        currencies.stream()
                .sorted(Comparator.comparing(Currency::getId, String.CASE_INSENSITIVE_ORDER))
                .forEach(currency -> {
                    sender.sendMessage("§e▪ §f" + currency.getName() + " " + currency.getSymbol());
                    sender.sendMessage("  §7ID: §e" + currency.getId() + " §7| Tradeable: §f" +
                            (currency.isTradeable() ? "§aYes" : "§cNo"));
                });

        sender.sendMessage("§6§l════════════════════════════════");
    }

    private void handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /oecurrency info <id>");
            return;
        }

        String id = args[1].toLowerCase(Locale.ROOT);
        Currency currency = plugin.getCurrencyService().getCurrency(id);

        if (currency == null) {
            sender.sendMessage("§c✖ Currency not found: " + id);
            return;
        }

        sender.sendMessage("§6§l════════════════════════════════");
        sender.sendMessage("§6§l    Currency Info");
        sender.sendMessage("§6§l════════════════════════════════");
        sender.sendMessage("§7ID: §e" + currency.getId());
        sender.sendMessage("§7Name: §f" + currency.getName());
        sender.sendMessage("§7Display Name: §f" + currency.getDisplayName());
        sender.sendMessage("§7Symbol: §f" + currency.getSymbol());
        sender.sendMessage("§7Tradeable: §f" + (currency.isTradeable() ? "§aYes" : "§cNo"));
        sender.sendMessage("§7Cross-Server: §f" + (currency.isCrossServer() ? "§aYes" : "§cNo"));
        sender.sendMessage("§7Default Balance: §f" + currency.format(currency.getDefaultBalance()));
        sender.sendMessage("§6§l════════════════════════════════");
    }

    private void handleGive(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage("§cUsage: /oecurrency give <player|uuid> <currency> <amount>");
            return;
        }

        resolveTargetUuid(args[1]).thenAccept(targetUuid -> {
            if (targetUuid == null) {
                sender.sendMessage("§c✖ Player not found: " + args[1]);
                return;
            }

            String currencyId = args[2].toLowerCase(Locale.ROOT);
            Currency currency = plugin.getCurrencyService().getCurrency(currencyId);
            if (currency == null) {
                sender.sendMessage("§c✖ Currency not found: " + currencyId);
                return;
            }

            double amount = parseDouble(args[3], -1);
            if (amount <= 0) {
                sender.sendMessage("§c✖ Invalid amount: " + args[3]);
                return;
            }

            plugin.getCurrencyService().deposit(targetUuid, currencyId, amount).thenAccept(success -> {
                if (success) {
                    sender.sendMessage("§a✔ Gave " + currency.format(amount) + " to §f" + nameOrUuid(args[1], targetUuid));
                    notifyTargetIfOnlineAnywhere(targetUuid,
                            "§a✔ You received " + currency.format(amount) + " from §f" + sender.getName());
                    plugin.getLogger().info("[Currency] GIVE: " + sender.getName() + " -> " + nameOrUuid(args[1], targetUuid)
                            + " | " + currency.format(amount) + " | uuid=" + targetUuid);
                } else {
                    sender.sendMessage("§c✖ Failed to give currency (deposit returned false)");
                    plugin.getLogger().warning("[Currency] GIVE FAILED: " + sender.getName() + " -> "
                            + nameOrUuid(args[1], targetUuid) + " | " + currency.format(amount) + " | uuid=" + targetUuid);
                }
            }).exceptionally(ex -> {
                sender.sendMessage("§c✖ Error giving currency: " + ex.getMessage());
                plugin.getLogger().severe("[Currency] GIVE ERROR: " + ex.getMessage());
                ex.printStackTrace();
                return null;
            });
        });
    }

    private void handleTake(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage("§cUsage: /oecurrency take <player|uuid> <currency> <amount>");
            return;
        }

        resolveTargetUuid(args[1]).thenAccept(targetUuid -> {
            if (targetUuid == null) {
                sender.sendMessage("§c✖ Player not found: " + args[1]);
                return;
            }

            String currencyId = args[2].toLowerCase(Locale.ROOT);
            Currency currency = plugin.getCurrencyService().getCurrency(currencyId);
            if (currency == null) {
                sender.sendMessage("§c✖ Currency not found: " + currencyId);
                return;
            }

            double amount = parseDouble(args[3], -1);
            if (amount <= 0) {
                sender.sendMessage("§c✖ Invalid amount: " + args[3]);
                return;
            }

            plugin.getCurrencyService().getBalance(targetUuid, currencyId).thenAccept(currentBalance -> {
                plugin.getLogger().info("[Currency] TAKE attempt: target=" + nameOrUuid(args[1], targetUuid)
                        + " currency=" + currencyId + " currentBalance=" + currentBalance + " taking=" + amount);
                plugin.getCurrencyService().withdraw(targetUuid, currencyId, amount).thenAccept(success -> {
                    if (success) {
                        sender.sendMessage("§a✔ Took " + currency.format(amount) + " from §f" + nameOrUuid(args[1], targetUuid));
                        notifyTargetIfOnlineAnywhere(targetUuid,
                                "§c✖ " + currency.format(amount) + " was taken from you by §f" + sender.getName());
                        plugin.getLogger().info("[Currency] TAKE OK: " + sender.getName() + " took "
                                + currency.format(amount) + " from " + nameOrUuid(args[1], targetUuid));
                    } else {
                        sender.sendMessage("§c✖ Insufficient balance! §f" + nameOrUuid(args[1], targetUuid)
                                + " §7has §f" + currency.format(currentBalance) + " §7but you tried to take §f" + currency.format(amount));
                        plugin.getLogger().info("[Currency] TAKE FAILED (insufficient): balance=" + currentBalance
                                + " requested=" + amount + " currency=" + currencyId);
                    }
                }).exceptionally(ex -> {
                    sender.sendMessage("§c✖ Error taking currency: " + ex.getMessage());
                    plugin.getLogger().severe("[Currency] TAKE ERROR: " + ex.getMessage());
                    ex.printStackTrace();
                    return null;
                });
            });
        });
    }

    private void handleSet(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage("§cUsage: /oecurrency set <player|uuid> <currency> <amount>");
            return;
        }

        resolveTargetUuid(args[1]).thenAccept(targetUuid -> {
            if (targetUuid == null) {
                sender.sendMessage("§c✖ Player not found: " + args[1]);
                return;
            }

            String currencyId = args[2].toLowerCase(Locale.ROOT);
            Currency currency = plugin.getCurrencyService().getCurrency(currencyId);
            if (currency == null) {
                sender.sendMessage("§c✖ Currency not found: " + currencyId);
                return;
            }

            double amount = parseDouble(args[3], -1);
            if (amount < 0) {
                sender.sendMessage("§c✖ Invalid amount: " + args[3]);
                return;
            }

            plugin.getCurrencyService().setBalance(targetUuid, currencyId, amount).thenAccept(v -> {
                sender.sendMessage("§a✔ Set " + nameOrUuid(args[1], targetUuid) + "'s balance to " + currency.format(amount));
                notifyTargetIfOnlineAnywhere(targetUuid,
                        "§e⚠ Your " + currency.getName() + " balance was set to " + currency.format(amount) + " by §f" + sender.getName());
                plugin.getLogger().info("[Currency] SET: " + sender.getName() + " set " + nameOrUuid(args[1], targetUuid)
                        + "'s " + currencyId + " balance to " + amount + " | uuid=" + targetUuid);
            }).exceptionally(ex -> {
                sender.sendMessage("§c✖ Error setting balance: " + ex.getMessage());
                plugin.getLogger().severe("[Currency] SET ERROR: " + ex.getMessage());
                ex.printStackTrace();
                return null;
            });
        });
    }

    private void handleReload(CommandSender sender) {
        try {
            plugin.getCurrencyConfig().reload();
            sender.sendMessage("§a✔ Currency configuration reloaded");
        } catch (Exception e) {
            sender.sendMessage("§c✖ Failed to reload: " + e.getMessage());
        }
    }


    private CompletableFuture<UUID> resolveTargetUuid(String nameOrUuid) {
        return CompletableFuture.supplyAsync(() -> {
            if (nameOrUuid == null || nameOrUuid.isBlank()) return null;

            try {
                return UUID.fromString(nameOrUuid);
            } catch (IllegalArgumentException ignored) {}

            Player local = Bukkit.getPlayerExact(nameOrUuid);
            if (local != null) return local.getUniqueId();

            try {
                var cache = plugin.getModuleRegistry().read(fr.elias.oessentials.economy.lifecycle.EconomyServices.class, e -> e);
                if (cache != null) {
                    UUID cached = cache.offlinePlayerId(nameOrUuid);
                    if (cached != null) return cached;
                }
            } catch (Throwable ignored) {}

            try {
                PlayerLocator dir = plugin.getPlayerLocator();
                if (dir != null) {
                    UUID id = dir.lookupUuidByName(nameOrUuid);
                    if (id != null) return id;
                }
            } catch (Throwable ignored) {}

            OfflinePlayer off = Bukkit.getOfflinePlayer(nameOrUuid);
            if (off != null && (off.getName() != null || off.hasPlayedBefore())) {
                return off.getUniqueId();
            }
            return null;
        });
    }

    private void notifyTargetIfOnlineAnywhere(UUID targetUuid, String msg) {
        // local?
        Player local = Bukkit.getPlayer(targetUuid);
        if (local != null) {
            local.sendMessage(msg);
            return;
        }

        try {
            var pm = plugin.getPacketBus();
            var dir = plugin.getPlayerLocator();
            if (pm == null || !pm.isInitialized() || dir == null) return;

            String server = dir.lookupCurrentServer(targetUuid);
            if (server == null) return;

            pm.sendPacket(
                    fr.elias.oessentials.messaging.api.PacketChannel.individual(server),
                    new fr.elias.oessentials.messaging.api.SendRemoteMessagePacket(targetUuid, msg)
            );
        } catch (Throwable ignored) {}
    }

    private String nameOrUuid(String raw, UUID uuid) {
        if (raw != null && !raw.isBlank()) return raw;
        return uuid != null ? uuid.toString() : "unknown";
    }

    private double parseDouble(String str, double defaultValue) {
        try {
            return Double.parseDouble(str);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}