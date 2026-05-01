package me.pirot.kingdomCore;

import me.pirot.kingdomCore.database.PlayerData;
import me.pirot.kingdomCore.economy.EconomyManager;
import me.pirot.kingdomCore.rpg.SpecialItems;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Sound;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

import me.pirot.kingdomCore.rpg.WeaponManager;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Tracks player combat statistics (kills and deaths) in real-time.
 * Manages Shard item drops from mobs, bosses, and player deaths.
 */
public class CombatListener implements Listener {

    private final EconomyManager economyManager;
    private final SpecialItems specialItems;
    private final WeaponManager weaponManager;

    public CombatListener(EconomyManager economyManager, SpecialItems specialItems, WeaponManager weaponManager) {
        this.economyManager = economyManager;
        this.specialItems = specialItems;
        this.weaponManager = weaponManager;
    }

    /**
     * Intercept damage to apply custom RPG weapon stats.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onWeaponDamage(EntityDamageByEntityEvent event) {
        Player damager = null;

        // 1. Melee detection (handled by Attributes mostly, but we can fine-tune here if needed)
        if (event.getDamager() instanceof Player p) {
            damager = p;
            // Note: Melee attributes handle standard hits, we don't need to override event damage
            // unless we want to add extra RPG logic like crit multipliers from levels.
        }

        // 2. Projectile detection (Archer bows)
        if (event.getDamager() instanceof Projectile proj && proj.getShooter() instanceof Player p) {
            damager = p;
            ItemStack mainHand = p.getInventory().getItemInMainHand();
            
            if (weaponManager.isKingdomWeapon(mainHand)) {
                double customDamage = weaponManager.getWeaponDamage(mainHand);
                if (customDamage > 0) {
                    // We set the damage to the bow's stat value.
                    // Vanilla damage is replaced by the RPG damage.
                    event.setDamage(customDamage);
                }
            }
        }
    }

    /**
     * Handle K/D tracking and player death shard penalty.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        // Track death and calculate shards
        PlayerData victimData = economyManager.getPlayerData(victim.getUniqueId());
        if (victimData != null) {
            economyManager.addDeath(victim.getUniqueId());

            // Calculate 5% shard loss
            int currentShards = victimData.getShards();
            if (currentShards > 0) {
                int dropAmount = (int) (currentShards * 0.05);
                if (dropAmount > 0) {
                    // Use economyManager to ensure sync-safe subtraction and immediate save
                    economyManager.removeShards(victim.getUniqueId(), dropAmount);
                    victim.sendMessage("§c§l[Death Penalty] §7You lost §a" + dropAmount + " Shards§7!");
                    // Drop the shards as physical item
                    event.getDrops().add(specialItems.createPhysicalShard(dropAmount));
                }
            }
        }

        // 15% chance to drop a Soul Fragment on any death
        if (ThreadLocalRandom.current().nextInt(100) < 15) {
            event.getDrops().add(specialItems.createSoulItem());
            victim.sendMessage("§d§l[Death] §7A §b✦ Soul Fragment §7slipped from your grasp!");
        }

        // Track kill for killer
        if (killer != null && !killer.equals(victim)) {
            // Using EconomyManager adds to cache + saves to DB
            economyManager.addKill(killer.getUniqueId());
        }
    }

    /**
     * Handle mob and boss shard drops.
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        int dropAmount = 0;
        int xpAmount = 0;

        if (isBoss(entity)) {
            dropAmount = ThreadLocalRandom.current().nextInt(1000, 5001); // 1K - 5K shards
            xpAmount = ThreadLocalRandom.current().nextInt(250, 1001);   // 250 - 1K XP
        } else if (isHostileMob(entity)) {
            dropAmount = ThreadLocalRandom.current().nextInt(5, 26);     // 5 - 25 shards
            xpAmount = ThreadLocalRandom.current().nextInt(10, 26);      // 10 - 25 XP
        }

        if (dropAmount > 0) {
            event.getDrops().add(specialItems.createPhysicalShard(dropAmount));
        }

        // ---- XP Gain ----
        Player killer = entity.getKiller();
        if (killer != null && xpAmount > 0) {
            economyManager.addXp(killer.getUniqueId(), xpAmount);
            killer.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                    new TextComponent("§a§l+ " + xpAmount + " XP"));
        }
    }

    /**
     * Handle player picking up physical shards.
     */
    @EventHandler
    public void onPickupShard(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        org.bukkit.inventory.ItemStack item = event.getItem().getItemStack();
        if (specialItems.isSpecialItem(item)) {
            String id = specialItems.getSpecialItemId(item);
            if (SpecialItems.PHYSICAL_SHARD_ID.equals(id)) {
                event.setCancelled(true);
                event.getItem().remove();

                int value = specialItems.getPhysicalShardValue(item);
                if (value > 0) {
                    economyManager.addShards(player.getUniqueId(), value);
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                            new TextComponent("§e§l+ " + value + " Shards"));
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.2f);
                }
            }
        }
    }

    private boolean isBoss(LivingEntity entity) {
        return entity instanceof Wither || 
               entity instanceof EnderDragon || 
               entity instanceof ElderGuardian || 
               entity instanceof Warden;
    }

    private boolean isHostileMob(LivingEntity entity) {
        return entity instanceof Monster || entity instanceof Slime || entity instanceof MagmaCube;
    }
}
