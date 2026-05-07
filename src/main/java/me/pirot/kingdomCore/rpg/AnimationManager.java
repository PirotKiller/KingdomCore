package me.pirot.kingdomCore.rpg;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.Pair;
import me.pirot.kingdomCore.KingdomCore;
import me.pirot.kingdomCore.database.PlayerData;
import me.pirot.kingdomCore.economy.EconomyManager;
import org.bukkit.*;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * High-fidelity rendering engine for Class Special Abilities.
 * Uses ItemDisplay entities and ProtocolLib for smooth, packet-based animations.
 *
 * Each class has a completely unique animation style:
 *   Knight  - Colossal overhead smash with ground crater
 *   Ronin   - Lightning-fast horizontal crescent sweep
 *   Rogue   - Backstab lunge from brief invisibility
 *   Archer  - Vibrating bow draw then tornado arrow volley
 *   Wizard  - Staff raise casting phase then meteor impact
 */
public class AnimationManager {

    private final KingdomCore plugin;
    private final EconomyManager economyManager;
    private final ProtocolManager protocolManager;

    // Safety net: track active displays so we can always clean up
    private final Map<UUID, List<ItemDisplay>> activeDisplays = new ConcurrentHashMap<>();

    public AnimationManager(KingdomCore plugin, EconomyManager economyManager) {
        this.plugin = plugin;
        this.economyManager = economyManager;
        this.protocolManager = ProtocolLibrary.getProtocolManager();
    }

    // ============================================================
    // COOLDOWN CALCULATOR (Level-Based)
    // ============================================================

    /**
     * Calculate a cooldown in milliseconds that decreases with player level.
     * @param baseCooldownMs  The cooldown at level 1
     * @param minCooldownMs   The absolute minimum cooldown
     * @param level           The player's RPG level
     * @return Cooldown in milliseconds
     */
    public long calculateCooldown(long baseCooldownMs, long minCooldownMs, int level) {
        // Each level reduces cooldown by 500ms, capped at minimum
        long reduction = (level - 1) * 500L;
        return Math.max(minCooldownMs, baseCooldownMs - reduction);
    }

    // ============================================================
    // KNIGHT: Colossal Smash
    // ============================================================

    public void playKnightAbility(Player player, ItemStack weapon, WeaponTier tier) {
        PlayerData data = economyManager.getPlayerData(player.getUniqueId());
        int level = data != null ? data.getLevel() : 1;
        float intensity = 1.0f + (level / 50.0f);

        // Detect weapon hand slot to avoid duplication/switching bugs
        EnumWrappers.ItemSlot originalSlot = getHandOfWeapon(player, weapon);

        // Hide real weapon via packets
        updateVisualEquipment(player, new ItemStack(Material.AIR), originalSlot);

        Location spawnLoc = player.getEyeLocation().add(0, 1, 0);
        ItemDisplay display = spawnSafeDisplay(player, spawnLoc, weapon);
        display.setInterpolationDuration(10);
        display.setInterpolationDelay(0);

        // Phase 1: MASSIVE WIND UP — weapon scales to 2x and tilts back 90°
        display.setTransformation(new Transformation(
                new Vector3f(0, 0.5f, 0),
                new Quaternionf().rotationX((float) Math.toRadians(-90)),
                new Vector3f(2.0f * intensity, 2.0f * intensity, 2.0f * intensity),
                new Quaternionf()
        ));

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) { cleanupAll(player, weapon, originalSlot); return; }

