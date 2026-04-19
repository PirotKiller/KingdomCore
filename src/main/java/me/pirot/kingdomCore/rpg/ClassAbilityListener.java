package me.pirot.kingdomCore.rpg;

import me.pirot.kingdomCore.KingdomCore;
import me.pirot.kingdomCore.config.ConfigManager;
import me.pirot.kingdomCore.database.PlayerData;
import me.pirot.kingdomCore.economy.EconomyManager;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles all active class abilities, XP awards, and class-specific restrictions.
 * Separated from ClassListener (which handles passive combat modifiers).
 */
public class ClassAbilityListener implements Listener {

    private final KingdomCore plugin;
    private final ClassManager classManager;
    private final ConfigManager configManager;
    private final EconomyManager economyManager;
    private final WeaponManager weaponManager;

    // Cooldown tracking: playerUUID -> ability name -> last use time (ms)
    private final Map<UUID, Map<String, Long>> cooldowns = new ConcurrentHashMap<>();

    // Rogue invisibility state
    private final Set<UUID> invisiblePlayers = ConcurrentHashMap.newKeySet();

    public ClassAbilityListener(KingdomCore plugin, ClassManager classManager,
                                ConfigManager configManager, EconomyManager economyManager,
                                WeaponManager weaponManager) {
        this.plugin = plugin;
        this.classManager = classManager;
        this.configManager = configManager;
        this.economyManager = economyManager;
        this.weaponManager = weaponManager;
    }

