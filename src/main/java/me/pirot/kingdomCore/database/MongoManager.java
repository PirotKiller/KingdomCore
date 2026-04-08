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
            logger.info("[KingdomCore] Connected to MongoDB: " + dbName);
        } catch (Exception e) {
            logger.severe("[KingdomCore] Failed to connect to MongoDB: " + e.getMessage());
            e.printStackTrace();
        }
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
     * This requires a MongoDB Replica Set.
     */
    public void startPlayerWatchStream(EconomyManager economyManager) {
        executor.submit(() -> {
            try {
                logger.info("[KingdomCore] Attempting to start real-time MongoDB watch stream...");
                playerCollection.watch()
                        .forEach(change -> {
                            Document fullDoc = change.getFullDocument();
                            if (fullDoc != null && fullDoc.containsKey("uuid")) {
                                UUID uuid = UUID.fromString(fullDoc.getString("uuid"));
                                
                                // Only process if player is online (in cache)
                                if (economyManager.getPlayerData(uuid) != null) {
                                    PlayerData updatedData = documentToPlayerData(fullDoc);
                                    
                                    // Update on main thread to be safe
                                    Bukkit.getScheduler().runTask(economyManager.getPlugin(), () -> {
                                        economyManager.getCache().put(uuid, updatedData);
                                        // Optional: notify player or refresh scoreboard
                                    });
                                }
                            }
                        });
            } catch (com.mongodb.MongoCommandException e) {
                if (e.getErrorCode() == 40573 || e.getErrorMessage().contains("requires a replica set")) {
                    logger.warning("[KingdomCore] Real-time updates disabled: MongoDB is not a Replica Set. Falling back to 5s polling.");
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
                .append("online", data.isOnline());
    }

    private PlayerData documentToPlayerData(Document doc) {
        return new PlayerData(
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
    }
}
