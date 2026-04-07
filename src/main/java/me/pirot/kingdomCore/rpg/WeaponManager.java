package me.pirot.kingdomCore.rpg;

import me.pirot.kingdomCore.KingdomCore;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * Creates custom weapon ItemStacks with Custom Model Data and PDC stats.
 */
public class WeaponManager {

    private final KingdomCore plugin;
    public final NamespacedKey DAMAGE_KEY;
    public final NamespacedKey SPEED_KEY;
    public final NamespacedKey CLASS_KEY;
    public final NamespacedKey TIER_KEY;

    public WeaponManager(KingdomCore plugin) {
        this.plugin = plugin;
        this.DAMAGE_KEY = new NamespacedKey(plugin, "kingdom_damage");
        this.SPEED_KEY = new NamespacedKey(plugin, "kingdom_speed");
        this.CLASS_KEY = new NamespacedKey(plugin, "kingdom_class");
        this.TIER_KEY = new NamespacedKey(plugin, "kingdom_tier");
    }

    /**
     * Create a custom weapon with the resource pack model and custom stats.
     *
     * @param rpgClass The class this weapon belongs to
     * @param tier     The weapon tier
     * @param name     Display name
     * @param lore     Lore lines
     * @param damage   Custom damage value
     * @param speed    Custom attack speed value
     * @return Fully configured ItemStack
     */
    public ItemStack createWeapon(RPGClass rpgClass, WeaponTier tier, String name,
                                  List<String> lore, double damage, double speed) {
        Material baseMaterial = rpgClass.getWeaponMaterial();
        ItemStack item = new ItemStack(baseMaterial, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        // Set display name
        meta.setDisplayName(name);

        // Set lore with stats
        List<String> fullLore = new ArrayList<>();
        if (lore != null) fullLore.addAll(lore);
        fullLore.add("");
        fullLore.add("§7Damage: §c" + damage);
        fullLore.add("§7Speed: §a" + speed);
        fullLore.add("§7Class: " + rpgClass.getColoredName());
        fullLore.add("§7Tier: " + tier.getColoredName());
        meta.setLore(fullLore);

        // Set Custom Model Data using the legacy method (works on both Spigot and Paper)
        // CMD = class offset + tier sub-offset (e.g., ARCHER GOLD = 1000 + 1 = 1001)
        int cmd = rpgClass.getCmdOffset() + tier.getCmdSubOffset();
        meta.setCustomModelData(cmd);

        // Store custom stats in PersistentDataContainer
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(DAMAGE_KEY, PersistentDataType.DOUBLE, damage);
        pdc.set(SPEED_KEY, PersistentDataType.DOUBLE, speed);
        pdc.set(CLASS_KEY, PersistentDataType.STRING, rpgClass.name());
        pdc.set(TIER_KEY, PersistentDataType.STRING, tier.name());

        item.setItemMeta(meta);
        return item;
    }

    /**
     * Check if an ItemStack is a Kingdom weapon.
     */
    public boolean isKingdomWeapon(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(DAMAGE_KEY, PersistentDataType.DOUBLE);
    }

    /**
     * Get the custom damage value from a Kingdom weapon.
     */
    public double getWeaponDamage(ItemStack item) {
        if (!isKingdomWeapon(item)) return 0;
        Double val = item.getItemMeta().getPersistentDataContainer().get(DAMAGE_KEY, PersistentDataType.DOUBLE);
        return val != null ? val : 0;
    }

    /**
     * Get the custom speed value from a Kingdom weapon.
     */
    public double getWeaponSpeed(ItemStack item) {
        if (!isKingdomWeapon(item)) return 0;
        Double val = item.getItemMeta().getPersistentDataContainer().get(SPEED_KEY, PersistentDataType.DOUBLE);
        return val != null ? val : 0;
    }
}