    // ============================================================
    // XP AWARDS
    // ============================================================

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityKillForXP(EntityDeathEvent event) {
        LivingEntity victim = event.getEntity();
        Player killer = victim.getKiller();
        if (killer == null) return;

        RPGClass rpgClass = classManager.getPlayerClass(killer);
        if (rpgClass == null) return;

        int xpAmount = 0;

        if (victim instanceof Player) {
            xpAmount = configManager.getXpPlayerKill();
        } else if (victim instanceof Boss || isWarden(victim) || isElderGuardian(victim)) {
            xpAmount = configManager.getXpBossKill();
        } else {
            xpAmount = configManager.getXpMobKill();
        }

        if (xpAmount > 0) {
            PlayerData data = economyManager.getPlayerData(killer.getUniqueId());
            if (data != null) {
                int oldLevel = data.getLevel();
                data.addXp(xpAmount);

                // Action bar notification
                killer.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                        new TextComponent("§a+" + xpAmount + " XP §7(" + data.getXp() + "/" + data.getXpNeeded() + ")"));

                // Level up notification
                if (data.getLevel() > oldLevel) {
                    killer.sendMessage("");
                    killer.sendMessage("§6§l✦ §e§lLEVEL UP! §6§l✦");
                    killer.sendMessage("§7You are now §eLevel " + data.getLevel() + "§7!");
                    killer.sendMessage("");
                    killer.playSound(killer.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);

                    // Particle effects
                    killer.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, killer.getLocation().add(0, 1, 0), 50, 0.5, 1, 0.5, 0.1);
                }
            }
        }
    }

    private boolean isWarden(LivingEntity entity) {
        return entity.getType().name().equals("WARDEN");
    }

    private boolean isElderGuardian(LivingEntity entity) {
        return entity instanceof ElderGuardian;
    }

    // ============================================================
    // ROGUE: Invisibility on Crouch
    // ============================================================

    @EventHandler
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        RPGClass rpgClass = classManager.getPlayerClass(player);
        if (rpgClass != RPGClass.ROGUE) return;

        if (event.isSneaking()) {
            // Go invisible
            player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY,
                    PotionEffect.INFINITE_DURATION, 0, false, false, false));
            invisiblePlayers.add(player.getUniqueId());

            // Hide armor visually (particles)
            player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation().add(0, 1, 0), 10, 0.3, 0.5, 0.3, 0.01);
        } else {
            // Remove invisible
            player.removePotionEffect(PotionEffectType.INVISIBILITY);
            invisiblePlayers.remove(player.getUniqueId());
        }
    }

    /**
     * Rogue: 2x damage when invisible (always crits).
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onRogueDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        RPGClass rpgClass = classManager.getPlayerClass(attacker);
        if (rpgClass != RPGClass.ROGUE) return;

        if (invisiblePlayers.contains(attacker.getUniqueId())) {
            event.setDamage(event.getDamage() * 2.0);

            // Remove invisibility after attack
            attacker.removePotionEffect(PotionEffectType.INVISIBILITY);
            invisiblePlayers.remove(attacker.getUniqueId());
        }
    }

    /**
     * Rogue: Loses hunger faster (1.25x exhaustion while sprinting).
     */
    @EventHandler
    public void onRogueExhaustion(EntityExhaustionEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        RPGClass rpgClass = classManager.getPlayerClass(player);
        if (rpgClass != RPGClass.ROGUE) return;

        if (player.isSprinting()) {
            event.setExhaustion(event.getExhaustion() * 1.25f);
        }
    }

    /**
     * Rogue: Gains half hunger from food.
     */
    @EventHandler
    public void onRogueEat(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        RPGClass rpgClass = classManager.getPlayerClass(player);
        if (rpgClass != RPGClass.ROGUE) return;

        int gained = event.getFoodLevel() - player.getFoodLevel();
        if (gained > 0) {
            event.setFoodLevel(player.getFoodLevel() + (gained / 2));
        }
    }

    // ============================================================
    // KNIGHT: Ender Pearl Death + AOE Shield
    // ============================================================

    /**
     * Knight: Ender Pearls kill the player.
     */
    @EventHandler
    public void onKnightEnderPearl(PlayerTeleportEvent event) {
        if (event.getCause() != PlayerTeleportEvent.TeleportCause.ENDER_PEARL) return;
        Player player = event.getPlayer();
        RPGClass rpgClass = classManager.getPlayerClass(player);
        if (rpgClass != RPGClass.KNIGHT) return;

        event.setCancelled(true);
        player.setHealth(0);
        player.sendMessage("§c§l[Kingdom] §7Knights cannot use Ender Pearls!");
    }

    /**
     * Knight: Defensive AOE shield on right-click (final level only).
     */
    @EventHandler
    public void onKnightShieldAbility(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        RPGClass rpgClass = classManager.getPlayerClass(player);
        if (rpgClass != RPGClass.KNIGHT) return;

        PlayerData data = economyManager.getPlayerData(player.getUniqueId());
        if (data == null || data.getLevel() < 10) return;

        ItemStack held = player.getInventory().getItemInMainHand();
        if (!weaponManager.isKingdomWeapon(held)) return;
        String tier = held.getItemMeta().getPersistentDataContainer()
                .get(weaponManager.TIER_KEY, org.bukkit.persistence.PersistentDataType.STRING);
        if (!"CHAOS".equals(tier)) return;

        if (isOnCooldown(player, "knight_aoe", 15000)) return;
        setCooldown(player, "knight_aoe");

        // Create AOE defensive zone
        Location center = player.getLocation();
        player.sendMessage("§b§l[Kingdom] §7Defensive AOE activated!");
        player.playSound(center, Sound.ITEM_TOTEM_USE, 1f, 1.5f);

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= 100) { // 5 seconds
                    this.cancel();
                    return;
                }
                // Shield particles in a circle
                double radius = 4.0;
                for (int i = 0; i < 36; i++) {
                    double angle = Math.toRadians(i * 10 + ticks * 5);
                    double x = center.getX() + radius * Math.cos(angle);
                    double z = center.getZ() + radius * Math.sin(angle);
                    center.getWorld().spawnParticle(Particle.END_ROD, x, center.getY() + 0.5, z, 1, 0, 0, 0, 0);
                }

                // Push away enemies
                for (Entity entity : center.getWorld().getNearbyEntities(center, radius, 3, radius)) {
                    if (entity instanceof LivingEntity && entity != player) {
                        Vector push = entity.getLocation().toVector().subtract(center.toVector()).normalize().multiply(0.5);
                        push.setY(0.2);
                        entity.setVelocity(push);
                    }
                }

                ticks += 2;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    // ============================================================
    // WIZARD: Potion Duration, Fall Damage, Enchanting, Fireball Staff
    // ============================================================

    /**
     * Wizard: Potion effects last 2x longer.
     */
    @EventHandler
    public void onWizardPotionEffect(EntityPotionEffectEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        RPGClass rpgClass = classManager.getPlayerClass(player);
        if (rpgClass != RPGClass.WIZARD) return;

        if (event.getAction() == EntityPotionEffectEvent.Action.ADDED && event.getNewEffect() != null) {
            PotionEffect effect = event.getNewEffect();
            // Don't double our own applied effects
            if (effect.getDuration() < PotionEffect.INFINITE_DURATION / 2) {
                PotionEffect doubled = new PotionEffect(
                        effect.getType(), effect.getDuration() * 2,
                        effect.getAmplifier(), effect.isAmbient(),
                        effect.hasParticles(), effect.hasIcon());
                event.setCancelled(true);
                Bukkit.getScheduler().runTask(plugin, () -> player.addPotionEffect(doubled));
            }
        }
    }

    /**
     * Wizard: Half fall damage.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onWizardFallDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) return;
        RPGClass rpgClass = classManager.getPlayerClass(player);
        if (rpgClass != RPGClass.WIZARD) return;

        event.setDamage(event.getDamage() * 0.5);
    }

    /**
     * Wizard: Right-click staff to shoot fireball.
     */
    @EventHandler
    public void onWizardFireball(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        RPGClass rpgClass = classManager.getPlayerClass(player);
        if (rpgClass != RPGClass.WIZARD) return;

        ItemStack held = player.getInventory().getItemInMainHand();
        if (!weaponManager.isKingdomWeapon(held)) return;

        String classKey = held.getItemMeta().getPersistentDataContainer()
                .get(weaponManager.CLASS_KEY, org.bukkit.persistence.PersistentDataType.STRING);
        if (!"WIZARD".equals(classKey)) return;

        PlayerData data = economyManager.getPlayerData(player.getUniqueId());
        int level = data != null ? data.getLevel() : 1;

        // Cooldown scales with level: 3s at level 1, 1s at level 10
        long cooldownMs = Math.max(1000, 3000 - (level * 200L));
        if (isOnCooldown(player, "wizard_fireball", cooldownMs)) return;
        setCooldown(player, "wizard_fireball");

        // Shoot fireball
        SmallFireball fireball = player.launchProjectile(SmallFireball.class);
        fireball.setDirection(player.getLocation().getDirection().multiply(1.5));
        fireball.setIsIncendiary(false);
        fireball.setYield(0); // No block damage

        // Scale damage with level
        double baseDamage = 4.0 + (level * 0.5);
        fireball.setMetadata("wizard_damage", new FixedMetadataValue(plugin, baseDamage));

        player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 0.8f, 1.2f);
        player.getWorld().spawnParticle(Particle.FLAME, player.getLocation().add(0, 1, 0), 10, 0.2, 0.2, 0.2, 0.05);

        event.setCancelled(true);
    }

    /**
     * Handle wizard fireball damage.
     */
    @EventHandler
    public void onFireballHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof SmallFireball fireball)) return;
        if (!fireball.hasMetadata("wizard_damage")) return;

        double damage = fireball.getMetadata("wizard_damage").get(0).asDouble();
        event.setDamage(damage);
    }

    // ============================================================
    // RONIN: Speed, Hunger, Dash, Shield Block, Magic Weakness
    // ============================================================

    /**
     * Ronin: Cannot use shields — prevent equipping.
     */
    @EventHandler
    public void onRoninShieldEquip(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        RPGClass rpgClass = classManager.getPlayerClass(player);
        if (rpgClass != RPGClass.RONIN) return;

        // Off-hand slot = 40
        if (event.getSlot() == 40 && event.getCurrentItem() != null) {
            ItemStack cursor = event.getCursor();
            if (cursor != null && cursor.getType() == Material.SHIELD) {
                event.setCancelled(true);
                player.sendMessage("§c§l[Kingdom] §7Ronin cannot use shields!");
            }
        }
    }

    /**
     * Ronin: Reduced hunger (half exhaustion).
     */
    @EventHandler
    public void onRoninExhaustion(EntityExhaustionEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        RPGClass rpgClass = classManager.getPlayerClass(player);
        if (rpgClass != RPGClass.RONIN) return;

        event.setExhaustion(event.getExhaustion() * 0.5f);
    }

    /**
     * Ronin: Chargeable Dash Attack — right-click with katana.
     * Bypasses shield blocking.
     */
    @EventHandler
    public void onRoninDash(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        RPGClass rpgClass = classManager.getPlayerClass(player);
        if (rpgClass != RPGClass.RONIN) return;

        ItemStack held = player.getInventory().getItemInMainHand();
        if (!weaponManager.isKingdomWeapon(held)) return;

        String classKey = held.getItemMeta().getPersistentDataContainer()
                .get(weaponManager.CLASS_KEY, org.bukkit.persistence.PersistentDataType.STRING);
        if (!"RONIN".equals(classKey)) return;

        if (isOnCooldown(player, "ronin_dash", 5000)) return;
        setCooldown(player, "ronin_dash");

        // Dash forward
        Vector direction = player.getLocation().getDirection().normalize().multiply(2.0);
        direction.setY(0.3);
        player.setVelocity(direction);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1.5f);
        player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, player.getLocation().add(0, 1, 0), 5, 0.5, 0.3, 0.5, 0);

        // Damage entities along the dash path
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            double damage = weaponManager.getWeaponDamage(held) * 1.2;
            for (Entity entity : player.getNearbyEntities(3, 2, 3)) {
                if (entity instanceof LivingEntity target && target != player) {
                    // Bypass shield by dealing damage directly
                    if (target instanceof Player targetPlayer) {
                        targetPlayer.setShieldBlockingDelay(20); // Disable shield briefly
                    }
                    target.damage(damage, player);
                    target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0), 15, 0.3, 0.3, 0.3, 0.1);
                }
            }
        }, 5L);

        event.setCancelled(true);
    }

    /**
     * Ronin: 1.5x damage from magic and potions.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onRoninMagicDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        RPGClass rpgClass = classManager.getPlayerClass(player);
        if (rpgClass != RPGClass.RONIN) return;

        if (event.getCause() == EntityDamageEvent.DamageCause.MAGIC ||
                event.getCause() == EntityDamageEvent.DamageCause.POISON ||
                event.getCause() == EntityDamageEvent.DamageCause.WITHER) {
            event.setDamage(event.getDamage() * 1.5);
        }
    }

    // ============================================================
    // ARCHER: Bow Range & Infinity at Max Level
    // ============================================================

    /**
     * Archer: Faster draw + further shooting distance.
     */
    @EventHandler
    public void onArcherBowShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        RPGClass rpgClass = classManager.getPlayerClass(player);
        if (rpgClass != RPGClass.ARCHER) return;

        // Increase projectile velocity for further range
        if (event.getProjectile() instanceof Arrow arrow) {
            arrow.setVelocity(arrow.getVelocity().multiply(1.3));
        }
    }

    // ============================================================
    // RONIN: Apply speed on join/class set
    // ============================================================

    /**
     * Apply passive speed to Ronin on join (handled by ClassManager.applyPassives).
     * This is for completeness — the ClassManager already handles it.
     */

    // ============================================================
    // COOLDOWN UTILITIES
    // ============================================================

    private boolean isOnCooldown(Player player, String ability, long cooldownMs) {
        Map<String, Long> playerCooldowns = cooldowns.get(player.getUniqueId());
        if (playerCooldowns == null) return false;

        Long lastUse = playerCooldowns.get(ability);
        if (lastUse == null) return false;

        long elapsed = System.currentTimeMillis() - lastUse;
        if (elapsed < cooldownMs) {
            double remaining = (cooldownMs - elapsed) / 1000.0;
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                    new TextComponent("§c§lCooldown: §f" + String.format("%.1fs", remaining)));
            return true;
        }
        return false;
    }

    private void setCooldown(Player player, String ability) {
        cooldowns.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>())
                .put(ability, System.currentTimeMillis());
    }

    /**
     * Clean up when player quits.
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        cooldowns.remove(uuid);
        invisiblePlayers.remove(uuid);
    }
}
