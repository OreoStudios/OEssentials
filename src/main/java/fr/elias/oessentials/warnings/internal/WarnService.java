package fr.elias.oessentials.warnings.internal;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WarnService {

    public record WarnEntry(String id, UUID issuer, String issuerName, String reason, long issuedAt, long expiresAt) {
        /** -1 expiresAt means never expires */
        public boolean isExpired() {
            return expiresAt != -1 && System.currentTimeMillis() > expiresAt;
        }
    }

    private final Plugin plugin;
    private final File file;
    private FileConfiguration cfg;
    /** Shared network storage, or {@code null} to keep warnings in data/warnings.yml on this server. */
    private final MongoWarnStore mongo;

    /** playerUuid -> list of warnings */
    private final Map<UUID, List<WarnEntry>> warnings = new ConcurrentHashMap<>();

    // Config values
    private int maxWarnings;
    private String maxAction;        // none, kick, tempban, ban
    private long maxActionDurationMs;
    private boolean broadcastOnWarn;
    private long expireAfterMs;      // -1 = never

    private static final Pattern DURATION_TOKEN = Pattern.compile("(\\d+)([smhdw])", Pattern.CASE_INSENSITIVE);

    public WarnService(Plugin plugin) {
        this(plugin, null);
    }

    /**
     * @param shared a MongoDB collection to share warnings across servers, or {@code null} for the
     *               local data/warnings.yml (warnings then only exist on this server)
     */
    public WarnService(Plugin plugin, com.mongodb.client.MongoCollection<org.bson.Document> shared) {
        this.plugin = plugin;
        this.mongo = shared == null ? null : new MongoWarnStore(shared);

        // Save default config
        plugin.saveResource("config/server/warnings.yml", false);
        FileConfiguration warnConfig = YamlConfiguration.loadConfiguration(
                new File(plugin.getDataFolder(), "config/server/warnings.yml"));

        maxWarnings       = warnConfig.getInt("warnings.max-warnings", 3);
        maxAction         = warnConfig.getString("warnings.max-action", "tempban").toLowerCase(Locale.ROOT);
        maxActionDurationMs = parseDuration(warnConfig.getString("warnings.max-action-duration", "7d"));
        broadcastOnWarn   = warnConfig.getBoolean("warnings.broadcast-on-warn", false);
        String expStr     = warnConfig.getString("warnings.expire-after", "");
        expireAfterMs     = (expStr == null || expStr.isBlank()) ? -1 : parseDuration(expStr);

        // Load persistent warn data
        this.file = new File(plugin.getDataFolder(), "data/warnings.yml");
        if (!file.exists()) {
            try { plugin.getDataFolder().mkdirs(); file.createNewFile(); }
            catch (IOException e) { plugin.getLogger().severe("[Warn] Cannot create warnings.yml: " + e.getMessage()); }
        }
        this.cfg = YamlConfiguration.loadConfiguration(file);
        loadAll();
        if (mongo != null) importLocalWarnings();
    }

    /** First start with shared storage: copy this server's YAML warnings into MongoDB once. */
    private void importLocalWarnings() {
        if (warnings.isEmpty() || !mongo.isEmpty()) return;
        int count = 0;
        for (var e : warnings.entrySet()) {
            for (WarnEntry w : e.getValue()) { mongo.add(e.getKey(), w); count++; }
        }
        plugin.getLogger().info("[Warn] Imported " + count + " warning(s) from data/warnings.yml into MongoDB.");
    }

    // ---------------------------------------------------------------
    // Public API
    // ---------------------------------------------------------------

    /** Add a warning. Returns the new active warning count (after adding). */
    public int warn(UUID target, UUID issuer, String issuerName, String reason) {
        long now = System.currentTimeMillis();
        long expires = (expireAfterMs == -1) ? -1 : now + expireAfterMs;
        String id = UUID.randomUUID().toString().substring(0, 8);

        WarnEntry entry = new WarnEntry(id, issuer, issuerName, reason, now, expires);
        if (mongo != null) {
            mongo.add(target, entry);
            return getActiveCount(target);
        }
        warnings.computeIfAbsent(target, k -> new ArrayList<>()).add(entry);
        savePlayer(target);
        return getActiveCount(target);
    }

    /** Remove a specific warning by id. Returns true if removed. */
    public boolean removeById(UUID target, String warnId) {
        if (mongo != null) return mongo.remove(target, warnId);
        List<WarnEntry> list = warnings.get(target);
        if (list == null) return false;
        boolean removed = list.removeIf(e -> e.id().equals(warnId));
        if (removed) savePlayer(target);
        return removed;
    }

    /** Clear all warnings for a player. */
    public void clearAll(UUID target) {
        if (mongo != null) { mongo.clear(target); return; }
        warnings.remove(target);
        cfg.set("warnings." + target, null);
        saveFile();
    }

    /** Active (non-expired) warnings for a player. */
    public List<WarnEntry> getActive(UUID target) {
        List<WarnEntry> list = mongo != null ? mongo.load(target) : warnings.getOrDefault(target, Collections.emptyList());
        List<WarnEntry> active = new ArrayList<>();
        for (WarnEntry e : list) {
            if (!e.isExpired()) active.add(e);
        }
        return Collections.unmodifiableList(active);
    }

    public int getActiveCount(UUID target) {
        return getActive(target).size();
    }

    public int getMaxWarnings() { return maxWarnings; }
    public String getMaxAction() { return maxAction; }
    public long getMaxActionDurationMs() { return maxActionDurationMs; }
    public boolean isBroadcastOnWarn() { return broadcastOnWarn; }

    /** Friendly remaining time string like "6d 23h 59m" */
    public static String friendlyDuration(long ms) {
        if (ms <= 0) return "0s";
        long s = ms / 1000;
        long days = s / 86400; s %= 86400;
        long hours = s / 3600; s %= 3600;
        long mins = s / 60;    s %= 60;
        StringBuilder sb = new StringBuilder();
        if (days  > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (mins  > 0) sb.append(mins).append("m ");
        if (sb.isEmpty()) sb.append(s).append("s");
        return sb.toString().trim();
    }

    // ---------------------------------------------------------------
    // Persistence
    // ---------------------------------------------------------------

    private void loadAll() {
        var root = cfg.getConfigurationSection("warnings");
        if (root == null) return;
        for (String uuidStr : root.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                var warnSection = cfg.getConfigurationSection("warnings." + uuidStr);
                if (warnSection == null) continue;
                List<WarnEntry> list = new ArrayList<>();
                for (String key : warnSection.getKeys(false)) {
                    String path = "warnings." + uuidStr + "." + key + ".";
                    try {
                        UUID issuer     = UUID.fromString(cfg.getString(path + "issuer", uuid.toString()));
                        String issuerName = cfg.getString(path + "issuerName", "Unknown");
                        String reason   = cfg.getString(path + "reason", "No reason");
                        long issuedAt   = cfg.getLong(path + "issuedAt", System.currentTimeMillis());
                        long expiresAt  = cfg.getLong(path + "expiresAt", -1);
                        list.add(new WarnEntry(key, issuer, issuerName, reason, issuedAt, expiresAt));
                    } catch (Exception ignored) {}
                }
                if (!list.isEmpty()) warnings.put(uuid, list);
            } catch (Exception ignored) {}
        }
    }

    private void savePlayer(UUID player) {
        List<WarnEntry> list = warnings.getOrDefault(player, Collections.emptyList());
        String base = "warnings." + player + ".";
        // Clear existing
        cfg.set("warnings." + player, null);
        for (WarnEntry e : list) {
            cfg.set(base + e.id() + ".issuer",      e.issuer().toString());
            cfg.set(base + e.id() + ".issuerName",  e.issuerName());
            cfg.set(base + e.id() + ".reason",      e.reason());
            cfg.set(base + e.id() + ".issuedAt",    e.issuedAt());
            cfg.set(base + e.id() + ".expiresAt",   e.expiresAt());
        }
        saveFile();
    }

    private void saveFile() {
        try { cfg.save(file); }
        catch (IOException e) { plugin.getLogger().severe("[Warn] Failed to save warnings.yml: " + e.getMessage()); }
    }

    private static long parseDuration(String s) {
        if (s == null || s.isBlank()) return -1;
        Matcher m = DURATION_TOKEN.matcher(s);
        long total = 0;
        while (m.find()) {
            long val;
            try { val = Long.parseLong(m.group(1)); } catch (NumberFormatException e) { return -1; }
            long unit = switch (Character.toLowerCase(m.group(2).charAt(0))) {
                case 's' -> 1000L;
                case 'm' -> 60_000L;
                case 'h' -> 3_600_000L;
                case 'd' -> 86_400_000L;
                case 'w' -> 604_800_000L;
                default  -> 0L;
            };
            total = fr.elias.oessentials.shared.Durations.plus(total, fr.elias.oessentials.shared.Durations.times(val, unit));
            if (total < 0) return -1; // longer than 100 years: treated as unset
        }
        return total == 0 ? -1 : total;
    }
}
