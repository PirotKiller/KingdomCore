package me.pirot.kingdomCore.database;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * POJO representing a player's persisted RPG state.
 * Directly maps to/from MongoDB documents.
 * Tracks level, XP, currencies, combat stats, and per-class progress.
 */
public class PlayerData {

    private final UUID uuid;
    private String lastKnownName;
    private String className; // RPGClass name or "NONE"
    private int shards;
    private int gems;
    private int xp;
    private int level;
    private int bounty;
    private int kills;
    private int deaths;
    private boolean scoreboardEnabled;
    private boolean online;
    private long lastLocalUpdate; // Local timestamp of last in-game change

    // Per-class progress: stores level & XP for each class the player has played
    // Key = class name (e.g. "ARCHER"), Value = {level, xp}
    private Map<String, ClassProgress> classProgress;

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
        this.lastKnownName = "Unknown";
        this.className = "NONE";
        this.shards = 0;
        this.gems = 0;
        this.xp = 0;
        this.level = 1;
        this.bounty = 0;
        this.kills = 0;
        this.deaths = 0;
        this.scoreboardEnabled = true;
        this.online = false;
        this.lastLocalUpdate = 0;
        this.classProgress = new HashMap<>();
    }

    public PlayerData(UUID uuid, String lastKnownName, String className, int shards, int gems,
                      int xp, int level, int bounty, int kills, int deaths,
                      boolean scoreboardEnabled, boolean online) {
        this.uuid = uuid;
        this.lastKnownName = lastKnownName;
        this.className = className;
        this.shards = shards;
        this.gems = gems;
        this.xp = xp;
        this.level = level;
        this.bounty = bounty;
        this.kills = kills;
        this.deaths = deaths;
        this.scoreboardEnabled = scoreboardEnabled;
        this.online = online;
        this.lastLocalUpdate = 0;
        this.classProgress = new HashMap<>();
    }

    /**
     * Mark that a value was updated locally in-game.
     * Prevents sync-back from DB for a short period.
     */
    public void updateLocal() {
        this.lastLocalUpdate = System.currentTimeMillis();
    }

    public long getLastLocalUpdate() {
        return lastLocalUpdate;
    }

    // ---- XP Formula ----

    /**
     * Exponential XP formula matching the design doc:
     * Level 2 = 250 XP, Level 5 ≈ 3,750 XP, Level 10 ≈ 127,750 XP
     * Formula: 250 * 1.5^(level-2) for level >= 2
     */
    public int getXpNeeded() {
        if (this.level <= 1) return 250;
        return (int) (250 * Math.pow(1.5, this.level - 2));
    }

    /**
     * Get total XP needed from level 1 to reach a specific level.
     */
    public static int getTotalXpForLevel(int targetLevel) {
        int total = 0;
        for (int l = 1; l < targetLevel; l++) {
            if (l <= 1) total += 250;
            else total += (int) (250 * Math.pow(1.5, l - 2));
        }
        return total;
    }

    // ---- Per-Class Progress ----

    /**
     * Save current class progress before switching.
     */
    public void saveCurrentClassProgress() {
        if (className == null || className.equals("NONE")) return;
        classProgress.put(className, new ClassProgress(level, xp));
    }

    /**
     * Restore progress for a class (used with Data Restore Tome).
     * Returns true if progress was found and restored.
     */
    public boolean restoreClassProgress(String targetClass) {
        ClassProgress progress = classProgress.get(targetClass);
        if (progress == null) return false;
        this.level = progress.level;
        this.xp = progress.xp;
        return true;
    }

    /**
     * Get saved progress for a specific class.
     */
    public ClassProgress getClassProgress(String className) {
        return classProgress.get(className);
    }

    public Map<String, ClassProgress> getAllClassProgress() {
        return classProgress;
    }

    public void setClassProgress(Map<String, ClassProgress> progress) {
        this.classProgress = progress != null ? progress : new HashMap<>();
    }

    // ---- Getters & Setters ----

    public UUID getUuid() {
        return uuid;
    }

    public String getLastKnownName() {
        return lastKnownName;
    }

    public void setLastKnownName(String lastKnownName) {
        this.lastKnownName = lastKnownName;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public int getShards() {
        return shards;
    }

    public void setShards(int shards) {
        this.shards = shards;
    }

    public void addShards(int amount) {
        this.shards += amount;
    }

    public boolean removeShards(int amount) {
        if (this.shards >= amount) {
            this.shards -= amount;
            return true;
        }
        return false;
    }

    public int getGems() {
        return gems;
    }

    public void setGems(int gems) {
        this.gems = gems;
    }

    public void addGems(int amount) {
        this.gems += amount;
    }

    public boolean removeGems(int amount) {
        if (this.gems >= amount) {
            this.gems -= amount;
            return true;
        }
        return false;
    }

    public int getXp() {
        return xp;
    }

    public void setXp(int xp) {
        this.xp = xp;
    }

    public int addXp(int amount) {
        this.xp += amount;
        int levelsGained = 0;
        // Level up check
        while (this.xp >= getXpNeeded()) {
            this.xp -= getXpNeeded();
            this.level++;
            levelsGained++;
        }
        return levelsGained;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getBounty() {
        return bounty;
    }

    public void setBounty(int bounty) {
        this.bounty = bounty;
    }

    public void addBounty(int amount) {
        this.bounty += amount;
    }

    public int getKills() {
        return kills;
    }

    public void addKills(int amount) {
        this.kills += amount;
    }

    public void setKills(int kills) {
        this.kills = kills;
    }

    public int getDeaths() {
        return deaths;
    }

    public void addDeaths(int amount) {
        this.deaths += amount;
    }

    public void setDeaths(int deaths) {
        this.deaths = deaths;
    }

    public boolean isScoreboardEnabled() {
        return scoreboardEnabled;
    }

    public void setScoreboardEnabled(boolean scoreboardEnabled) {
        this.scoreboardEnabled = scoreboardEnabled;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    /**
     * Stores saved progress for a specific class.
     */
    public static class ClassProgress {
        public final int level;
        public final int xp;

        public ClassProgress(int level, int xp) {
            this.level = level;
            this.xp = xp;
        }
    }
}
