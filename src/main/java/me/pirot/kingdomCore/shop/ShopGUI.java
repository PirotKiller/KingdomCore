package me.pirot.kingdomCore.shop;

import me.pirot.kingdomCore.KingdomCore;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds shop GUI inventories from YAML configuration.
 * Enhanced with themed borders, decorative separators, and rich lore.
 */
public class ShopGUI {

    private final KingdomCore plugin;

    public final NamespacedKey SHOP_ITEM_KEY;
    public final NamespacedKey SHOP_TYPE_KEY;
    public final NamespacedKey SHOP_PRICE_SHARDS_KEY;
    public final NamespacedKey SHOP_PRICE_GEMS_KEY;
    public final NamespacedKey SHOP_CLASS_KEY;

    // Prefix used to identify shop inventories
    public static final String SHOP_IDENTIFIER = "§8§l[";

    public ShopGUI(KingdomCore plugin) {
        this.plugin = plugin;
        this.SHOP_ITEM_KEY = new NamespacedKey(plugin, "shop_item_id");
        this.SHOP_TYPE_KEY = new NamespacedKey(plugin, "shop_type");
        this.SHOP_PRICE_SHARDS_KEY = new NamespacedKey(plugin, "shop_price_shards");
        this.SHOP_PRICE_GEMS_KEY = new NamespacedKey(plugin, "shop_price_gems");
        this.SHOP_CLASS_KEY = new NamespacedKey(plugin, "shop_class");
    }

    /**
     * Build and return a shop inventory for the given shop type.
     */
    public Inventory buildShopInventory(ShopType shopType) {
        FileConfiguration shopConfig = plugin.getConfigManager().getShopConfig(shopType.getConfigKey());

        if (shopConfig == null) {
            plugin.getLogger().warning("No config found for shop: " + shopType.getConfigKey());
            return Bukkit.createInventory(null, 27, "§cShop not configured");
        }

        String title = shopConfig.getString("title", "§8Shop");
        int size = shopConfig.getInt("size", 27);
        // Ensure size is multiple of 9
        size = Math.min(54, Math.max(9, (size / 9) * 9));

        Inventory inv = Bukkit.createInventory(null, size, title);

        // Fill borders with themed glass panes
        fillBorder(inv, size, shopType);

        // Add info item at top center
        addInfoItem(inv, shopType, shopConfig);

        // Add items
        ConfigurationSection itemsSection = shopConfig.getConfigurationSection("items");
        if (itemsSection == null) return inv;

        // For class selection, center the items
        if (shopType == ShopType.CLASSES) {
            placeItemsCentered(inv, itemsSection, shopType, size);
        } else {
            placeItemsSequential(inv, itemsSection, shopType, size);
        }

        return inv;
    }

    /**
     * Place items centered in the GUI (used for class selection).
     */
    private void placeItemsCentered(Inventory inv, ConfigurationSection itemsSection,
                                    ShopType shopType, int size) {
        List<String> keys = new ArrayList<>(itemsSection.getKeys(false));
        int count = keys.size();
        
        // Calculate center row (row 1 for 27-slot, row 2 for 45/54-slot)
        int centerRow = (size / 9) / 2;
        int startCol = Math.max(1, (9 - count) / 2); // Center horizontally

        for (int i = 0; i < count; i++) {
            int col = startCol + i;
            if (col > 7) break; // Don't go past border
            int slot = centerRow * 9 + col;

            ConfigurationSection itemConfig = itemsSection.getConfigurationSection(keys.get(i));
            if (itemConfig == null) continue;

            ItemStack shopItem = createShopItem(keys.get(i), itemConfig, shopType);
            if (shopItem != null) {
                inv.setItem(slot, shopItem);
            }
        }
    }

    /**
     * Place items sequentially in the GUI (standard shops).
     */
    private void placeItemsSequential(Inventory inv, ConfigurationSection itemsSection,
                                      ShopType shopType, int size) {
        int slot = 10; // Start placing items inside the border
        for (String key : itemsSection.getKeys(false)) {
            ConfigurationSection itemConfig = itemsSection.getConfigurationSection(key);
            if (itemConfig == null) continue;

            // Skip border slots
            while (isBorderSlot(slot, size) && slot < size) slot++;
            if (slot >= size) break;

            ItemStack shopItem = createShopItem(key, itemConfig, shopType);
            if (shopItem != null) {
                inv.setItem(slot, shopItem);
            }

            slot++;
        }
    }

