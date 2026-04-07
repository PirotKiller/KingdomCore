package me.pirot.kingdomCore.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

/**
 * Manages loading of config.yml and prices.yml.
 */
public class ConfigManager {

    private final JavaPlugin plugin;
    private FileConfiguration config;
    private java.util.Map<String, FileConfiguration> shopConfigs = new java.util.HashMap<>();

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Load both config files. Call from onEnable.
     */
    public void load() {
        // config.yml
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.config = plugin.getConfig();

        // Load all shops
        for (me.pirot.kingdomCore.shop.ShopType type : me.pirot.kingdomCore.shop.ShopType.values()) {
            String key = type.getConfigKey();
            File shopFile = new File(plugin.getDataFolder(), "shops/" + key + ".yml");
            if (!shopFile.exists()) {
                plugin.saveResource("shops/" + key + ".yml", false);
            }
            shopConfigs.put(key, YamlConfiguration.loadConfiguration(shopFile));
        }
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public FileConfiguration getShopConfig(String key) {
        return shopConfigs.get(key);
    }

    /**
     * Reload a single shop configuration from its file.
     */
    public void reloadShop(String key) {
        File shopFile = new File(plugin.getDataFolder(), "shops/" + key + ".yml");
        if (shopFile.exists()) {
            shopConfigs.put(key, YamlConfiguration.loadConfiguration(shopFile));
            plugin.getLogger().info("[KingdomCore] Automatically reloaded shop config: " + key + ".yml");
        }
    }

    // ---- Convenience getters for config.yml ----

    public String getMongoUri() {
        return config.getString("mongodb.uri", "mongodb://localhost:27017");
    }

    public String getMongoDatabase() {
        return config.getString("mongodb.database", "kingdomcore");
    }

    public String getMongoCollection() {
        return config.getString("mongodb.collection", "players");
    }

    public String getAuctionCollection() {
        return config.getString("mongodb.auction-collection", "auctions");
    }

    public int getGemSyncInterval() {
        return config.getInt("sync.gem-sync-interval-seconds", 60);
    }

    public double getClassMultiplier(String className, String key) {
        return config.getDouble("classes." + className.toLowerCase() + "." + key, 1.0);
    }

    public double getWeaponDamage(String tier) {
        return config.getDouble("weapons." + tier.toLowerCase() + ".damage", 6.0);
    }

    public double getWeaponSpeed(String tier) {
        return config.getDouble("weapons." + tier.toLowerCase() + ".speed", 1.4);
    }

    public int getMinBounty() {
        return config.getInt("bounty.min-amount", 50);
    }

    public int getCompassUpdateTicks() {
        return config.getInt("bounty.compass-update-ticks", 100);
    }

    public String getScoreboardTitle() {
        return config.getString("scoreboard.title", "§6§lTHE KINGDOM");
    }

    public int getScoreboardUpdateTicks() {
        return config.getInt("scoreboard.update-ticks", 40);
    }
}
