package me.pirot.kingdomCore.shop;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import me.pirot.kingdomCore.database.MongoManager;
import org.bson.Document;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.InputStream;
import java.io.InputStreamReader;
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
        // Use a temporary map to avoid "empty cache" windows during loading
        Map<ShopType, List<ShopItemData>> stagingCache = new HashMap<>();
        for (ShopType type : ShopType.values()) {
            stagingCache.put(type, new ArrayList<>());
        }

        try {
            MongoCollection<Document> shopsCol = mongoManager.getShopsCollection();
            if (shopsCol == null) {
                logger.warning("[KingdomCore] Shops collection not available. Shops will be empty.");
                return;
            }

            // --- AUTO BOOTSTRAP ---
            long count = shopsCol.countDocuments();
            if (count == 0) {
                logger.info("[KingdomCore] MongoDB Shops collection is empty. Performing auto-migration from YAML files...");
                // Note: migrateYAMLtoMongoDB is still safe to call here as it's typically startup or manual sync
                migrateYAMLtoMongoDB();
            }

            int total = 0;
            for (Document doc : shopsCol.find()) {
                String shopTypeName = doc.getString("shopType");
                if (shopTypeName == null) continue;

                ShopType type = ShopType.fromString(shopTypeName);
                if (type == null) continue;

                ShopItemData item = documentToShopItem(doc);
                if (item != null && item.isActive()) {
                    stagingCache.get(type).add(item);
                    total++;
                }
            }

            // Sort each shop's items by order
            for (ShopType type : ShopType.values()) {
                stagingCache.get(type).sort(Comparator.comparingInt(ShopItemData::getOrder));
            }

            // Atomic update of the main cache
            cache.clear();
            cache.putAll(stagingCache);

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

    /**
     * Migrates items from YAML files in resources to MongoDB.
     * This is a one-time or manual sync utility.
     */
    public void migrateYAMLtoMongoDB() {
        if (mongoManager.getShopsCollection() == null) {
            logger.warning("[KingdomCore] Cannot migrate: Shops collection is null.");
            return;
        }

        logger.info("[KingdomCore] Starting YAML to MongoDB migration...");
        int totalMigrated = 0;

        for (ShopType type : ShopType.values()) {
            String fileName = "shops/" + type.getConfigKey() + ".yml";
            InputStream is = getClass().getClassLoader().getResourceAsStream(fileName);
            
            if (is == null) {
                // Not every shop type might have a YAML file, skip if missing
                continue;
            }

            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(new InputStreamReader(is));
            ConfigurationSection itemsSection = yaml.getConfigurationSection("items");
            if (itemsSection == null) continue;

            for (String key : itemsSection.getKeys(false)) {
                ConfigurationSection itemSec = itemsSection.getConfigurationSection(key);
                if (itemSec == null) continue;

                Document doc = new Document()
                        .append("shopType", type.getConfigKey())
                        .append("itemKey", key)
                        .append("name", itemSec.getString("name"))
                        .append("material", itemSec.getString("material"))
                        .append("amount", itemSec.getInt("amount", 1))
                        .append("priceShards", itemSec.getInt("price-shards", itemSec.getInt("price", 0)))
                        .append("priceGems", itemSec.getInt("price-gems", 0))
                        .append("order", itemSec.getInt("order", 0))
                        .append("active", true);

                // Optional fields
                if (itemSec.contains("lore")) doc.append("lore", itemSec.getStringList("lore"));
                if (itemSec.contains("enchant")) {
                    doc.append("enchant", itemSec.getString("enchant"));
                    doc.append("enchantLevel", itemSec.getInt("enchant-level", 1));
                }
                if (itemSec.contains("potion-type")) {
                    doc.append("potionType", itemSec.getString("potion-type"));
                    doc.append("potionLevel", itemSec.getInt("potion-level", 1));
                    doc.append("potionDuration", itemSec.getInt("potion-duration", 120));
                }
                if (itemSec.contains("damage")) doc.append("damage", itemSec.getDouble("damage"));
                if (itemSec.contains("speed")) doc.append("speed", itemSec.getDouble("speed"));
                if (itemSec.contains("class")) doc.append("class", itemSec.getString("class"));
                if (itemSec.contains("tier")) doc.append("tier", itemSec.getString("tier"));
                if (itemSec.contains("cmd")) doc.append("cmd", itemSec.getInt("cmd"));

                // Upsert into MongoDB
                mongoManager.getShopsCollection().replaceOne(
                        Filters.and(
                                Filters.eq("shopType", type.getConfigKey()),
                                Filters.eq("itemKey", key)
                        ),
                        doc,
                        new ReplaceOptions().upsert(true)
                );
                totalMigrated++;
            }
        }

        logger.info("[KingdomCore] Migration complete! Total items synced to MongoDB: " + totalMigrated);
    }

    private ShopItemData documentToShopItem(Document doc) {
        try {
            List<String> rawLore = doc.get("lore", List.class);
            List<String> lore = new ArrayList<>();
            if (rawLore != null) {
                for (Object line : rawLore) lore.add(String.valueOf(line));
            }

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
            boolean active = activeVal == null || activeVal;

            return new ShopItemData(
                    itemKey, name, material, amount, lore,
                    priceShards, priceGems, enchant, enchantLevel,
                    damage, speed, rpgClass, tier, cmd, order, active
            );
        } catch (Exception e) {
            logger.warning("[KingdomCore] Failed to parse shop item document: " + e.getMessage());
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
