package me.pirot.kingdomCore.shop;

import me.pirot.kingdomCore.KingdomCore;
import me.pirot.kingdomCore.economy.EconomyManager;
import me.pirot.kingdomCore.rpg.RPGClass;
import me.pirot.kingdomCore.rpg.WeaponManager;
import me.pirot.kingdomCore.rpg.WeaponTier;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.UUID;

/**
 * Handles all inventory click events for shop GUIs.
 * Supports paginated navigation and item purchases.
 * Uses ShopDataManager (MongoDB) for item lookups.
 */
public class GUIListener implements Listener {

    private final KingdomCore plugin;
    private final ShopGUI shopGUI;
    private final EconomyManager economyManager;
    private final WeaponManager weaponManager;
    private final ShopDataManager shopDataManager;
    private final ShopCommandHandler shopCommandHandler;

    public GUIListener(KingdomCore plugin, ShopGUI shopGUI, EconomyManager economyManager,
                       WeaponManager weaponManager, ShopDataManager shopDataManager, 
                       ShopCommandHandler shopCommandHandler) {
        this.plugin = plugin;
        this.shopGUI = shopGUI;
        this.economyManager = economyManager;
        this.weaponManager = weaponManager;
        this.shopDataManager = shopDataManager;
        this.shopCommandHandler = shopCommandHandler;
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

        // Ignore glass pane border clicks
        if (clickedItem.getType().name().endsWith("_STAINED_GLASS_PANE")) return;

        ItemMeta meta = clickedItem.getItemMeta();
        if (meta == null) return;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        // ---- Handle navigation button clicks ----
        if (pdc.has(shopGUI.SHOP_NAV_KEY, PersistentDataType.STRING)) {
            String navData = pdc.get(shopGUI.SHOP_NAV_KEY, PersistentDataType.STRING);
            if (navData != null && navData.contains(":")) {
                String[] parts = navData.split(":");
                ShopType navShopType = ShopType.fromString(parts[0]);
                int targetPage = Integer.parseInt(parts[1]);
                if (navShopType != null) {
                    Inventory newInv = shopGUI.buildShopInventory(navShopType, targetPage);
                    player.openInventory(newInv);
                    player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1f, 1f);
                }
            }
            return;
        }

