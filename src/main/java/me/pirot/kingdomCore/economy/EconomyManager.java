package me.pirot.kingdomCore.economy;

import me.pirot.kingdomCore.database.MongoManager;
import me.pirot.kingdomCore.database.PlayerData;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory economy cache backed by MongoDB.
 * Handles Shards (in-game) and Gems (web/premium) currencies.
 */
public class EconomyManager {

    private final JavaPlugin plugin;
    private final MongoManager mongoManager;
    private final Map<UUID, PlayerData> cache = new ConcurrentHashMap<>();

    public EconomyManager(JavaPlugin plugin, MongoManager mongoManager) {
        this.plugin = plugin;
        this.mongoManager = mongoManager;
    }

    public JavaPlugin getPlugin() {
        return plugin;
    }

    // ---- Player Data Cache ----

    /**
     * Load player data from MongoDB into cache.
     */
    public CompletableFuture<PlayerData> loadPlayer(UUID uuid) {
        return mongoManager.loadPlayerData(uuid).thenApply(data -> {
            cache.put(uuid, data);
            return data;
        });
    }

    /**
     * Save player data from cache to MongoDB.
     */
    public CompletableFuture<Void> savePlayer(UUID uuid) {
        PlayerData data = cache.get(uuid);
        if (data == null) return CompletableFuture.completedFuture(null);
        return mongoManager.savePlayerData(data);
    }

    /**
     * Save all cached players. Returns a future that completes when all saves finish.
     */
    public CompletableFuture<Void> saveAll() {
        CompletableFuture<?>[] futures = cache.entrySet().stream()
                .map(entry -> mongoManager.savePlayerData(entry.getValue()))
                .toArray(CompletableFuture[]::new);
        return CompletableFuture.allOf(futures);
    }

    /**
     * Unload a player from cache (e.g., on quit).
     */
    public void unloadPlayer(UUID uuid) {
        cache.remove(uuid);
    }

    /**
     * Get cached PlayerData. May be null if not loaded yet.
     */
    public PlayerData getPlayerData(UUID uuid) {
        return cache.get(uuid);
    }

    // ---- Shard Operations ----

    public int getShards(UUID uuid) {
        PlayerData data = cache.get(uuid);
        return data != null ? data.getShards() : 0;
    }

    public void setShards(UUID uuid, int amount) {
        double current = getShards(uuid);
        if (amount > current) {
            addShards(uuid, (int) (amount - current));
        } else if (amount < current) {
            removeShards(uuid, (int) (current - amount));
        }
    }

    public void addShards(UUID uuid, int amount) {
        PlayerData data = cache.get(uuid);
        if (data != null) {
            data.addShards(amount);
            data.updateLocal();
            savePlayer(uuid);
        }
    }

    /**
     * Remove shards. Returns true if successful, false if insufficient funds.
     */
    public boolean removeShards(UUID uuid, int amount) {
        PlayerData data = cache.get(uuid);
        if (data != null && data.removeShards(amount)) {
            data.updateLocal();
            savePlayer(uuid);
            return true;
        }
        return false;
    }

    // ---- Gem Operations ----

    public int getGems(UUID uuid) {
        PlayerData data = cache.get(uuid);
        return data != null ? data.getGems() : 0;
    }

    public void setGems(UUID uuid, int amount) {
        PlayerData data = cache.get(uuid);
        if (data != null) {
            data.setGems(amount);
            data.updateLocal();
            savePlayer(uuid);
        }
    }

    public void addGems(UUID uuid, int amount) {
        PlayerData data = cache.get(uuid);
        if (data != null) {
            data.addGems(amount);
            data.updateLocal();
            savePlayer(uuid);
        }
    }

    public boolean removeGems(UUID uuid, int amount) {
        PlayerData data = cache.get(uuid);
        if (data != null && data.removeGems(amount)) {
            data.updateLocal();
            savePlayer(uuid);
            return true;
        }
        return false;
    }

    /**
     * Check if player has enough shards and gems.
     */
    public boolean hasBalance(UUID uuid, int shards, int gems) {
        PlayerData data = cache.get(uuid);
        if (data == null) return false;
        return data.getShards() >= shards && data.getGems() >= gems;
    }

    // ---- XP/Level ----

