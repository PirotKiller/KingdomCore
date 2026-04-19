package me.pirot.kingdomCore.rpg;

import me.pirot.kingdomCore.KingdomCore;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;

/**
 * Factory for creating special RPG items:
 * - Class Change Scroll: allows switching to a different class
 * - Data Restore Tome: restores saved progress from a previously played class
 * - Soul Item: crafting ingredient for class weapons
 */
public class SpecialItems {

    public final NamespacedKey SPECIAL_ITEM_KEY;
    public final NamespacedKey SHARD_VALUE_KEY;

    public static final String CLASS_CHANGE_ID = "CLASS_CHANGE_SCROLL";
    public static final String DATA_RESTORE_ID = "DATA_RESTORE_TOME";
    public static final String SOUL_ITEM_ID = "SOUL_ITEM";

    public SpecialItems(KingdomCore plugin) {
        this.SPECIAL_ITEM_KEY = new NamespacedKey(plugin, "special_item");
        this.SHARD_VALUE_KEY = new NamespacedKey(plugin, "shard_value");
    }

    /**
     * Class Change Scroll — consumes to open class selection GUI.
     * Resets current class XP/level (but saves progress for restoration).
     */
    public ItemStack createClassChangeScroll() {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§6§l✦ Class Change Scroll §6§l✦");
            meta.setLore(Arrays.asList(
                    "§8§m                              ",
                    "",
                    "§7Use this scroll to change your",
                    "§7RPG class. Your current progress",
                    "§7will be saved and can be restored",
                    "§7later with a §eData Restore Tome§7.",
                    "",
                    "§c⚠ §7Your level & XP will reset",
                    "§c⚠ §7for the new class!",
                    "",
                    "§a▸ Right-click to use",
                    "§8§m                              "
            ));
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            meta.getPersistentDataContainer().set(SPECIAL_ITEM_KEY, PersistentDataType.STRING, CLASS_CHANGE_ID);
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Data Restore Tome — restores saved progress from a previously played class.
     */
    public ItemStack createDataRestoreTome() {
        ItemStack item = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§5§l✦ Data Restore Tome §5§l✦");
            meta.setLore(Arrays.asList(
                    "§8§m                              ",
                    "",
                    "§7Use this tome to restore your",
                    "§7level and XP from a previously",
                    "§7played class.",
                    "",
                    "§7Only works if you have prior",
                    "§7saved progress for your current class.",
                    "",
                    "§a▸ Right-click to use",
                    "§8§m                              "
            ));
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            meta.getPersistentDataContainer().set(SPECIAL_ITEM_KEY, PersistentDataType.STRING, DATA_RESTORE_ID);
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Soul Item — used as a crafting ingredient for class weapons.
     * Obtained from boss kills, quests, or purchasing.
     */
    public ItemStack createSoulItem() {
        return createSoulItem(1);
    }

    public ItemStack createSoulItem(int amount) {
        ItemStack item = new ItemStack(Material.NETHER_STAR, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§b§l✦ Soul Fragment §b§l✦");
            meta.setLore(Arrays.asList(
                    "§8§m                              ",
                    "",
                    "§7A fragment of pure essence.",
                    "§7Used for crafting §eclass weapons§7.",
                    "",
                    "§7Obtained from:",
                    "§8 ▸ §7Boss Kills",
                    "§8 ▸ §7Special Quests",
                    "",
                    "§8§m                              "
            ));
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            meta.getPersistentDataContainer().set(SPECIAL_ITEM_KEY, PersistentDataType.STRING, SOUL_ITEM_ID);
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Check if an ItemStack is a special item.
     */
    public boolean isSpecialItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(SPECIAL_ITEM_KEY, PersistentDataType.STRING);
    }

    /**
     * Get the special item ID from an ItemStack.
     */
    public String getSpecialItemId(ItemStack item) {
        if (!isSpecialItem(item)) return null;
        return item.getItemMeta().getPersistentDataContainer().get(SPECIAL_ITEM_KEY, PersistentDataType.STRING);
    }

    // ==== PHYSICAL SHARD SYSTEM ====

    public static final String PHYSICAL_SHARD_ID = "PHYSICAL_SHARD";

    /**
     * Creates a physical shard item that drops on the ground and can be picked up.
     */
    public ItemStack createPhysicalShard(int value) {
        ItemStack item = new ItemStack(Material.SUNFLOWER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§6§l✦ Shards §8(§a" + value + "§8) §6§l✦");
            meta.setLore(Arrays.asList(
                    "§7A cluster of valuable shards.",
                    "",
                    "§a▸ Pick up to add to balance"
            ));
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            
            meta.getPersistentDataContainer().set(SPECIAL_ITEM_KEY, PersistentDataType.STRING, PHYSICAL_SHARD_ID);
            meta.getPersistentDataContainer().set(SHARD_VALUE_KEY, PersistentDataType.INTEGER, value);
            
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Gets the value of a physical shard item.
     */
    public int getPhysicalShardValue(ItemStack item) {
        if (!isSpecialItem(item)) return 0;
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.getPersistentDataContainer().has(SHARD_VALUE_KEY, PersistentDataType.INTEGER)) {
            Integer val = meta.getPersistentDataContainer().get(SHARD_VALUE_KEY, PersistentDataType.INTEGER);
            return val != null ? val : 0;
        }
        return 0;
    }
}
