package me.pirot.kingdomCore.database;

import java.util.UUID;

/**
 * POJO representing a player's persisted RPG state.
 * Directly maps to/from MongoDB documents.
 * Tracks level, XP, currencies, and combat stats.
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
    }

    // ---- Status Check ----

    /**
     * @return the XP required to reach the next level.
     * Formula based on screenshot analysis: Level * 100
     */
    public int getXpNeeded() {
        return Math.max(100, this.level * 100);
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

    public void addXp(int amount) {
        this.xp += amount;
        // Level up check
        while (this.xp >= getXpNeeded()) {
            this.xp -= getXpNeeded();
            this.level++;
        }
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
}
