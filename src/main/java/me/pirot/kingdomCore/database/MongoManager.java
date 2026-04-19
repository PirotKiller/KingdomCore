package me.pirot.kingdomCore.database;

import me.pirot.kingdomCore.economy.EconomyManager;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
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
    private com.mongodb.client.ChangeStreamIterable<Document> playerChangeStream;
    private final ExecutorService executor;

    public MongoManager(Logger logger) {
        this.logger = logger;
        this.executor = Executors.newFixedThreadPool(4);
    }

    /**
     * Connect to MongoDB. Call from onEnable.
     */
    public void connect(String uri, String dbName, String playerCollName, String auctionCollName) {
        try {
            this.mongoClient = MongoClients.create(uri);
            this.database = mongoClient.getDatabase(dbName);
            this.playerCollection = database.getCollection(playerCollName);
            this.auctionCollection = database.getCollection(auctionCollName);
            this.shopsCollection = database.getCollection("shops");
            this.purchasesCollection = database.getCollection("purchases");
            logger.info("[KingdomCore] Connected to MongoDB: " + dbName);
        } catch (Exception e) {
            logger.severe("[KingdomCore] Failed to connect to MongoDB: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Start a watch stream for the purchases collection to notify players of real-time store deliveries.
     */
    public void startPurchaseWatchStream(EconomyManager economyManager) {
        executor.submit(() -> {
            try {
                logger.info("[KingdomCore] Starting real-time MongoDB watch stream for purchases...");
                purchasesCollection.watch()
                        .fullDocument(com.mongodb.client.model.changestream.FullDocument.UPDATE_LOOKUP)
                        .forEach(change -> {
                            // We look for inserts or updates where status becomes 'delivered'
                            Document fullDoc = change.getFullDocument();
                            if (fullDoc != null) {
                                String status = fullDoc.getString("status");
                                if ("delivered".equalsIgnoreCase(status) || "completed".equalsIgnoreCase(status)) {
                                    String uuidStr = fullDoc.getString("minecraftUuid");
                                    String itemName = fullDoc.getString("itemName");
                                    
                                    if (uuidStr != null) {
                                        UUID uuid = UUID.fromString(uuidStr);
                                        Bukkit.getScheduler().runTask(economyManager.getPlugin(), () -> {
                                            Player player = Bukkit.getPlayer(uuid);
                                            if (player != null && player.isOnline()) {
                                                player.sendMessage("");
                                                player.sendMessage("§b§l§m     §b§l[ §f§lKINGDOM STORE §b§l]§b§l§m     ");
                                                player.sendMessage("§7Thank you for your purchase, §f" + player.getName() + "§7!");
                                                player.sendMessage("§7Items: §e" + (itemName != null ? itemName : "Premium Content"));
                                                player.sendMessage("§aYour rewards have been added to your account.");
                                                player.sendMessage("§b§l§m                          ");
                                                player.sendMessage("");
                                            }
                                        });
                                    }
                                }
                            }
                        });
            } catch (Exception e) {
                logger.warning("[KingdomCore] Purchase Watch Stream error: " + e.getMessage());
            }
        });
    }

    /**
     * Load a player's data from MongoDB asynchronously.
     */
    public CompletableFuture<PlayerData> loadPlayerData(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Document doc = playerCollection.find(Filters.eq("uuid", uuid.toString())).first();
                if (doc == null) {
                    // New player — create default data
                    PlayerData data = new PlayerData(uuid);
                    savePlayerDataSync(data);
                    return data;
                }
                return documentToPlayerData(doc);
            } catch (Exception e) {
                logger.warning("[KingdomCore] Failed to load player data for " + uuid + ": " + e.getMessage());
                return new PlayerData(uuid);
            }
        }, executor);
    }

    /**
     * Save a player's data to MongoDB asynchronously.
     */
    public CompletableFuture<Void> savePlayerData(PlayerData data) {
        return CompletableFuture.runAsync(() -> savePlayerDataSync(data), executor);
    }

    /**
     * Synchronous save — used internally.
     */
    private void savePlayerDataSync(PlayerData data) {
        try {
            Document doc = playerDataToDocument(data);
            playerCollection.replaceOne(
                    Filters.eq("uuid", data.getUuid().toString()),
                    doc,
                    new ReplaceOptions().upsert(true)
            );
        } catch (Exception e) {
            logger.warning("[KingdomCore] Failed to save player data for " + data.getUuid() + ": " + e.getMessage());
        }
    }

    /**
     * Sync player data directly from MongoDB (for web store purchases).
     */
    public CompletableFuture<Document> syncPlayerData(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return playerCollection.find(Filters.eq("uuid", uuid.toString())).first();
            } catch (Exception e) {
                logger.warning("[KingdomCore] Failed to sync player data for " + uuid + ": " + e.getMessage());
            }
            return null;
        }, executor);
    }

    /**
     * Start a real-time watch stream on the players collection.
     * This ensures that admin changes from the web panel are reflected in-game instantly.
     */
    public void startPlayerWatchStream(EconomyManager economyManager) {
        executor.submit(() -> {
            try {
                logger.info("[KingdomCore] Starting real-time MongoDB watch stream for player updates...");
                
                // Use UPDATE_LOOKUP so we get the full document even for partial $set updates
                playerCollection.watch()
                        .fullDocument(com.mongodb.client.model.changestream.FullDocument.UPDATE_LOOKUP)
                        .forEach(change -> {
                            Document fullDoc = change.getFullDocument();
                            if (fullDoc != null && fullDoc.containsKey("uuid")) {
                                try {
                                    UUID uuid = UUID.fromString(fullDoc.getString("uuid"));
                                    
                                    // Process the update in the game cache
                                    PlayerData updatedData = documentToPlayerData(fullDoc);
                                    
                                    // Update on main thread
                                    Bukkit.getScheduler().runTask(economyManager.getPlugin(), () -> {
                                        PlayerData localData = economyManager.getCache().get(uuid);
                                        if (localData != null) {
                                            // If we updated locally in the last 10 seconds, ignore the sync
                                            if (System.currentTimeMillis() - localData.getLastLocalUpdate() < 10000) {
                                                return;
                                            }
                                            
                                            // Merge fields instead of replacing the entire object
                                            PlayerData dbData = documentToPlayerData(fullDoc);
                                            localData.setShards(dbData.getShards());
                                            localData.setGems(dbData.getGems());
                                            localData.setXp(dbData.getXp());
                                            localData.setLevel(dbData.getLevel());
                                            localData.setBounty(dbData.getBounty());
                                            localData.setClassName(dbData.getClassName());
                                            localData.setKills(dbData.getKills());
                                            localData.setDeaths(dbData.getDeaths());
                                            localData.setLastKnownName(dbData.getLastKnownName());
                                            // Note: We don't update lastLocalUpdate here because this IS a sync update
                                        }
                                    });
                                } catch (IllegalArgumentException e) {
                                    // Skip invalid UUIDs
                                }
                            }
                        });
            } catch (com.mongodb.MongoCommandException e) {
                if (e.getErrorCode() == 40573 || e.getErrorMessage().contains("requires a replica set")) {
                    logger.warning("[KingdomCore] Real-time updates disabled: MongoDB is not a Replica Set.");
                } else {
                    logger.warning("[KingdomCore] MongoDB Watch Stream error: " + e.getMessage());
                }
            } catch (Exception e) {
                logger.warning("[KingdomCore] Unexpected error in Watch Stream: " + e.getMessage());
            }
        });
    }

    /**
     * Get the auction collection for use by the AuctionManager.
     */
    public MongoCollection<Document> getAuctionCollection() {
        return auctionCollection;
    }

    public MongoCollection<Document> getShopsCollection() {
        return shopsCollection;
    }

    public ExecutorService getExecutor() {
        return executor;
    }

    /**
     * Get the raw MongoDB database for web integration (verify, purchases).
     */
    public MongoDatabase getDatabase() {
        return database;
    }

    /**
     * Close the MongoDB connection.
     */
    public void close() {
        if (mongoClient != null) {
            mongoClient.close();
            logger.info("[KingdomCore] MongoDB connection closed.");
        }
        executor.shutdown();
    }

    // ---- Conversion Helpers ----

    private Document playerDataToDocument(PlayerData data) {
        // Convert class progress map to Document
        Document progressDoc = new Document();
        for (Map.Entry<String, PlayerData.ClassProgress> entry : data.getAllClassProgress().entrySet()) {
            progressDoc.append(entry.getKey(), new Document()
                    .append("level", entry.getValue().level)
                    .append("xp", entry.getValue().xp));
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
                .append("scoreboardEnabled", data.isScoreboardEnabled())
                .append("online", data.isOnline())
                .append("classProgress", progressDoc);
    }

    private PlayerData documentToPlayerData(Document doc) {
        PlayerData data = new PlayerData(
                UUID.fromString(doc.getString("uuid")),
                doc.getString("lastKnownName"),
                doc.getString("class"),
                doc.getInteger("shards", 0),
                doc.getInteger("gems", 0),
                doc.getInteger("xp", 0),
                doc.getInteger("level", 1),
                doc.getInteger("bounty", 0),
                doc.getInteger("kills", 0),
                doc.getInteger("deaths", 0),
                doc.getBoolean("scoreboardEnabled", true),
                doc.getBoolean("online", false)
        );

        // Load class progress
        Document progressDoc = doc.get("classProgress", Document.class);
        if (progressDoc != null) {
            Map<String, PlayerData.ClassProgress> progress = new HashMap<>();
            for (String key : progressDoc.keySet()) {
                Document entry = progressDoc.get(key, Document.class);
                if (entry != null) {
                    progress.put(key, new PlayerData.ClassProgress(
                            entry.getInteger("level", 1),
                            entry.getInteger("xp", 0)
                    ));
                }
            }
            data.setClassProgress(progress);
        }

        return data;
    }
}
