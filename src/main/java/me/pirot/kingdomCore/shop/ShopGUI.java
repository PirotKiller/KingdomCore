package me.pirot.kingdomCore.shop;

import me.pirot.kingdomCore.KingdomCore;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
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
 * Builds paginated shop GUI inventories (double chest, 54 slots) from MongoDB data.
 * Layout per page:
 *   Row 0 (slots 0-8):   Border + Info item at slot 4
 *   Rows 1-4 (slots 9-44): Border on cols 0,8 — items in cols 1-7 (28 slots)
 *   Row 5 (slots 45-53): Navigation bar (prev, page info, next)
 */
public class ShopGUI {

    private final KingdomCore plugin;
    private ShopDataManager shopDataManager;

    public final NamespacedKey SHOP_ITEM_KEY;
    public final NamespacedKey SHOP_TYPE_KEY;
    public final NamespacedKey SHOP_PRICE_SHARDS_KEY;
    public final NamespacedKey SHOP_PRICE_GEMS_KEY;
    public final NamespacedKey SHOP_NAV_KEY;
    public final NamespacedKey SHOP_BACK_KEY;

    // Prefix used to identify shop inventories
    public static final String SHOP_IDENTIFIER = "§6§l[ Shop ] §8§l";

    // 28 item slots per page (rows 1-4, cols 1-7)
    private static final int ITEMS_PER_PAGE = 28;

    public ShopGUI(KingdomCore plugin) {
        this.plugin = plugin;
        this.SHOP_ITEM_KEY = new NamespacedKey(plugin, "shop_item_id");
        this.SHOP_TYPE_KEY = new NamespacedKey(plugin, "shop_type");
        this.SHOP_PRICE_SHARDS_KEY = new NamespacedKey(plugin, "shop_price_shards");
        this.SHOP_PRICE_GEMS_KEY = new NamespacedKey(plugin, "shop_price_gems");
        this.SHOP_NAV_KEY = new NamespacedKey(plugin, "shop_nav");
        this.SHOP_BACK_KEY = new NamespacedKey(plugin, "shop_back");
    }

    public void setShopDataManager(ShopDataManager shopDataManager) {
        this.shopDataManager = shopDataManager;
    }

