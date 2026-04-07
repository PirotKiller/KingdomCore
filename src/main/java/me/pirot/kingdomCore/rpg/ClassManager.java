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

    public ClassManager(KingdomCore plugin, ConfigManager configManager, EconomyManager economyManager) {
        this.configManager = configManager;
        this.economyManager = economyManager;
    }

    /**
     * Set a player's RPG class. Removes old passives, applies new ones.
     */
    public void setClass(Player player, RPGClass rpgClass) {
        UUID uuid = player.getUniqueId();
        PlayerData data = economyManager.getPlayerData(uuid);
        if (data == null) return;

        // Remove old passives
        removePassives(player);

        // Set new class
        data.setClassName(rpgClass.name());

        // Apply new passives
        applyPassives(player);

        player.sendMessage("§a§l[Kingdom] §7You are now a " + rpgClass.getColoredName() + "§7!");
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
                double speedMultiplier = configManager.getClassMultiplier("rogue", "speed-multiplier");
                AttributeInstance speedAttr = player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
                if (speedAttr != null) {
                    // Default walking speed base value is 0.1
                    speedAttr.setBaseValue(0.1 * speedMultiplier);
                }
                // Permanent Feather Falling IV
                player.addPotionEffect(new PotionEffect(
                        PotionEffectType.SLOW_FALLING,
                        PotionEffect.INFINITE_DURATION,
                        0, // Feather Falling is handled through boots enchant, but we use slow falling as visual
                        true, false, false
                ));
                break;

            case KNIGHT:
                // Knight passives are reactive (handled in ClassListener), no persistent effects needed
                break;

            case ARCHER:
                // Archer passives are reactive (handled in ClassListener), no persistent effects needed
                break;

            case WIZARD:
                // Wizard passives are reactive (handled in ClassListener), no persistent effects needed
                break;

            case RONIN:
                // Ronin passives are reactive (handled in ClassListener), no persistent effects needed
                break;
        }
    }

    /**
     * Remove all passives from a player (used on class change or quit).
     */
    public void removePassives(Player player) {
        // Reset speed
        AttributeInstance speedAttr = player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
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
