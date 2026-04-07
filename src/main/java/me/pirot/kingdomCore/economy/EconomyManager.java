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

    // ---- Shard Operations (Vault) ----

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
        if (data != null) data.addShards(amount);
    }

    /**
     * Remove shards. Returns true if successful, false if insufficient funds.
     */
    public boolean removeShards(UUID uuid, int amount) {
        PlayerData data = cache.get(uuid);
        return data != null && data.removeShards(amount);
    }

    // ---- Gem Operations ----

    public int getGems(UUID uuid) {
        PlayerData data = cache.get(uuid);
        return data != null ? data.getGems() : 0;
    }

    public void setGems(UUID uuid, int amount) {
        PlayerData data = cache.get(uuid);
        if (data != null) data.setGems(amount);
    }

    public void addGems(UUID uuid, int amount) {
        PlayerData data = cache.get(uuid);
        if (data != null) data.addGems(amount);
    }

    public boolean removeGems(UUID uuid, int amount) {
        PlayerData data = cache.get(uuid);
        return data != null && data.removeGems(amount);
    }

    // ---- XP/Level ----

    public void addXp(UUID uuid, int amount) {
        PlayerData data = cache.get(uuid);
        if (data != null) data.addXp(amount);
    }

    // ---- Currency Sync Task ----
 
    /**
     * Start the async repeating task that syncs both gems and shards from MongoDB.
     * This picks up web store purchases for in-game base and premium currencies.
     */
    public void startCurrencySyncTask() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                UUID uuid = player.getUniqueId();
                mongoManager.syncPlayerData(uuid).thenAccept(doc -> {
                    if (doc == null) return;
                    
                    PlayerData data = cache.get(uuid);
                    if (data == null) return;

                    int webGems = doc.getInteger("gems", 0);
                    int webShards = doc.getInteger("shards", 0);
                    int webBounty = doc.getInteger("bounty", 0);
                    boolean updated = false;

                    if (webGems > data.getGems()) {
                        data.setGems(webGems);
                        updated = true;
                    }
                    
                    if (webShards > data.getShards()) {
                        data.setShards(webShards);
                        updated = true;
                    }

                    if (webBounty != data.getBounty()) {
                        data.setBounty(webBounty);
                        updated = true;
                    }

                    if (updated) {
                        // Notify on main thread
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            if (player.isOnline()) {
                                player.sendMessage("§a§l[Kingdom] §7Your balances have been synced from the store!");
                                player.sendMessage("§6✦ §eGems: §b" + data.getGems() + " §7| §eShards: §a" + data.getShards());
                            }
                        });
                    }
                });
            }
        }, 100L, 100L);
    }

    public Map<UUID, PlayerData> getCache() {
        return cache;
    }
}