    /**
     * Build and return a paginated shop inventory for the given shop type and page.
     */
    public Inventory buildShopInventory(ShopType shopType, int page) {
        if (shopDataManager == null) {
            plugin.getLogger().warning("ShopDataManager not initialized!");
            return Bukkit.createInventory(null, 27, "§cShop not configured");
        }

        List<ShopItemData> allItems = shopDataManager.getItems(shopType);

        // Build title from ShopType
        String title = getShopTitle(shopType);

        int totalItems = allItems.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalItems / ITEMS_PER_PAGE));

        // Clamp page
        page = Math.max(0, Math.min(page, totalPages - 1));

        // Always 54 slots (double chest)
        Inventory inv = Bukkit.createInventory(null, 54, title);

        // Fill borders
        fillBorder(inv, shopType);

        // Add info item at top center
        addInfoItem(inv, shopType, page, totalPages);

        // Place items for this page
        if (!allItems.isEmpty()) {
            int startIndex = page * ITEMS_PER_PAGE;
            int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, totalItems);

            List<ShopItemData> pageItems = allItems.subList(startIndex, endIndex);
            placeItemsInGrid(inv, pageItems, shopType);
        }

        // Add navigation bar
        addNavigationBar(inv, shopType, page, totalPages);

        return inv;
    }

    /**
     * Overload for backwards compatibility — opens page 0.
     */
    public Inventory buildShopInventory(ShopType shopType) {
        return buildShopInventory(shopType, 0);
    }

    /**
     * Place items in the inner grid (rows 1-4, cols 1-7).
     */
    private void placeItemsInGrid(Inventory inv, List<ShopItemData> items, ShopType shopType) {
        int gridIndex = 0;
        for (ShopItemData itemData : items) {
            int slot = gridIndexToSlot(gridIndex);
            if (slot == -1) break; // Exceeded grid capacity

            ItemStack shopItem = createShopItem(itemData, shopType);
            if (shopItem != null) {
                inv.setItem(slot, shopItem);
            }
            gridIndex++;
        }
    }

    /**
     * Maps a sequential grid index (0-27) to the actual slot in the 54-slot inventory.
     */
    private int gridIndexToSlot(int index) {
        if (index < 0 || index >= ITEMS_PER_PAGE) return -1;
        int row = (index / 7) + 1; // Rows 1-4
        int col = (index % 7) + 1; // Cols 1-7
        return row * 9 + col;
    }

    /**
     * Create a single shop item from ShopItemData.
     */
    private ItemStack createShopItem(ShopItemData data, ShopType shopType) {
        Material material;
        try {
            material = Material.valueOf(data.getMaterial().toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid material: " + data.getMaterial() + " for shop item: " + data.getItemKey());
            return null;
        }

        int amount = data.getAmount();
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;

        // Display name
        meta.setDisplayName(data.getName());

        // Build enhanced lore
        List<String> lore = new ArrayList<>();

        // Item description from data
        if (data.getLore() != null && !data.getLore().isEmpty()) {
            lore.addAll(data.getLore());
        }

        // Separator line
        lore.add("");
        lore.add("§8§m                              ");

        // Price section with icons
        addPriceLore(lore, data, shopType);

        // Click instruction
        lore.add("");
        lore.add("§e▸ Click to purchase");

        meta.setLore(lore);

        // Custom model data
        if (data.getCmd() > 0) {
            meta.setCustomModelData(data.getCmd());
        }

        // Enchanted book handling
        if (data.isEnchantedBook()) {
            Enchantment enchantment = Enchantment.getByKey(NamespacedKey.minecraft(data.getEnchant().toLowerCase()));
            if (enchantment != null && meta instanceof EnchantmentStorageMeta esMeta) {
                esMeta.addStoredEnchant(enchantment, data.getEnchantLevel(), true);
            }
        }

        // PDC: store shop metadata
        meta.getPersistentDataContainer().set(SHOP_ITEM_KEY, PersistentDataType.STRING, data.getItemKey());
        meta.getPersistentDataContainer().set(SHOP_TYPE_KEY, PersistentDataType.STRING, shopType.name());

        // Store prices in PDC for purchase handler
        int priceShards = 0;
        int priceGems = 0;
        switch (shopType.getCurrencyMode()) {
            case SHARDS:
                priceShards = data.getPriceShards();
                break;
            case GEMS:
                priceGems = data.getPriceGems();
                break;
            case DUAL:
                priceShards = data.getPriceShards();
                priceGems = data.getPriceGems();
                break;
        }
        meta.getPersistentDataContainer().set(SHOP_PRICE_SHARDS_KEY, PersistentDataType.INTEGER, priceShards);
        meta.getPersistentDataContainer().set(SHOP_PRICE_GEMS_KEY, PersistentDataType.INTEGER, priceGems);

        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);

        item.setItemMeta(meta);
        return item;
    }

    private void addPriceLore(List<String> lore, ShopItemData data, ShopType shopType) {
        switch (shopType.getCurrencyMode()) {
            case SHARDS:
                lore.add("§6✦ §ePrice: §a" + formatNumber(data.getPriceShards()) + " Shards");
                break;
            case GEMS:
                lore.add("§b✦ §ePrice: §b" + formatNumber(data.getPriceGems()) + " Gems");
                break;
            case DUAL:
                int shards = data.getPriceShards();
                int gems = data.getPriceGems();
                if (shards > 0) lore.add("§6✦ §eShards: §a" + formatNumber(shards));
                if (gems > 0)   lore.add("§b✦ §eGems:   §b" + formatNumber(gems));
                if (shards == 0 && gems == 0) lore.add("§a✦ Free");
                break;
        }
    }

    /**
     * Add an info item at the top center of the GUI.
     */
    private void addInfoItem(Inventory inv, ShopType shopType, int page, int totalPages) {
        Material infoMaterial = getInfoMaterial(shopType);
        ItemStack info = new ItemStack(infoMaterial);
        ItemMeta meta = info.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§f§l" + getShopTitle(shopType).replaceAll("§.§.\\[\\s*", "").replaceAll("\\s*§.§.\\]", "").trim());

            List<String> lore = new ArrayList<>();
            lore.add("§8§m                              ");
            lore.add("");
            lore.add(getCurrencyHint(shopType));
            if (totalPages > 1) {
                lore.add("§7Page §f" + (page + 1) + " §7of §f" + totalPages);
            }
            lore.add("");
            lore.add("§7Browse the items below to");
            lore.add("§7find what you need!");
            meta.setLore(lore);

            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            info.setItemMeta(meta);
        }
        inv.setItem(4, info);
    }

    /**
     * Add navigation bar on the bottom row (row 5, slots 45-53).
     */
    private void addNavigationBar(Inventory inv, ShopType shopType, int page, int totalPages) {
        Material borderMat = getBorderMaterial(shopType);
        for (int i = 45; i <= 53; i++) {
            inv.setItem(i, createDecoPane(borderMat, " "));
        }

        // Back to Menu button at the end of the navigation bar
        ItemStack back = new ItemStack(Material.OAK_DOOR);
        ItemMeta backMeta = back.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName("§c§l◀ Return to Menu");
            List<String> lore = new ArrayList<>();
            lore.add("§8§m                              ");
            lore.add("§7Exit this shop and");
            lore.add("§7select a different one.");
            lore.add("");
            lore.add("§e▸ Click to go back");
            backMeta.setLore(lore);
            backMeta.getPersistentDataContainer().set(SHOP_BACK_KEY, PersistentDataType.BOOLEAN, true);
            back.setItemMeta(backMeta);
        }
        inv.setItem(53, back);

        if (totalPages <= 1) return;

        if (page > 0) {
            ItemStack prev = new ItemStack(Material.ARROW);
            ItemMeta prevMeta = prev.getItemMeta();
            if (prevMeta != null) {
                prevMeta.setDisplayName("§a◀ Previous Page");
                List<String> lore = new ArrayList<>();
                lore.add("§7Go to page §f" + page);
                prevMeta.setLore(lore);
                prevMeta.getPersistentDataContainer().set(SHOP_NAV_KEY, PersistentDataType.STRING,
                        shopType.name() + ":" + (page - 1));
                prev.setItemMeta(prevMeta);
            }
            inv.setItem(48, prev);
        }

        ItemStack pageInfo = new ItemStack(Material.BOOK);
        ItemMeta pageMeta = pageInfo.getItemMeta();
        if (pageMeta != null) {
            pageMeta.setDisplayName("§e§lPage " + (page + 1) + " §7/ §e§l" + totalPages);
            List<String> lore = new ArrayList<>();
            lore.add("§8§m                              ");
            lore.add("§7Use the arrows to");
            lore.add("§7navigate between pages.");
            pageMeta.setLore(lore);
            pageMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            pageInfo.setItemMeta(pageMeta);
        }
        inv.setItem(49, pageInfo);

        if (page < totalPages - 1) {
            ItemStack next = new ItemStack(Material.ARROW);
            ItemMeta nextMeta = next.getItemMeta();
            if (nextMeta != null) {
                nextMeta.setDisplayName("§aNext Page ▶");
                List<String> lore = new ArrayList<>();
                lore.add("§7Go to page §f" + (page + 2));
                nextMeta.setLore(lore);
                nextMeta.getPersistentDataContainer().set(SHOP_NAV_KEY, PersistentDataType.STRING,
                        shopType.name() + ":" + (page + 1));
                next.setItemMeta(nextMeta);
            }
            inv.setItem(50, next);
        }
    }

    private String getShopTitle(ShopType shopType) {
        String base = SHOP_IDENTIFIER;
        return switch (shopType) {
            case ENCHANT -> base + "§9§lEnchantments";
            case POTION -> base + "§d§lPotions";
            case NETHER -> base + "§4§lNether Shop";
            case END -> base + "§e§lEnd Shop";
            case ARMOR -> base + "§b§lArmor Shop";
            case WOOD -> base + "§a§lWood Shop";
            case STONE -> base + "§7§lStone Shop";
            case FISHERMAN -> base + "§3§lFisherman";
            case FLETCHER -> base + "§a§lFletcher";
            case REDSTONE -> base + "§c§lRedstone Shop";
            case FARMING -> base + "§e§lFarming Shop";
            case CONVERTER -> base + "§a§lOre Converter";
        };
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
            case ENCHANT -> Material.ENCHANTING_TABLE;
            case POTION -> Material.BREWING_STAND;
            case NETHER -> Material.NETHERRACK;
            case END -> Material.END_STONE;
            case ARMOR -> Material.DIAMOND_CHESTPLATE;
            case WOOD -> Material.OAK_LOG;
            case STONE -> Material.COBBLESTONE;
            case FISHERMAN -> Material.FISHING_ROD;
            case FLETCHER -> Material.ARROW;
            case REDSTONE -> Material.REDSTONE;
            case FARMING -> Material.WHEAT;
            case CONVERTER -> Material.GOLD_INGOT;
        };
    }

    private void fillBorder(Inventory inv, ShopType shopType) {
        Material borderMaterial = getBorderMaterial(shopType);
        Material cornerMaterial = getCornerMaterial(shopType);

        ItemStack border = createDecoPane(borderMaterial, " ");
        ItemStack corner = createDecoPane(cornerMaterial, " ");

        for (int i = 0; i < 45; i++) {
            if (isBorderSlot(i)) {
                if (isCornerSlot(i)) {
                    inv.setItem(i, corner);
                } else {
                    inv.setItem(i, border);
                }
            }
        }
    }

    private Material getBorderMaterial(ShopType shopType) {
        return switch (shopType) {
            case ENCHANT -> Material.PURPLE_STAINED_GLASS_PANE;
            case POTION -> Material.PINK_STAINED_GLASS_PANE;
            case NETHER -> Material.RED_STAINED_GLASS_PANE;
            case END -> Material.YELLOW_STAINED_GLASS_PANE;
            case ARMOR -> Material.LIGHT_BLUE_STAINED_GLASS_PANE;
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
            case ENCHANT -> Material.BLACK_STAINED_GLASS_PANE;
            case POTION -> Material.MAGENTA_STAINED_GLASS_PANE;
            case NETHER -> Material.BLACK_STAINED_GLASS_PANE;
            case END -> Material.BLACK_STAINED_GLASS_PANE;
            case ARMOR -> Material.BLUE_STAINED_GLASS_PANE;
            case WOOD -> Material.BLACK_STAINED_GLASS_PANE;
            case STONE -> Material.BLACK_STAINED_GLASS_PANE;
            case FISHERMAN -> Material.BLUE_STAINED_GLASS_PANE;
            case FLETCHER -> Material.GREEN_STAINED_GLASS_PANE;
            case REDSTONE -> Material.BLACK_STAINED_GLASS_PANE;
            case FARMING -> Material.LIME_STAINED_GLASS_PANE;
            case CONVERTER -> Material.BLACK_STAINED_GLASS_PANE;
        };
    }

    private boolean isBorderSlot(int slot) {
        int row = slot / 9;
        int col = slot % 9;
        return row == 0 || col == 0 || col == 8;
    }

    private boolean isCornerSlot(int slot) {
        return (slot == 0 || slot == 8);
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

    private String formatNumber(int number) {
        if (number >= 1000) {
            return String.format("%,d", number);
        }
        return String.valueOf(number);
    }
}
