package me.pirot.kingdomCore.shop;

import com.mongodb.client.MongoCollection;
import me.pirot.kingdomCore.database.MongoManager;
import org.bson.Document;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Manages loading and caching of shop items from MongoDB.
 * Replaces the YAML-based shop configuration system.
 */
public class ShopDataManager {

    private final Logger logger;
    private final MongoManager mongoManager;

    // Cache: ShopType -> sorted list of active items
    private final Map<ShopType, List<ShopItemData>> cache = new ConcurrentHashMap<>();

    public ShopDataManager(Logger logger, MongoManager mongoManager) {
        this.logger = logger;
        this.mongoManager = mongoManager;
    }

    /**
     * Load all shop items from MongoDB into the cache.
     * Call from onEnable after MongoDB is connected.
     */
    public void loadAll() {
        cache.clear();

        // Pre-fill with empty lists
        for (ShopType type : ShopType.values()) {
            cache.put(type, new ArrayList<>());
        }

        try {
            MongoCollection<Document> shopsCol = mongoManager.getShopsCollection();
            if (shopsCol == null) {
                logger.warning("[KingdomCore] Shops collection not available. Shops will be empty.");
                return;
            }

            int total = 0;
            for (Document doc : shopsCol.find()) {
                String shopTypeName = doc.getString("shopType");
                if (shopTypeName == null) continue;

                ShopType type = ShopType.fromString(shopTypeName);
                if (type == null) continue;

                ShopItemData item = documentToShopItem(doc);
                if (item != null && item.isActive()) {
                    cache.get(type).add(item);
                    total++;
                }
            }

            // Sort each shop's items by order
            for (ShopType type : ShopType.values()) {
                cache.get(type).sort(Comparator.comparingInt(ShopItemData::getOrder));
            }

            logger.info("[KingdomCore] Loaded " + total + " shop items from MongoDB.");
        } catch (Exception e) {
            logger.severe("[KingdomCore] Failed to load shop items: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Get all active items for a shop type.
     */
    public List<ShopItemData> getItems(ShopType shopType) {
        return cache.getOrDefault(shopType, Collections.emptyList());
    }

    /**
     * Find a specific item by shopType and itemKey.
     */
    public ShopItemData findItem(ShopType shopType, String itemKey) {
        return getItems(shopType).stream()
                .filter(item -> item.getItemKey().equals(itemKey))
                .findFirst()
                .orElse(null);
    }

    /**
     * Reload all shop data from MongoDB.
     */
    public void reload() {
        loadAll();
    }

    private ShopItemData documentToShopItem(Document doc) {
        try {
            List<String> rawLore = doc.get("lore", List.class);
            List<String> lore = new ArrayList<>();
            if (rawLore != null) {
                for (Object line : rawLore) lore.add(String.valueOf(line));
            }

            // Safely handle potential nulls from MongoDB (BSON Document getters have 1 arg)
            String itemKey = doc.getString("itemKey");
            String name = doc.getString("name");
            String material = doc.getString("material");
            
            int amount = getInt(doc, "amount", 1);
            int priceShards = getInt(doc, "priceShards", 0);
            int priceGems = getInt(doc, "priceGems", 0);
            
            String enchant = doc.getString("enchant");
            int enchantLevel = getInt(doc, "enchantLevel", 0);
            
            double damage = getDouble(doc, "damage", 0.0);
            double speed = getDouble(doc, "speed", 0.0);

            String rpgClass = doc.getString("class");
            String tier = doc.getString("tier");
            
            int cmd = getInt(doc, "cmd", 0);
            int order = getInt(doc, "order", 0);
            
            Boolean activeVal = doc.getBoolean("active");
            boolean active = activeVal == null || activeVal; // Default to true if missing

            return new ShopItemData(
                    itemKey, name, material, amount, lore,
                    priceShards, priceGems, enchant, enchantLevel,
                    damage, speed, rpgClass, tier, cmd, order, active
            );
        } catch (Exception e) {
            logger.warning("[KingdomCore] Failed to parse shop item document: " + e.getMessage());
            logger.warning("[KingdomCore] Raw document: " + doc.toJson());
            e.printStackTrace();
            return null;
        }
    }

    private int getInt(Document doc, String key, int defaultValue) {
        Object val = doc.get(key);
        if (val instanceof Number num) {
            return num.intValue();
        }
        return defaultValue;
    }

    private double getDouble(Document doc, String key, double defaultValue) {
        Object val = doc.get(key);
        if (val instanceof Number num) {
            return num.doubleValue();
        }
        return defaultValue;
    }
}