                // Phase 2: CRUSHING SLAM — 5 ticks, fast and heavy
                display.setInterpolationDuration(5);
                display.setTransformation(new Transformation(
                        new Vector3f(0, -1.5f, 0.8f),
                        new Quaternionf().rotationX((float) Math.toRadians(110)),
                        new Vector3f(2.0f * intensity, 2.0f * intensity, 2.0f * intensity),
                        new Quaternionf()
                ));
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.2f, 0.5f);

                // Phase 3: Impact crater + shockwave + flying blocks
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        Location impact = player.getLocation().add(
                                player.getLocation().getDirection().setY(0).normalize().multiply(1.5));
                        World w = impact.getWorld();
                        w.spawnParticle(Particle.EXPLOSION, impact, (int)(5 * intensity), 1, 0.5, 1, 0.1);
                        w.spawnParticle(Particle.LARGE_SMOKE, impact, (int)(20 * intensity), 1.5, 0.2, 1.5, 0.05);
                        w.playSound(impact, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 1f, 0.8f);

                        // Launch flying blocks into the air (visual only — never place)
                        Material[] debrisMaterials = {Material.DIRT, Material.COBBLESTONE, Material.GRAVEL, Material.STONE};
                        int blockCount = (int)(4 * intensity);
                        for (int i = 0; i < blockCount; i++) {
                            Material mat = debrisMaterials[i % debrisMaterials.length];
                            FallingBlock fb = w.spawnFallingBlock(impact.clone().add(
                                    Math.random() * 2 - 1, 0.5, Math.random() * 2 - 1), mat.createBlockData());
                            fb.setDropItem(false);
                            fb.setHurtEntities(false);
                            fb.setCancelDrop(true);
                            double vx = (Math.random() - 0.5) * 0.8;
                            double vy = 0.5 + Math.random() * 0.8;
                            double vz = (Math.random() - 0.5) * 0.8;
                            fb.setVelocity(new Vector(vx, vy, vz));
                            // Remove after 3 seconds
                            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                                if (!fb.isDead()) fb.remove();
                            }, 60L);
                        }

                        spawnShockwave(player, tier, intensity);
                        cleanupAll(player, weapon, originalSlot);
                    }
                }.runTaskLater(plugin, 5L);
            }
        }.runTaskLater(plugin, 10L);
    }

    private void spawnShockwave(Player player, WeaponTier tier, float intensity) {
        Location loc = player.getLocation();
        Vector dir = loc.getDirection().setY(0).normalize();
        int range = (int)(5 * intensity);
        for (int i = 1; i <= range; i++) {
            final int step = i;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                Location point = loc.clone().add(dir.clone().multiply(step));
                spawnTierParticle(point, tier, (int)(10 * intensity), 0.5, 0.1, 0.5);
                point.getWorld().playSound(point, Sound.BLOCK_ANVIL_LAND, 0.4f, 0.5f + (step * 0.1f));
            }, i * 2L); // Stagger for traveling wave effect
        }
    }

    // ============================================================
    // RONIN: Crescent Sweep
    // ============================================================

    public void playRoninAbility(Player player, ItemStack weapon, WeaponTier tier) {
        PlayerData data = economyManager.getPlayerData(player.getUniqueId());
        int level = data != null ? data.getLevel() : 1;
        float intensity = 1.0f + (level / 50.0f);

        // Detect weapon hand slot to avoid duplication/switching bugs
        EnumWrappers.ItemSlot originalSlot = getHandOfWeapon(player, weapon);

        updateVisualEquipment(player, new ItemStack(Material.AIR), originalSlot);

        Location spawnLoc = player.getEyeLocation().add(
                player.getLocation().getDirection().setY(0).normalize().multiply(0.5));
        ItemDisplay display = spawnSafeDisplay(player, spawnLoc, weapon);
        display.setInterpolationDuration(5);

        // Phase 1: Position katana at LEFT side
        display.setTransformation(new Transformation(
                new Vector3f(-0.5f, -0.2f, 0.5f),
                new Quaternionf().rotationY((float) Math.toRadians(90)),
                new Vector3f(1.3f, 1.3f, 1.3f),
                new Quaternionf()
        ));

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) { cleanupAll(player, weapon, originalSlot); return; }

                // Phase 2: WIDE 180° HORIZONTAL SWEEP to the right
                display.setInterpolationDuration(4);
                display.setTransformation(new Transformation(
                        new Vector3f(0.5f, -0.2f, 0.5f),
                        new Quaternionf().rotationY((float) Math.toRadians(-90)),
                        new Vector3f(1.3f, 1.3f, 1.3f),
                        new Quaternionf()
                ));

                spawnTierParticle(player.getLocation().add(0, 1, 0), tier, (int)(15 * intensity), 1.0, 0.2, 1.0);
                player.getWorld().spawnParticle(Particle.SWEEP_ATTACK,
                        player.getEyeLocation().add(player.getLocation().getDirection()), 1);
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.2f, 1.2f);

                Bukkit.getScheduler().runTaskLater(plugin, () -> cleanupAll(player, weapon, originalSlot), 6L);
            }
        }.runTaskLater(plugin, 5L);
    }

    // ============================================================
    // ROGUE: Shadow Lunge (backstab from invisibility)
    // ============================================================

    public void playRogueAbility(Player player, ItemStack weapon, WeaponTier tier) {
        PlayerData data = economyManager.getPlayerData(player.getUniqueId());
        int level = data != null ? data.getLevel() : 1;
        float intensity = 1.0f + (level / 50.0f);

        // Detect weapon hand slot to avoid duplication/switching bugs
        EnumWrappers.ItemSlot originalSlot = getHandOfWeapon(player, weapon);

        // Phase 1: Brief invisibility + smoke puff at origin
        Location origin = player.getLocation().clone();
        origin.getWorld().spawnParticle(Particle.LARGE_SMOKE, origin.clone().add(0, 1, 0),
                (int)(15 * intensity), 0.3, 0.5, 0.3, 0.05);
        player.playSound(origin, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.5f);

        // Make player briefly invisible (1 second)
        player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.INVISIBILITY, 20, 0, false, false));

        updateVisualEquipment(player, new ItemStack(Material.AIR), originalSlot);

        // Phase 2: Lunge forward after 5 ticks
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) { cleanupAll(player, weapon, originalSlot); return; }

                Vector lungeDir = player.getLocation().getDirection().normalize().multiply(1.5);
                lungeDir.setY(0.2);
                player.setVelocity(lungeDir);

                // Phase 3: Spawn dagger slash effect at target
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (!player.isOnline()) { cleanupAll(player, weapon, originalSlot); return; }

                    Location slashLoc = player.getEyeLocation().add(
                            player.getLocation().getDirection().multiply(1.0));
                    ItemDisplay slash = spawnSafeDisplay(player, slashLoc, weapon);
                    slash.setInterpolationDuration(3);

                    // Quick diagonal slash (X-cut)
                    slash.setTransformation(new Transformation(
                            new Vector3f(0.3f, 0.3f, 0),
                            new Quaternionf().rotationZ((float) Math.toRadians(45)),
                            new Vector3f(1.0f, 1.0f, 1.0f),
                            new Quaternionf()
                    ));

                    player.playSound(slashLoc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 2.0f);
                    spawnTierParticle(slashLoc, tier, (int)(10 * intensity), 0.3, 0.3, 0.3);
                    player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, slashLoc, 2, 0.2, 0.2, 0.2, 0);

                    // Deal damage to nearby entities (first slash)
                    double slashDamage = 6.0 * intensity;
                    for (Entity e : player.getNearbyEntities(3, 2, 3)) {
                        if (e instanceof LivingEntity target && target != player) {
                            target.damage(slashDamage, player);
                        }
                    }

                    // Second slash (opposite diagonal) after 3 ticks
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (!player.isOnline()) { cleanupAll(player, weapon, originalSlot); return; }

                        Location slash2Loc = player.getEyeLocation().add(
                                player.getLocation().getDirection().multiply(1.0));
                        ItemDisplay slash2 = spawnSafeDisplay(player, slash2Loc, weapon);
                        slash2.setInterpolationDuration(3);

                        slash2.setTransformation(new Transformation(
                                new Vector3f(-0.3f, 0.3f, 0),
                                new Quaternionf().rotationZ((float) Math.toRadians(-45)),
                                new Vector3f(1.0f, 1.0f, 1.0f),
                                new Quaternionf()
                        ));

                        player.playSound(slash2Loc, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1f, 1.8f);
                        spawnTierParticle(slash2Loc, tier, (int)(10 * intensity), 0.3, 0.3, 0.3);

                        // Deal damage to nearby entities (second slash — bonus crit)
                        double critDamage = 8.0 * intensity;
                        for (Entity e : player.getNearbyEntities(3, 2, 3)) {
                            if (e instanceof LivingEntity target && target != player) {
                                target.damage(critDamage, player);
                            }
                        }

                        // Cleanup everything
                        Bukkit.getScheduler().runTaskLater(plugin, () -> cleanupAll(player, weapon, originalSlot), 5L);
                    }, 3L);
                }, 4L);
            }
        }.runTaskLater(plugin, 5L);
    }

    // ============================================================
    // ARCHER: Vibrating Draw + Tornado Volley
    // ============================================================

    public void playArcherAbility(Player player, ItemStack weapon, WeaponTier tier) {
        PlayerData data = economyManager.getPlayerData(player.getUniqueId());
        int level = data != null ? data.getLevel() : 1;
        float intensity = 1.0f + (level / 50.0f);

        // Detect weapon hand slot to avoid duplication/switching bugs
        EnumWrappers.ItemSlot originalSlot = getHandOfWeapon(player, weapon);

        updateVisualEquipment(player, new ItemStack(Material.AIR), originalSlot);

        Location bowLoc = player.getEyeLocation().add(player.getLocation().getDirection().multiply(0.5));
        ItemDisplay bowDisp = spawnSafeDisplay(player, bowLoc, weapon);
        bowDisp.setInterpolationDuration(1);

        // Phase 1: VIBRATING DRAW (Tension — 10 ticks of jitter)
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= 10 || !player.isOnline()) {
                    this.cancel();
                    if (!player.isOnline()) { cleanupAll(player, weapon, originalSlot); return; }
                    fireVolley(player, weapon, tier, intensity, originalSlot);
                    return;
                }

                float jitter = (float)(Math.random() * 0.05 - 0.025);
                bowDisp.setTransformation(new Transformation(
                        new Vector3f(jitter, jitter, jitter),
                        new Quaternionf().rotationX((float) Math.toRadians(10)),
                        new Vector3f(1.1f, 1.1f, 1.1f),
                        new Quaternionf()
                ));

                if (ticks % 2 == 0) player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.5f, 1.5f);
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void fireVolley(Player player, ItemStack weapon, WeaponTier tier, float intensity, EnumWrappers.ItemSlot originalSlot) {
        player.playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1f, 0.5f);

        int arrowCount = (int)(3 + intensity);
        for (int i = 0; i < arrowCount; i++) {
            double angle = Math.toRadians(i * (360.0 / arrowCount));
            Vector dir = player.getLocation().getDirection();
            Vector offset = new Vector(Math.cos(angle) * 0.2, Math.sin(angle) * 0.2, 0);
            dir.add(offset).normalize().multiply(2.0);

            Arrow arrow = player.launchProjectile(Arrow.class, dir);
            arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
            arrow.setCritical(true);
            arrow.setDamage(5.0 * intensity);

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!arrow.isDead()) spawnTierParticle(arrow.getLocation(), tier, 3, 0.1, 0.1, 0.1);
            }, 1L);
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> cleanupAll(player, weapon, originalSlot), 5L);
    }

    // ============================================================
    // WIZARD: Astral Summon (Staff Raise → Meteor)
    // ============================================================

    public void playWizardAbility(Player player, ItemStack weapon, WeaponTier tier) {
        PlayerData data = economyManager.getPlayerData(player.getUniqueId());
        int level = data != null ? data.getLevel() : 1;
        final float intensity = 1.0f + (level / 50.0f);

        // Range scales with level: 15 base + 0.5 per level, max 40
        final int range = Math.min(40, 15 + (level / 2));

        // Casting particles and sound (no staff animation)
        player.getWorld().spawnParticle(Particle.WITCH, player.getLocation().add(0, 2, 0), 30, 0.5, 0.5, 0.5, 0.05);
        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_AMBIENT, 1f, 1.5f);

        // After 10 ticks, spawn meteor at target
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) { cleanupAll(player, weapon); return; }

                // Targeting: Prefer player if looking at one within range
                Location tLoc = null;
                List<Entity> targets = player.getNearbyEntities(range, range, range);
                Vector direction = player.getLocation().getDirection();
                Location start = player.getEyeLocation();
                
                double closest = Double.MAX_VALUE;
                for (Entity e : targets) {
                    if (e instanceof Player && e != player) {
                        Vector toEntity = e.getLocation().toVector().subtract(start.toVector());
                        if (toEntity.normalize().dot(direction) > 0.98) { // Tight cone
                            double dist = e.getLocation().distance(start);
                            if (dist < closest) {
                                closest = dist;
                                tLoc = e.getLocation();
                            }
                        }
                    }
                }

                if (tLoc == null) {
                    tLoc = player.getTargetBlock(null, range).getLocation();
                    if (tLoc.getBlock().getType() == Material.AIR) {
                        tLoc = player.getLocation().add(player.getLocation().getDirection().multiply(range / 2.0));
                    }
                }
                final Location targetLoc = tLoc;

                Location meteorStart = targetLoc.clone().add(0, 15, 0);
                ItemDisplay meteor = spawnSafeDisplay(player, meteorStart,
                        new ItemStack(Material.GILDED_BLACKSTONE));
                
                // Ensure the client knows it's at start position before animating
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (meteor.isDead()) return;
                        
                        meteor.setInterpolationDuration(12);
                        meteor.setInterpolationDelay(0);
                        meteor.setTransformation(new Transformation(
                                new Vector3f(0, -15f, 0),
                                new Quaternionf().rotationXYZ(3, 3, 3),
                                new Vector3f(5.0f * intensity, 5.0f * intensity, 5.0f * intensity),
                                new Quaternionf()
                        ));
                    }
                }.runTaskLater(plugin, 1L);

                // Phase 2: Meteor impact → Explosion and AOE Damage
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        World w = targetLoc.getWorld();
                        
                        // Deal TNT-like damage (scales with level)
                        float power = 3.0f * intensity; 
                        w.spawnParticle(Particle.EXPLOSION, targetLoc, (int)(5 * intensity), 1, 0.5, 1, 0.1);
                        w.spawnParticle(Particle.LARGE_SMOKE, targetLoc, (int)(30 * intensity), 2, 1, 2, 0.1);
                        spawnTierParticle(targetLoc, tier, (int)(80 * intensity), 2, 1, 2);
                        
                        // (false, false) means no fire and no block damage
                        w.createExplosion(targetLoc, power, false, false, player);
                        
                        cleanupAll(player, weapon);
                    }
                }.runTaskLater(plugin, 13L);
            }
        }.runTaskLater(plugin, 10L);
    }

    // ============================================================
    // SAFE DISPLAY MANAGEMENT
    // ============================================================

    /**
     * Spawns an ItemDisplay and tracks it for guaranteed cleanup.
     */
    private ItemDisplay spawnSafeDisplay(Player owner, Location loc, ItemStack item) {
        ItemDisplay display = loc.getWorld().spawn(loc, ItemDisplay.class, ent -> {
            ent.setItemStack(item);
            ent.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.THIRDPERSON_RIGHTHAND);
        });
        activeDisplays.computeIfAbsent(owner.getUniqueId(), k -> new ArrayList<>()).add(display);
        return display;
    }

    /**
     * Helper to find which slot the original weapon was held in.
     */
    private EnumWrappers.ItemSlot getHandOfWeapon(Player player, ItemStack weapon) {
        if (weapon != null && player.getInventory().getItemInOffHand().isSimilar(weapon)) {
            return EnumWrappers.ItemSlot.OFFHAND;
        }
        return EnumWrappers.ItemSlot.MAINHAND;
    }

    /**
     * Removes ALL active displays for a player and restores their weapon.
     */
    private void cleanupAll(Player player, ItemStack originalWeapon) {
        EnumWrappers.ItemSlot originalSlot = getHandOfWeapon(player, originalWeapon);
        cleanupAll(player, originalWeapon, originalSlot);
    }

    private void cleanupAll(Player player, ItemStack originalWeapon, EnumWrappers.ItemSlot originalSlot) {
        List<ItemDisplay> displays = activeDisplays.remove(player.getUniqueId());
        if (displays != null) {
            for (ItemDisplay d : displays) {
                if (d != null && !d.isDead()) d.remove();
            }
        }
        if (player.isOnline()) {
            updateVisualEquipment(player, originalWeapon, originalSlot);
        }
    }

    // ============================================================
    // PARTICLE UTILITIES
    // ============================================================

    /**
     * Safely spawn tier-based particles. Uses only Void-data particles
     * to avoid 1.21.x IllegalArgumentException.
     */
    private void spawnTierParticle(Location loc, WeaponTier tier, int count,
                                    double offX, double offY, double offZ) {
        Particle particle = getTierParticle(tier);
        World world = loc.getWorld();
        if (world == null) return;
        world.spawnParticle(particle, loc, count, offX, offY, offZ, 0.05);
    }

    private Particle getTierParticle(WeaponTier tier) {
        return switch (tier) {
            case GOLD -> Particle.TOTEM_OF_UNDYING;
            case DIAMOND -> Particle.ENCHANTED_HIT;
            case NETHER -> Particle.FLAME;
            case BONE -> Particle.SOUL;
            case CHAOS -> Particle.LARGE_SMOKE;   // Safe Void-data particle
            default -> Particle.LARGE_SMOKE;
        };
    }

    // ============================================================
    // PROTOCOLLIB EQUIPMENT PACKETS
    // ============================================================

    private void updateVisualEquipment(Player performer, ItemStack item) {
        EnumWrappers.ItemSlot originalSlot = getHandOfWeapon(performer, item);
        updateVisualEquipment(performer, item, originalSlot);
    }

    private void updateVisualEquipment(Player performer, ItemStack item, EnumWrappers.ItemSlot originalSlot) {
        PacketContainer packet = protocolManager.createPacket(PacketType.Play.Server.ENTITY_EQUIPMENT);
        packet.getIntegers().write(0, performer.getEntityId());

        List<Pair<EnumWrappers.ItemSlot, ItemStack>> equipment = new ArrayList<>();
        equipment.add(new Pair<>(originalSlot, item));
        packet.getSlotStackPairLists().write(0, equipment);

        protocolManager.broadcastServerPacket(packet, performer, true);
    }
}