    public void addXp(UUID uuid, int amount) {
        PlayerData data = cache.get(uuid);
        if (data != null) {
            int levelsGained = data.addXp(amount);
            data.updateLocal();
            savePlayer(uuid);

            if (levelsGained > 0) {
                Document log = new Document("timestamp", new java.util.Date())
                        .append("source", "GAME")
                        .append("type", "LEVEL_UP")
                        .append("player", new Document("uuid", uuid.toString())
                                .append("name", data.getLastKnownName()))
                        .append("summary", "Leveled up " + levelsGained + " time(s)! New Level: " + data.getLevel())
                        .append("metadata", new Document("levelsGained", levelsGained)
                                .append("newLevel", data.getLevel())
                                .append("xpLeft", data.getXp()));
                mongoManager.logAction(log);
            }
        }
    }

    // ---- Stat Operations ----

    public void addKill(UUID uuid) {
        PlayerData data = cache.get(uuid);
        if (data != null) {
            data.addKills(1);
            data.updateLocal();
            savePlayer(uuid);
        }
    }

    public void addDeath(UUID uuid) {
        PlayerData data = cache.get(uuid);
        if (data != null) {
            data.addDeaths(1);
            data.updateLocal();
            savePlayer(uuid);
        }
    }

    public void setLevel(UUID uuid, int level) {
        PlayerData data = cache.get(uuid);
        if (data != null) {
            data.setLevel(level);
            data.updateLocal();
            savePlayer(uuid);
        }
    }

    public void setBounty(UUID uuid, int amount, boolean add) {
        PlayerData data = cache.get(uuid);
        if (data != null) {
            if (add) data.addBounty(amount);
            else data.setBounty(amount);
            data.updateLocal();
            savePlayer(uuid);
        }
    }

    public void setClassName(UUID uuid, String className) {
        PlayerData data = cache.get(uuid);
        if (data != null) {
            data.setClassName(className);
            data.updateLocal();
            savePlayer(uuid);
        }
    }

    public void fullReset(UUID uuid) {
        PlayerData data = cache.get(uuid);
        if (data != null) {
            data.setXp(0);
            data.setLevel(1);
            data.setShards(0);
            data.setBounty(0);
            data.setKills(0);
            data.setDeaths(0);
            data.updateLocal();
            savePlayer(uuid);
        }
    }

    // ---- Currency Sync Task ----
 
    /**
     * Start the async repeating task that syncs both gems and shards from MongoDB.
     * This picks up web store purchases for in-game base and premium currencies.
     */
    /**
     * Start async tasks for synchronization and persistence.
     */
    public void startSyncTasks() {
        // Task 1: Polling Sync (Fallback for real-time updates)
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                UUID uuid = player.getUniqueId();
                mongoManager.syncPlayerData(uuid).thenAccept(doc -> {
                    if (doc == null) return;
                    
                    PlayerData data = cache.get(uuid);
                    if (data == null) return;

                    // If we updated locally in the last 10 seconds, ignore the sync to avoid reverts
                    // while MongoDB is catching up with our recent saves.
                    if (System.currentTimeMillis() - data.getLastLocalUpdate() < 10000) {
                        return;
                    }

                    int webGems = doc.getInteger("gems", 0);
                    int webShards = doc.getInteger("shards", 0);
                    int webBounty = doc.getInteger("bounty", 0);
                    int webLevel = doc.getInteger("level", data.getLevel());
                    
                    boolean updated = false;

                    // Sync Gems, Shards, Level, and Bounty if they differ from local
                    // Note: We use != to allow admin overrides (even reductions)
                    if (webGems != data.getGems()) {
                        data.setGems(webGems);
                        updated = true;
                    }
                    
                    if (webShards != data.getShards()) {
                        data.setShards(webShards);
                        updated = true;
                    }

                    if (webLevel != data.getLevel()) {
                        data.setLevel(webLevel);
                        updated = true;
                    }

                    if (webBounty != data.getBounty()) {
                        data.setBounty(webBounty);
                        updated = true;
                    }

                    if (updated) {
                        // Silent update
                    }
                });
            }
        }, 200L, 200L); // Every 10 seconds

        // Task 2: Periodic Save (Ensures DB is current for the web panel)
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            if (cache.isEmpty()) return;
            plugin.getLogger().info("[KingdomCore] Performing periodic background save for " + cache.size() + " players...");
            saveAll();
        }, 6000L, 6000L); // Every 5 minutes
    }

    public Map<UUID, PlayerData> getCache() {
        return cache;
    }
}
