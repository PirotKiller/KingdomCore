package me.pirot.kingdomCore.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Manages loading of config.yml.
 * Shop data is now managed via MongoDB (ShopDataManager).
 */
public class ConfigManager {

    private final JavaPlugin plugin;
    private FileConfiguration config;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Load config.yml. Call from onEnable.
     */
    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }

    public FileConfiguration getConfig() {
        return config;
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

    // ---- XP Settings ----

    public int getXpMobKill() { return config.getInt("xp.mob-kill", 50); }
    public int getXpPlayerKill() { return config.getInt("xp.player-kill", 500); }
    public int getXpBossKill() { return config.getInt("xp.boss-kill", 500); }
    public boolean isClassSkillOnly() { return config.getBoolean("xp.class-skill-only", true); }

    // ---- Reset Settings ----

    public int getResetShardMinimum() { return config.getInt("reset.shard-minimum", 100); }
    public boolean isResetPreserveGems() { return config.getBoolean("reset.preserve-gems", true); }

    // ---- Bounty ----

    public int getDailySurvivalReward() { return config.getInt("bounty.daily-survival-reward", 100); }
}
