package me.pirot.kingdomCore.rpg;

import me.pirot.kingdomCore.KingdomCore;
import me.pirot.kingdomCore.config.ConfigManager;
import me.pirot.kingdomCore.database.PlayerData;
import me.pirot.kingdomCore.economy.EconomyManager;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.UUID;

/**
 * Manages RPG class assignment and passive effect application.
 */
public class ClassManager {

    private final ConfigManager configManager;
    private final EconomyManager economyManager;
    private final me.pirot.kingdomCore.database.MongoManager mongoManager;

    public ClassManager(KingdomCore plugin, ConfigManager configManager, EconomyManager economyManager) {
        this.configManager = configManager;
        this.economyManager = economyManager;
        this.mongoManager = plugin.getMongoManager();
    }

    /**
     * Set a player's RPG class. Saves old class progress, removes old passives, applies new ones.
     */
    public void setClass(Player player, RPGClass rpgClass) {
        UUID uuid = player.getUniqueId();
        PlayerData data = economyManager.getPlayerData(uuid);
        if (data == null) return;

        String oldClass = data.getClassName();

        // Save current class progress before switching
        data.saveCurrentClassProgress();

        // Remove old passives
        removePassives(player);

        // Set new class
        data.setClassName(rpgClass.name());

        // New class always starts at Level 1 by default.
        // Players must use a Data Restore Tome to retrieve previous progress.
        data.setLevel(1);
        data.setXp(0);
        data.updateLocal(); // Lock local data for 10s to prevent sync revert
        economyManager.savePlayer(uuid); // Sync to DB immediately

        PlayerData.ClassProgress saved = data.getClassProgress(rpgClass.name());
        if (saved != null && saved.level > 1) {
            player.sendMessage("§a§l[Kingdom] §7Saved progress found for this class (§eLevel " + saved.level + "§7).");
            player.sendMessage("§a§l[Kingdom] §7Use a §dData Restore Tome §7to restore it!");
        }

        // Apply new passives
        applyPassives(player);

        player.sendMessage("§a§l[Kingdom] §7You are now a " + rpgClass.getColoredName() + "§7!");

        // --- LOGGING ---
        org.bson.Document log = new org.bson.Document("timestamp", new java.util.Date())
                .append("source", "GAME")
                .append("type", "CLASS_CHANGE")
                .append("player", new org.bson.Document("uuid", uuid.toString())
                        .append("name", player.getName()))
                .append("summary", "Switched class from " + oldClass + " to " + rpgClass.name())
                .append("metadata", new org.bson.Document("oldClass", oldClass)
                        .append("newClass", rpgClass.name()));
        mongoManager.logAction(log);
    }

    /**
     * Apply passive effects based on the player's current class.
     */
    public void applyPassives(Player player) {
        PlayerData data = economyManager.getPlayerData(player.getUniqueId());
        if (data == null) return;

        RPGClass rpgClass = RPGClass.fromString(data.getClassName());
        if (rpgClass == null) return;

        switch (rpgClass) {
            case ROGUE:
                // 1.25x speed
                double rogueSpeed = configManager.getClassMultiplier("rogue", "speed-multiplier");
                AttributeInstance rogueSpeedAttr = player.getAttribute(Attribute.MOVEMENT_SPEED);
                if (rogueSpeedAttr != null) {
                    rogueSpeedAttr.setBaseValue(0.1 * rogueSpeed);
                }
                // Permanent Feather Falling IV effect
                player.addPotionEffect(new PotionEffect(
                        PotionEffectType.SLOW_FALLING,
                        PotionEffect.INFINITE_DURATION,
                        0, true, false, false
                ));
                break;

            case RONIN:
                // 1.25x speed (same as Rogue)
                double roninSpeed = 1.25;
                AttributeInstance roninSpeedAttr = player.getAttribute(Attribute.MOVEMENT_SPEED);
                if (roninSpeedAttr != null) {
                    roninSpeedAttr.setBaseValue(0.1 * roninSpeed);
                }
                break;

            case KNIGHT:
            case ARCHER:
            case WIZARD:
                // Reactive passives handled in ClassListener/ClassAbilityListener
                break;
        }
    }

    /**
     * Remove all passives from a player (used on class change or quit).
     */
    public void removePassives(Player player) {
        // Reset speed
        AttributeInstance speedAttr = player.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speedAttr != null) {
            speedAttr.setBaseValue(0.1); // Default
        }

        // Remove potion effects
        player.removePotionEffect(PotionEffectType.SLOW_FALLING);
    }

    /**
     * Get the current RPGClass for a player. May return null.
     */
    public RPGClass getPlayerClass(Player player) {
        PlayerData data = economyManager.getPlayerData(player.getUniqueId());
        if (data == null) return null;
        return RPGClass.fromString(data.getClassName());
    }
}
