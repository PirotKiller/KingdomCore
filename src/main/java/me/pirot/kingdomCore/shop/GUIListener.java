package me.pirot.kingdomCore.shop;

import me.pirot.kingdomCore.KingdomCore;
import me.pirot.kingdomCore.economy.EconomyManager;
import me.pirot.kingdomCore.rpg.ClassManager;
import me.pirot.kingdomCore.rpg.RPGClass;
import me.pirot.kingdomCore.rpg.WeaponManager;
import me.pirot.kingdomCore.rpg.WeaponTier;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.UUID;

/**
 * Handles all inventory click events for shop GUIs.
 */
public class GUIListener implements Listener {

    private final KingdomCore plugin;
    private final ShopGUI shopGUI;
    private final EconomyManager economyManager;
    private final ClassManager classManager;
    private final WeaponManager weaponManager;

    public GUIListener(KingdomCore plugin, ShopGUI shopGUI, EconomyManager economyManager,
                       ClassManager classManager, WeaponManager weaponManager) {
        this.plugin = plugin;
        this.shopGUI = shopGUI;
        this.economyManager = economyManager;
        this.classManager = classManager;
        this.weaponManager = weaponManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        // Check if this is a shop inventory by title prefix
        String title = event.getView().getTitle();
        if (!title.startsWith(ShopGUI.SHOP_IDENTIFIER)) return;

        // It's a shop GUI — cancel the event to prevent taking items
        event.setCancelled(true);

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;
        if (clickedItem.getType() == Material.BLACK_STAINED_GLASS_PANE) return; // Border

        ItemMeta meta = clickedItem.getItemMeta();
        if (meta == null) return;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        // Verify this is a shop item
        if (!pdc.has(shopGUI.SHOP_ITEM_KEY, PersistentDataType.STRING)) return;

        String itemId = pdc.get(shopGUI.SHOP_ITEM_KEY, PersistentDataType.STRING);
        String shopTypeName = pdc.get(shopGUI.SHOP_TYPE_KEY, PersistentDataType.STRING);
        int priceShards = pdc.getOrDefault(shopGUI.SHOP_PRICE_SHARDS_KEY, PersistentDataType.INTEGER, 0);
        int priceGems = pdc.getOrDefault(shopGUI.SHOP_PRICE_GEMS_KEY, PersistentDataType.INTEGER, 0);

        ShopType shopType = ShopType.fromString(shopTypeName);
        if (shopType == null) return;

        UUID uuid = player.getUniqueId();

        // ---- Check if player can afford ----
        // Bypass costs if it's a class selection and they are currently classless
        boolean isClassSelection = pdc.has(shopGUI.SHOP_CLASS_KEY, PersistentDataType.STRING);
        RPGClass currentClass = classManager.getPlayerClass(player);
        boolean isClassless = (currentClass == null);
        boolean waiveCost = isClassSelection && isClassless;

        if (!waiveCost) {
            if (priceShards > 0 && economyManager.getShards(uuid) < priceShards) {
                player.sendMessage("§c§l[Kingdom] §7Not enough Shards! Need §a" + priceShards + " Shards§7.");
                return;
            }
            if (priceGems > 0 && economyManager.getGems(uuid) < priceGems) {
                player.sendMessage("§c§l[Kingdom] §7Not enough Gems! Need §b" + priceGems + " Gems§7.");
                return;
            }
        }

        // ---- Handle class purchase ----
        if (isClassSelection) {
            String className = pdc.get(shopGUI.SHOP_CLASS_KEY, PersistentDataType.STRING);
            RPGClass rpgClass = RPGClass.fromString(className);
            if (rpgClass == null) {
                player.sendMessage("§c§l[Kingdom] §7Invalid class!");
                return;
            }

            // Check if already this class
            if (currentClass == rpgClass) {
                player.sendMessage("§c§l[Kingdom] §7You are already a " + rpgClass.getColoredName() + "§7!");
                return;
            }

            // Deduct currency if not waived
            if (!waiveCost) {
                if (priceShards > 0) economyManager.removeShards(uuid, priceShards);
                if (priceGems > 0) economyManager.removeGems(uuid, priceGems);
            } else {
                player.sendMessage("§a§l[Kingdom] §7Your first class selection is §f§lFREE§7!");
            }

            // Set class
            classManager.setClass(player, rpgClass);
            player.sendMessage("§a§l[Kingdom] §7You are now a " + rpgClass.getColoredName() + "§7!");
            player.closeInventory();
            return;
        }

        // ---- Handle weapon purchase (has custom damage/speed in config) ----
        ConfigurationSection itemConfig = getItemConfig(shopType, itemId);
        if (itemConfig != null && itemConfig.contains("damage") && itemConfig.contains("class") && itemConfig.contains("tier")) {
            // Deduct currency
            if (priceShards > 0) economyManager.removeShards(uuid, priceShards);
            if (priceGems > 0) economyManager.removeGems(uuid, priceGems);

            // Create weapon via WeaponManager
            RPGClass weaponClass = RPGClass.fromString(itemConfig.getString("class"));
            WeaponTier weaponTier = WeaponTier.fromString(itemConfig.getString("tier"));
            String name = itemConfig.getString("name", "§fWeapon");
            List<String> lore = itemConfig.getStringList("lore");
            double damage = itemConfig.getDouble("damage", 6.0);
            double speed = itemConfig.getDouble("speed", 1.4);

            if (weaponClass != null && weaponTier != null) {
                ItemStack weapon = weaponManager.createWeapon(weaponClass, weaponTier, name, lore, damage, speed);
                giveItem(player, weapon);
                player.sendMessage("§a§l[Kingdom] §7Purchased " + name + "§7!");
            }
            return;
        }

        // ---- Handle enchanted book purchase ----
        if (itemConfig != null && itemConfig.contains("enchant")) {
            if (priceShards > 0) economyManager.removeShards(uuid, priceShards);
            if (priceGems > 0) economyManager.removeGems(uuid, priceGems);

            ItemStack book = new ItemStack(Material.ENCHANTED_BOOK, 1);
            EnchantmentStorageMeta bookMeta = (EnchantmentStorageMeta) book.getItemMeta();
            if (bookMeta != null) {
                String enchantName = itemConfig.getString("enchant", "");
                int enchantLevel = itemConfig.getInt("enchant-level", 1);
                Enchantment enchantment = Enchantment.getByKey(NamespacedKey.minecraft(enchantName.toLowerCase()));
                if (enchantment != null) {
                    bookMeta.addStoredEnchant(enchantment, enchantLevel, true);
                }
                book.setItemMeta(bookMeta);
            }
            giveItem(player, book);
            String itemName = itemConfig.getString("name", "Enchanted Book");
            player.sendMessage("§a§l[Kingdom] §7Purchased " + itemName + "§7!");
            return;
        }

        // ---- Handle normal item purchase ----
        if (priceShards > 0) economyManager.removeShards(uuid, priceShards);
        if (priceGems > 0) economyManager.removeGems(uuid, priceGems);

        // Create a clean copy of the item (without shop PDC data)
        ItemStack purchased = new ItemStack(clickedItem.getType(), clickedItem.getAmount());
        // Don't copy over the shop metadata to the purchased item
        String itemName = meta.hasDisplayName() ? meta.getDisplayName() : clickedItem.getType().name();
        player.sendMessage("§a§l[Kingdom] §7Purchased " + itemName + "§7!");
        giveItem(player, purchased);
    }

    private ConfigurationSection getItemConfig(ShopType shopType, String itemId) {
        FileConfiguration shopConfig = plugin.getConfigManager().getShopConfig(shopType.getConfigKey());
        return shopConfig != null ? shopConfig.getConfigurationSection("items." + itemId) : null;
    }

    private void giveItem(Player player, ItemStack item) {
        if (player.getInventory().firstEmpty() != -1) {
            player.getInventory().addItem(item);
        } else {
            player.getWorld().dropItemNaturally(player.getLocation(), item);
            player.sendMessage("§e§l[Kingdom] §7Inventory full! Item dropped at your feet.");
        }
    }
}