        // ---- Handle back button click ----
        if (pdc.has(shopGUI.SHOP_BACK_KEY, PersistentDataType.BOOLEAN)) {
            shopCommandHandler.openMainMenu(player);
            player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1f, 1f);
            return;
        }

        // Ignore non-shop items (like the Book page indicator)
        if (!pdc.has(shopGUI.SHOP_ITEM_KEY, PersistentDataType.STRING)) return;

        String itemId = pdc.get(shopGUI.SHOP_ITEM_KEY, PersistentDataType.STRING);
        String shopTypeName = pdc.get(shopGUI.SHOP_TYPE_KEY, PersistentDataType.STRING);
        int priceShards = pdc.getOrDefault(shopGUI.SHOP_PRICE_SHARDS_KEY, PersistentDataType.INTEGER, 0);
        int priceGems = pdc.getOrDefault(shopGUI.SHOP_PRICE_GEMS_KEY, PersistentDataType.INTEGER, 0);

        ShopType shopType = ShopType.fromString(shopTypeName);
        if (shopType == null) return;

        UUID uuid = player.getUniqueId();

        // ---- Check if player can afford ----
        if (!economyManager.hasBalance(uuid, priceShards, priceGems)) {
            player.sendMessage("§c§l[Kingdom] §7You do not have enough funds!");
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        // ---- Look up item data from MongoDB cache ----
        ShopItemData itemData = shopDataManager.findItem(shopType, itemId);

        // ---- Handle weapon purchase ----
        if (itemData != null && itemData.isWeapon()) {
            economyManager.removeShards(uuid, priceShards);
            economyManager.removeGems(uuid, priceGems);

            RPGClass weaponClass = RPGClass.fromString(itemData.getRpgClass());
            WeaponTier weaponTier = WeaponTier.fromString(itemData.getTier());
            String name = itemData.getName();
            List<String> lore = itemData.getLore();
            double damage = itemData.getDamage();
            double speed = itemData.getSpeed();

            if (weaponClass != null && weaponTier != null) {
                ItemStack weapon = weaponManager.createWeapon(weaponClass, weaponTier, name, lore, damage, speed);
                giveItem(player, weapon);
                player.sendMessage("§a§l[Kingdom] §7Purchased " + name + "§7!");
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f);
                
                // --- LOGGING ---
                org.bson.Document log = new org.bson.Document()
                        .append("source", "GAME")
                        .append("type", "SHOP_PURCHASE")
                        .append("player", new org.bson.Document("uuid", uuid.toString()).append("name", player.getName()))
                        .append("summary", "Purchased weapon: " + name)
                        .append("currency", new org.bson.Document("shards", priceShards).append("gems", priceGems))
                        .append("metadata", new org.bson.Document("itemId", itemId).append("shop", shopTypeName));
                plugin.getMongoManager().logAction(log);
            }
            return;
        }

        // ---- Handle enchanted book purchase ----
        if (itemData != null && itemData.isEnchantedBook()) {
            economyManager.removeShards(uuid, priceShards);
            economyManager.removeGems(uuid, priceGems);

            ItemStack book = new ItemStack(Material.ENCHANTED_BOOK, 1);
            EnchantmentStorageMeta bookMeta = (EnchantmentStorageMeta) book.getItemMeta();
            if (bookMeta != null) {
                Enchantment enchantment = Enchantment.getByKey(org.bukkit.NamespacedKey.minecraft(itemData.getEnchant().toLowerCase()));
                if (enchantment != null) {
                    bookMeta.addStoredEnchant(enchantment, itemData.getEnchantLevel(), true);
                }
                book.setItemMeta(bookMeta);
            }
            giveItem(player, book);
            player.sendMessage("§a§l[Kingdom] §7Purchased " + itemData.getName() + "§7!");
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f);

            // --- LOGGING ---
            org.bson.Document log = new org.bson.Document()
                    .append("source", "GAME")
                    .append("type", "SHOP_PURCHASE")
                    .append("player", new org.bson.Document("uuid", uuid.toString()).append("name", player.getName()))
                    .append("summary", "Purchased enchantment: " + itemData.getName())
                    .append("currency", new org.bson.Document("shards", priceShards).append("gems", priceGems))
                    .append("metadata", new org.bson.Document("itemId", itemId).append("shop", shopTypeName));
            plugin.getMongoManager().logAction(log);
            return;
        }

        // ---- Handle normal item purchase ----
        economyManager.removeShards(uuid, priceShards);
        economyManager.removeGems(uuid, priceGems);

        // Create a clean copy of the item (without shop PDC data)
        ItemStack purchased = new ItemStack(clickedItem.getType(), clickedItem.getAmount());
        String displayName = (meta.hasDisplayName()) ? meta.getDisplayName() : clickedItem.getType().name();
        
        player.sendMessage("§a§l[Kingdom] §7Purchased §f" + purchased.getAmount() + "x " + displayName);
        giveItem(player, purchased);
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f);

        // --- LOGGING ---
        org.bson.Document log = new org.bson.Document()
                .append("source", "GAME")
                .append("type", "SHOP_PURCHASE")
                .append("player", new org.bson.Document("uuid", uuid.toString()).append("name", player.getName()))
                .append("summary", "Purchased item: " + purchased.getAmount() + "x " + displayName)
                .append("currency", new org.bson.Document("shards", priceShards).append("gems", priceGems))
                .append("metadata", new org.bson.Document("itemId", itemId).append("shop", shopTypeName));
        plugin.getMongoManager().logAction(log);
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
