package me.pirot.kingdomCore.database;

import me.pirot.kingdomCore.economy.EconomyManager;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.Sorts;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * Handles all MongoDB operations asynchronously via CompletableFuture.
 */
public class MongoManager {

    private final Logger logger;
    private MongoClient mongoClient;
    private MongoDatabase database;
    private MongoCollection<Document> playerCollection;
    private MongoCollection<Document> auctionCollection;
    private MongoCollection<Document> shopsCollection;
    private MongoCollection<Document> purchasesCollection;
    private MongoCollection<Document> gameLogsCollection;
    private final ExecutorService executor;

    public MongoManager(Logger logger) {
        this.logger = logger;
        this.executor = Executors.newFixedThreadPool(4);
    }

    public void connect(String uri, String dbName, String playerCollName, String auctionCollName) {
        try {
            this.mongoClient = MongoClients.create(uri);
            this.database = mongoClient.getDatabase(dbName);
            this.playerCollection = database.getCollection(playerCollName);
            this.auctionCollection = database.getCollection(auctionCollName);
            this.shopsCollection = database.getCollection("shops");
            this.purchasesCollection = database.getCollection("purchases");
            this.gameLogsCollection = database.getCollection("game_logs");
            logger.info("[KingdomCore] Connected to MongoDB.");
            logger.info("[KingdomCore] Active Database: " + dbName);
            logger.info("[KingdomCore] Player Collection: " + playerCollName);
        } catch (Exception e) {
            logger.severe("[KingdomCore] Failed to connect to MongoDB: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void logAction(Document log) {
        if (gameLogsCollection == null) return;
        if (!log.containsKey("timestamp")) log.append("timestamp", new java.util.Date());
        executor.submit(() -> {
            try { gameLogsCollection.insertOne(log); } 
            catch (Exception e) { logger.warning("[KingdomCore] Failed to log action: " + e.getMessage()); }
        });
    }

    public CompletableFuture<List<Document>> getRecentLogs(int limit, int skip) {
        return CompletableFuture.supplyAsync(() -> {
            List<Document> logs = new ArrayList<>();
            if (gameLogsCollection == null) return logs;
            try {
                gameLogsCollection.find()
                        .sort(Sorts.descending("timestamp"))
                        .skip(skip)
                        .limit(limit)
                        .into(logs);
            } catch (Exception e) { logger.warning("[KingdomCore] Logs fetch error: " + e.getMessage()); }
            return logs;
        }, executor);
    }

    public void startPurchaseWatchStream(EconomyManager economyManager) {
        executor.submit(() -> {
            try {
                purchasesCollection.watch().fullDocument(com.mongodb.client.model.changestream.FullDocument.UPDATE_LOOKUP).forEach(change -> {
                    Document fullDoc = change.getFullDocument();
                    if (fullDoc != null) {
                        String status = fullDoc.getString("status");
                        if ("delivered".equalsIgnoreCase(status) || "completed".equalsIgnoreCase(status)) {
                            String uuidStr = fullDoc.getString("minecraftUuid");
                            if (uuidStr == null) return;
                            UUID uuid = UUID.fromString(uuidStr);
                            Bukkit.getScheduler().runTask(economyManager.getPlugin(), () -> {
                                Player player = Bukkit.getPlayer(uuid);
                                if (player != null && player.isOnline()) {
                                    player.sendMessage("§b§l[Kingdom Store] §7Delivery complete: §e" + fullDoc.getString("itemName"));
                                }
                            });
                        }
                    }
                });
            } catch (Exception e) { logger.warning("[KingdomCore] Watch Stream Error: " + e.getMessage()); }
        });
    }

    public CompletableFuture<PlayerData> loadPlayerData(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Document doc = playerCollection.find(Filters.eq("uuid", uuid.toString())).first();
                if (doc == null) { PlayerData d = new PlayerData(uuid); savePlayerDataSync(d); return d; }
                return documentToPlayerData(doc);
            } catch (Exception e) { return new PlayerData(uuid); }
        }, executor);
    }

    public CompletableFuture<Void> savePlayerData(PlayerData data) {
        return CompletableFuture.runAsync(() -> savePlayerDataSync(data), executor);
    }

    private void savePlayerDataSync(PlayerData data) {
        try {
            playerCollection.replaceOne(Filters.eq("uuid", data.getUuid().toString()), playerDataToDocument(data), new ReplaceOptions().upsert(true));
        } catch (Exception e) { logger.warning("Save error: " + e.getMessage()); }
    }

    public CompletableFuture<Document> syncPlayerData(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try { return playerCollection.find(Filters.eq("uuid", uuid.toString())).first(); }
            catch (Exception e) { return null; }
        }, executor);
    }

    public void startPlayerWatchStream(EconomyManager economyManager) {
        executor.submit(() -> {
            try {
                playerCollection.watch().fullDocument(com.mongodb.client.model.changestream.FullDocument.UPDATE_LOOKUP).forEach(change -> {
                    Document fullDoc = change.getFullDocument();
                    if (fullDoc != null && fullDoc.containsKey("uuid")) {
                        UUID uuid = UUID.fromString(fullDoc.getString("uuid"));
                        PlayerData dbData = documentToPlayerData(fullDoc);
                        Bukkit.getScheduler().runTask(economyManager.getPlugin(), () -> {
                            PlayerData localData = economyManager.getCache().get(uuid);
                            if (localData != null && System.currentTimeMillis() - localData.getLastLocalUpdate() > 10000) {
                                localData.setShards(dbData.getShards()); 
                                localData.setGems(dbData.getGems());
                                localData.setXp(dbData.getXp()); 
                                localData.setLevel(dbData.getLevel());
                                localData.setBounty(dbData.getBounty()); 
                                localData.setClassName(dbData.getClassName());
                            }
                        });
                    }
                });
            } catch (Exception e) { logger.warning("Watch Error: " + e.getMessage()); }
        });
    }

    public MongoCollection<Document> getAuctionCollection() { return auctionCollection; }
    public MongoCollection<Document> getShopsCollection() { return shopsCollection; }
    public ExecutorService getExecutor() { return executor; }
    public MongoDatabase getDatabase() { return database; }

    public void close() {
        if (mongoClient != null) mongoClient.close();
        executor.shutdown();
    }

    private Document playerDataToDocument(PlayerData data) {
        Document progressDoc = new Document();
        for (Map.Entry<String, PlayerData.ClassProgress> entry : data.getAllClassProgress().entrySet()) {
            progressDoc.append(entry.getKey(), new Document().append("level", entry.getValue().level).append("xp", entry.getValue().xp));
        }
        return new Document()
                .append("uuid", data.getUuid().toString())
                .append("lastKnownName", data.getLastKnownName())
                .append("class", data.getClassName())
                .append("shards", data.getShards())
                .append("gems", data.getGems())
                .append("xp", data.getXp())
                .append("level", data.getLevel())
                .append("bounty", data.getBounty())
                .append("kills", data.getKills())
                .append("deaths", data.getDeaths())
                .append("online", data.isOnline())
                .append("classProgress", progressDoc);
    }

    private PlayerData documentToPlayerData(Document doc) {
        PlayerData data = new PlayerData(
                UUID.fromString(doc.getString("uuid")),
                doc.getString("lastKnownName"),
                doc.getString("class"),
                getInt(doc, "shards", 0),
                getInt(doc, "gems", 0),
                getInt(doc, "xp", 0),
                getInt(doc, "level", 1),
                getInt(doc, "bounty", 0),
                getInt(doc, "kills", 0),
                getInt(doc, "deaths", 0),
                doc.getBoolean("scoreboardEnabled", true),
                doc.getBoolean("online", false)
        );
        Document progressDoc = doc.get("classProgress", Document.class);
        if (progressDoc != null) {
            Map<String, PlayerData.ClassProgress> progress = new HashMap<>();
            for (String key : progressDoc.keySet()) {
                Document entry = progressDoc.get(key, Document.class);
                if (entry != null) {
                    progress.put(key, new PlayerData.ClassProgress(
                        getInt(entry, "level", 1), 
                        getInt(entry, "xp", 0)
                    ));
                }
            }
            data.setClassProgress(progress);
        }
        return data;
    }

    /**
     * Safely extracts an integer from a MongoDB document.
     * Handles cases where the value might be stored as an Integer, Long, or Double.
     */
    private int getInt(Document doc, String key, int defaultValue) {
        Object val = doc.get(key);
        if (val instanceof Number n) return n.intValue();
        return defaultValue;
    }
}