    /**
     * Create a single shop item from config with enhanced lore formatting.
     */
    private ItemStack createShopItem(String key, ConfigurationSection config, ShopType shopType) {
        String materialName = config.getString("material", "STONE");
        Material material;
        try {
            material = Material.valueOf(materialName.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid material: " + materialName + " for shop item: " + key);
            return null;
        }

        int amount = config.getInt("amount", 1);
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;

        // Display name
        String name = config.getString("name", "§f" + key);
        meta.setDisplayName(name);

        // Build enhanced lore
        List<String> lore = new ArrayList<>();

        // Item description/stats from config
        if (config.contains("lore")) {
            lore.addAll(config.getStringList("lore"));
        }

        // Separator line
        lore.add("");
        lore.add("§8§m                              ");

        // Price section with icons
        addPriceLore(lore, config, shopType);

        // Click instruction
        lore.add("");
        if (config.contains("class")) {
            lore.add("§e▸ Click to select this class");
        } else {
            lore.add("§e▸ Click to purchase");
        }

        meta.setLore(lore);

        // Custom model data for weapons
        if (config.contains("cmd")) {
            meta.setCustomModelData(config.getInt("cmd"));
        }

        // Enchanted book handling
        if (material == Material.ENCHANTED_BOOK && config.contains("enchant")) {
            String enchantName = config.getString("enchant", "");
            int enchantLevel = config.getInt("enchant-level", 1);
            Enchantment enchantment = Enchantment.getByKey(NamespacedKey.minecraft(enchantName.toLowerCase()));
            if (enchantment != null && meta instanceof EnchantmentStorageMeta esMeta) {
                esMeta.addStoredEnchant(enchantment, enchantLevel, true);
            }
        }

        // Add enchant glow for class items (makes them visually stand out)
        if (config.contains("class") && !(meta instanceof EnchantmentStorageMeta)) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        // PDC: store shop metadata
        meta.getPersistentDataContainer().set(SHOP_ITEM_KEY, PersistentDataType.STRING, key);
        meta.getPersistentDataContainer().set(SHOP_TYPE_KEY, PersistentDataType.STRING, shopType.name());

        // Store prices in PDC for purchase handler
        int priceShards = 0;
        int priceGems = 0;
        switch (shopType.getCurrencyMode()) {
            case SHARDS:
                priceShards = config.getInt("price", 0);
                break;
            case GEMS:
                priceGems = config.getInt("price", 0);
                break;
            case DUAL:
                priceShards = config.getInt("price-shards", 0);
                priceGems = config.getInt("price-gems", 0);
                break;
        }
        meta.getPersistentDataContainer().set(SHOP_PRICE_SHARDS_KEY, PersistentDataType.INTEGER, priceShards);
        meta.getPersistentDataContainer().set(SHOP_PRICE_GEMS_KEY, PersistentDataType.INTEGER, priceGems);

        // If this is a class purchase item, store the class name
        if (config.contains("class")) {
            meta.getPersistentDataContainer().set(SHOP_CLASS_KEY, PersistentDataType.STRING, config.getString("class"));
        }

        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);

        item.setItemMeta(meta);
        return item;
    }

    private void addPriceLore(List<String> lore, ConfigurationSection config, ShopType shopType) {
        switch (shopType.getCurrencyMode()) {
            case SHARDS:
                int priceShards = config.getInt("price", 0);
                lore.add("§6✦ §ePrice: §a" + formatNumber(priceShards) + " Shards");
                break;
            case GEMS:
                int priceGems = config.getInt("price", 0);
                lore.add("§b✦ §ePrice: §b" + formatNumber(priceGems) + " Gems");
                break;
            case DUAL:
                int shards = config.getInt("price-shards", 0);
                int gems = config.getInt("price-gems", 0);
                if (shards > 0) lore.add("§6✦ §eShards: §a" + formatNumber(shards));
                if (gems > 0)   lore.add("§b✦ §eGems:   §b" + formatNumber(gems));
                if (shards == 0 && gems == 0) lore.add("§a✦ Free");
                break;
        }
    }

    /**
     * Add an info item at the top center of the GUI.
     */
    private void addInfoItem(Inventory inv, ShopType shopType, FileConfiguration shopConfig) {
        Material infoMaterial = getInfoMaterial(shopType);
        ItemStack info = new ItemStack(infoMaterial);
        ItemMeta meta = info.getItemMeta();
        if (meta != null) {
            String shopName = shopConfig.getString("title", shopType.name());
            // Strip the §8§l[ ... ] wrapper for a clean display name
            meta.setDisplayName("§f§l" + stripBrackets(shopName));

            List<String> lore = new ArrayList<>();
            lore.add("§8§m                              ");
            lore.add("");
            lore.add(getCurrencyHint(shopType));
            lore.add("");
            lore.add("§7Browse the items below to");
            lore.add("§7find what you need!");
            meta.setLore(lore);

            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            info.setItemMeta(meta);
        }
        inv.setItem(4, info);
    }

    private String getCurrencyHint(ShopType shopType) {
        return switch (shopType.getCurrencyMode()) {
            case SHARDS -> "§6✦ §7This shop accepts §aShards";
            case GEMS -> "§b✦ §7This shop accepts §bGems";
            case DUAL -> "§6✦ §7Accepts §aShards §7+ §bGems";
        };
    }

