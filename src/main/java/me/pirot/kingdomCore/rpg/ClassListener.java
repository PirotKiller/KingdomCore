package me.pirot.kingdomCore.rpg;

import me.pirot.kingdomCore.config.ConfigManager;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * Listens for combat events and applies RPG class modifiers.
 */
public class ClassListener implements Listener {

    private final ConfigManager configManager;
    private final ClassManager classManager;
    private final WeaponManager weaponManager;

    public ClassListener(ConfigManager configManager, ClassManager classManager, WeaponManager weaponManager) {
        this.configManager = configManager;
        this.classManager = classManager;
        this.weaponManager = weaponManager;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // ---- ATTACKER MODIFIERS ----
        Player attacker = null;
        boolean isProjectile = false;

        if (event.getDamager() instanceof Player) {
            attacker = (Player) event.getDamager();
        } else if (event.getDamager() instanceof Projectile projectile) {
            if (projectile.getShooter() instanceof Player shooter) {
                attacker = shooter;
                isProjectile = true;
            }
        }

        if (attacker != null) {
            RPGClass attackerClass = classManager.getPlayerClass(attacker);
            if (attackerClass != null) {
                double damage = event.getDamage();

                // Apply weapon custom damage if holding a Kingdom weapon
                if (!isProjectile && weaponManager.isKingdomWeapon(attacker.getInventory().getItemInMainHand())) {
                    double weaponDamage = weaponManager.getWeaponDamage(attacker.getInventory().getItemInMainHand());
                    if (weaponDamage > 0) {
                        damage = weaponDamage;
                    }
                }

                switch (attackerClass) {
                    case ARCHER:
                        if (isProjectile) {
                            double projMultiplier = configManager.getClassMultiplier("archer", "projectile-damage-multiplier");
                            damage *= projMultiplier;
                        }
                        break;
                    case WIZARD:
                        if (isProjectile) {
                            double wizProjMultiplier = configManager.getClassMultiplier("wizard", "projectile-damage-multiplier");
                            damage *= wizProjMultiplier;
                        }
                        break;
                    case RONIN:
                        if (!isProjectile) {
                            double roninMeleeMultiplier = configManager.getClassMultiplier("ronin", "melee-damage-multiplier");
                            damage *= roninMeleeMultiplier;
                        }
                        break;
                    default:
                        break;
                }

                event.setDamage(damage);
            }
        }

        // ---- VICTIM MODIFIERS ----
        if (!(event.getEntity() instanceof Player victim)) return;

        RPGClass victimClass = classManager.getPlayerClass(victim);
        if (victimClass == null) return;

        double damage = event.getDamage();
        boolean victimHitByProjectile = event.getDamager() instanceof Projectile;

        switch (victimClass) {
            case ARCHER:
                // 1.5x melee damage taken (not projectiles)
                if (!victimHitByProjectile) {
                    double meleeTakenMult = configManager.getClassMultiplier("archer", "melee-damage-taken-multiplier");
                    damage *= meleeTakenMult;
                }
                break;
            case KNIGHT:
                // 80% melee damage reduction
                if (!victimHitByProjectile) {
                    double meleeReduction = configManager.getClassMultiplier("knight", "melee-damage-reduction");
                    damage *= (1.0 - meleeReduction);
                }
                break;
            case WIZARD:
                // 1.25x melee damage taken
                if (!victimHitByProjectile) {
                    double wizMeleeTaken = configManager.getClassMultiplier("wizard", "melee-damage-taken-multiplier");
                    damage *= wizMeleeTaken;
                }
                break;
            case RONIN:
                // 10% damage reduction on all damage
                double roninReduction = configManager.getClassMultiplier("ronin", "damage-reduction");
                damage *= (1.0 - roninReduction);
                break;
            default:
                break;
        }

        event.setDamage(damage);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        RPGClass rpgClass = classManager.getPlayerClass(player);
        if (rpgClass == null) return;

        switch (rpgClass) {
            case KNIGHT:
                // 25% blast resistance
                if (event.getCause() == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION ||
                        event.getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION) {
                    double blastResistance = configManager.getClassMultiplier("knight", "blast-resistance");
                    event.setDamage(event.getDamage() * (1.0 - blastResistance));
                }
                // 1.5x fall damage
                if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
                    double fallMultiplier = configManager.getClassMultiplier("knight", "fall-damage-multiplier");
                    event.setDamage(event.getDamage() * fallMultiplier);
                }
                break;
            default:
                break;
        }
    }
}
