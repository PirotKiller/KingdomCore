package me.pirot.kingdomCore.auction;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import me.pirot.kingdomCore.database.MongoManager;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

/**
 * Manages auction house listings backed by MongoDB.
 */
public class AuctionManager {

    private final JavaPlugin plugin;
    private final MongoManager mongoManager;
    private final Logger logger;
    private final MongoCollection<Document> collection;

    // In-memory cache of active listings
    private final List<AuctionListing> listings = new CopyOnWriteArrayList<>();

    public AuctionManager(JavaPlugin plugin, MongoManager mongoManager) {
        this.plugin = plugin;
        this.mongoManager = mongoManager;
        this.logger = plugin.getLogger();
        this.collection = mongoManager.getAuctionCollection();
        loadListings();
        startSyncTask();
    }

    /**
     * Load all active listings from MongoDB.
     */
    public void loadListings() {
        CompletableFuture.runAsync(() -> {
            try {
                List<AuctionListing> newListings = new ArrayList<>();
                for (Document doc : collection.find()) {
                    AuctionListing listing = documentToListing(doc);
                    if (listing != null && !listing.isClaimed()) {
                        newListings.add(listing);
                    }
                }
                listings.clear();
                listings.addAll(newListings);
            } catch (Exception e) {
                logger.warning("[KingdomCore] Failed to load auction listings: " + e.getMessage());
            }
        }, mongoManager.getExecutor());
    }

    /**
     * Start background sync task to refresh listings from MongoDB dynamically.
     * This allows webstore to make changes.
     */
    private void startSyncTask() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            loadListings();
        }, 200L, 200L); // Sync every 10 seconds (20 ticks * 10)
    }

    /**
     * Create a new listing with custom duration.
     */
    public CompletableFuture<Void> createListing(UUID seller, String sellerName, ItemStack item, int priceShards, int priceGems, long durationMillis) {
        String listingId = UUID.randomUUID().toString().substring(0, 8);
        long expireTime = System.currentTimeMillis() + durationMillis;

        AuctionListing listing = new AuctionListing(
                listingId, seller, sellerName,
                item.getType().name(), item.getAmount(),
                serializeItem(item),
                priceShards, priceGems, expireTime, false
        );

        listings.add(listing);

        return CompletableFuture.runAsync(() -> {
            try {
                collection.insertOne(listingToDocument(listing));
            } catch (Exception e) {
                logger.warning("[KingdomCore] Failed to save auction listing: " + e.getMessage());
            }
        }, mongoManager.getExecutor());
    }

    public CompletableFuture<Void> claimListing(String listingId) {
        listings.removeIf(l -> l.getListingId().equals(listingId));
        return CompletableFuture.runAsync(() -> {
            try {
                collection.deleteOne(Filters.eq("listingId", listingId));
            } catch (Exception e) {
                logger.warning("[KingdomCore] Failed to remove claimed auction: " + e.getMessage());
            }
        }, mongoManager.getExecutor());
    }

    public CompletableFuture<Void> removeListing(String listingId) {
        return claimListing(listingId);
    }

    public List<AuctionListing> getActiveListings() {
        return listings.stream()
                .filter(l -> !l.isExpired() && !l.isClaimed())
                .collect(java.util.stream.Collectors.toList());
    }

    public List<AuctionListing> getPlayerListings(UUID playerUuid) {
        return listings.stream()
                .filter(l -> l.getSeller().equals(playerUuid) && !l.isClaimed())
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Find a specific listing by its ID.
     */
    public AuctionListing getListing(String listingId) {
        if (listingId == null) return null;
        return listings.stream()
                .filter(l -> l.getListingId().equals(listingId))
                .findFirst().orElse(null);
    }

    public void purgeExpired() {
        // Stop deleting expired items immediately. 
        // We only purge items that are over 30 days old to avoid database bloat.
        long oldAge = System.currentTimeMillis() - (1000L * 60 * 60 * 24 * 30);
        CompletableFuture.runAsync(() -> {
            try {
                collection.deleteMany(Filters.lt("expireTime", oldAge));
            } catch (Exception e) {
                logger.warning("[KingdomCore] Failed to purge old auctions: " + e.getMessage());
            }
        }, mongoManager.getExecutor());
    }

    // ---- Serialization ----

    private String serializeItem(ItemStack item) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            BukkitObjectOutputStream oos = new BukkitObjectOutputStream(baos);
            oos.writeObject(item);
            oos.close();
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            logger.warning("[KingdomCore] Failed to serialize item: " + e.getMessage());
            return "";
        }
    }

    public ItemStack deserializeItem(String base64) {
        try {
            byte[] data = Base64.getDecoder().decode(base64);
            ByteArrayInputStream bais = new ByteArrayInputStream(data);
            BukkitObjectInputStream ois = new BukkitObjectInputStream(bais);
            ItemStack item = (ItemStack) ois.readObject();
            ois.close();
            return item;
        } catch (Exception e) {
            logger.warning("[KingdomCore] Failed to deserialize item: " + e.getMessage());
            return new ItemStack(Material.STONE);
        }
    }

    // ---- MongoDB conversions ----

    private Document listingToDocument(AuctionListing listing) {
        return new Document()
                .append("listingId", listing.getListingId())
                .append("seller", listing.getSeller().toString())
                .append("sellerName", listing.getSellerName())
                .append("materialName", listing.getMaterialName())
                .append("amount", listing.getAmount())
                .append("itemData", listing.getSerializedItem())
                .append("priceShards", listing.getPriceShards())
                .append("priceGems", listing.getPriceGems())
                .append("expireTime", listing.getExpireTime())
                .append("claimed", listing.isClaimed());
    }

    private AuctionListing documentToListing(Document doc) {
        try {
            return new AuctionListing(
                    doc.getString("listingId"),
                    UUID.fromString(doc.getString("seller")),
                    doc.getString("sellerName"),
                    doc.getString("materialName"),
                    doc.getInteger("amount", 1),
                    doc.getString("itemData"),
                    doc.getInteger("priceShards", 0),
                    doc.getInteger("priceGems", 0),
                    doc.getLong("expireTime"),
                    doc.getBoolean("claimed", false)
            );
        } catch (Exception e) {
            logger.warning("[KingdomCore] Failed to parse auction listing: " + e.getMessage());
            return null;
        }
    }

    /**
     * Represents a single auction listing.
     */
    public static class AuctionListing {
        private final String listingId;
        private final UUID seller;
        private final String sellerName;
        private final String materialName;
        private final int amount;
        private final String serializedItem;
        private final int priceShards;
        private final int priceGems;
        private final long expireTime;
        private final boolean claimed;

        public AuctionListing(String listingId, UUID seller, String sellerName,
                              String materialName, int amount, String serializedItem,
                              int priceShards, int priceGems, long expireTime, boolean claimed) {
            this.listingId = listingId;
            this.seller = seller;
            this.sellerName = sellerName;
            this.materialName = materialName;
            this.amount = amount;
            this.serializedItem = serializedItem;
            this.priceShards = priceShards;
            this.priceGems = priceGems;
            this.expireTime = expireTime;
            this.claimed = claimed;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > expireTime;
        }

        public boolean isClaimed() { return claimed; }
        public String getListingId() { return listingId; }
        public UUID getSeller() { return seller; }
        public String getSellerName() { return sellerName; }
        public String getMaterialName() { return materialName; }
        public int getAmount() { return amount; }
        public String getSerializedItem() { return serializedItem; }
        public int getPriceShards() { return priceShards; }
        public int getPriceGems() { return priceGems; }
        public long getExpireTime() { return expireTime; }
    }
}