    private Material getInfoMaterial(ShopType shopType) {
        return switch (shopType) {
            case BLACKSMITH -> Material.ANVIL;
            case ENCHANT -> Material.ENCHANTING_TABLE;
            case POTION -> Material.BREWING_STAND;
            case NETHER -> Material.NETHERRACK;
            case END -> Material.END_STONE;
            case ARMOR -> Material.DIAMOND_CHESTPLATE;
            case CLASSES -> Material.NETHER_STAR;
            case WOOD -> Material.OAK_LOG;
            case STONE -> Material.COBBLESTONE;
            case FISHERMAN -> Material.FISHING_ROD;
            case FLETCHER -> Material.ARROW;
            case REDSTONE -> Material.REDSTONE;
            case FARMING -> Material.WHEAT;
            case CONVERTER -> Material.GOLD_INGOT;
        };
    }

    private void fillBorder(Inventory inv, int size, ShopType shopType) {
        Material borderMaterial = getBorderMaterial(shopType);
        Material cornerMaterial = getCornerMaterial(shopType);

        ItemStack border = createDecoPane(borderMaterial, " ");
        ItemStack corner = createDecoPane(cornerMaterial, " ");

        for (int i = 0; i < size; i++) {
            if (isBorderSlot(i, size)) {
                if (isCornerSlot(i, size)) {
                    inv.setItem(i, corner);
                } else {
                    inv.setItem(i, border);
                }
            }
        }
    }

    private Material getBorderMaterial(ShopType shopType) {
        return switch (shopType) {
            case BLACKSMITH -> Material.GRAY_STAINED_GLASS_PANE;
            case ENCHANT -> Material.PURPLE_STAINED_GLASS_PANE;
            case POTION -> Material.PINK_STAINED_GLASS_PANE;
            case NETHER -> Material.RED_STAINED_GLASS_PANE;
            case END -> Material.YELLOW_STAINED_GLASS_PANE;
            case ARMOR -> Material.LIGHT_BLUE_STAINED_GLASS_PANE;
            case CLASSES -> Material.MAGENTA_STAINED_GLASS_PANE;
            case WOOD -> Material.BROWN_STAINED_GLASS_PANE;
            case STONE -> Material.LIGHT_GRAY_STAINED_GLASS_PANE;
            case FISHERMAN -> Material.CYAN_STAINED_GLASS_PANE;
            case FLETCHER -> Material.LIME_STAINED_GLASS_PANE;
            case REDSTONE -> Material.RED_STAINED_GLASS_PANE;
            case FARMING -> Material.GREEN_STAINED_GLASS_PANE;
            case CONVERTER -> Material.ORANGE_STAINED_GLASS_PANE;
        };
    }

    private Material getCornerMaterial(ShopType shopType) {
        return switch (shopType) {
            case BLACKSMITH -> Material.BLACK_STAINED_GLASS_PANE;
            case ENCHANT -> Material.BLACK_STAINED_GLASS_PANE;
            case POTION -> Material.MAGENTA_STAINED_GLASS_PANE;
            case NETHER -> Material.BLACK_STAINED_GLASS_PANE;
            case END -> Material.BLACK_STAINED_GLASS_PANE;
            case ARMOR -> Material.BLUE_STAINED_GLASS_PANE;
            case CLASSES -> Material.PURPLE_STAINED_GLASS_PANE;
            case WOOD -> Material.BLACK_STAINED_GLASS_PANE;
            case STONE -> Material.BLACK_STAINED_GLASS_PANE;
            case FISHERMAN -> Material.BLUE_STAINED_GLASS_PANE;
            case FLETCHER -> Material.GREEN_STAINED_GLASS_PANE;
            case REDSTONE -> Material.BLACK_STAINED_GLASS_PANE;
            case FARMING -> Material.LIME_STAINED_GLASS_PANE;
            case CONVERTER -> Material.BLACK_STAINED_GLASS_PANE;
        };
    }

    private boolean isCornerSlot(int slot, int size) {
        int maxRow = (size / 9) - 1;
        return (slot == 0 || slot == 8 || slot == maxRow * 9 || slot == maxRow * 9 + 8);
    }

    private boolean isBorderSlot(int slot, int size) {
        int row = slot / 9;
        int col = slot % 9;
        int maxRow = (size / 9) - 1;
        return row == 0 || row == maxRow || col == 0 || col == 8;
    }

    private ItemStack createDecoPane(Material material, String name) {
        ItemStack pane = new ItemStack(material);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            pane.setItemMeta(meta);
        }
        return pane;
    }

    private String stripBrackets(String title) {
        return title.replaceAll("§.§.\\[\\s*", "").replaceAll("\\s*§.§.\\]", "").trim();
    }

    private String formatNumber(int number) {
        if (number >= 1000) {
            return String.format("%,d", number);
        }
        return String.valueOf(number);
    }
}
