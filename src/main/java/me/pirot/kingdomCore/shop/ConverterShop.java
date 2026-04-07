package me.pirot.kingdomCore.shop;

import me.pirot.kingdomCore.KingdomCore;
import me.pirot.kingdomCore.config.ConfigManager;
import me.pirot.kingdomCore.economy.EconomyManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * The Converter Shop: players place ores/ingots into the GUI.
 * On close or clicking 'Sell', items are evaluated and Shards awarded.
 * Enhanced with themed visuals, price preview, and rich formatting.
 */
public class ConverterShop implements Listener {

    private final KingdomCore plugin;
    private final EconomyManager economyManager;

    // Track which inventories are converter GUIs
    private final Set<UUID> activeConverters = new HashSet<>();

    // Title used to identify converter inventories
    private String converterTitle;

    // Which slots can accept input (excluding border and sell button)
    private static final int SELL_BUTTON_SLOT = 49;
    private static final int INFO_SLOT = 4;

    public ConverterShop(KingdomCore plugin, ConfigManager configManager, EconomyManager economyManager) {
        this.plugin = plugin;
        this.economyManager = economyManager;
        FileConfiguration convConfig = configManager.getShopConfig("converter");
        this.converterTitle = convConfig != null ? convConfig.getString("title", "§8§l[ §a§lOre Converter §8§l]") : "§8§l[ §a§lOre Converter §8§l]";
    }

    /**
     * Open the converter GUI for a player.
     */
    public void openConverter(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, converterTitle);

        // Fill themed border
        fillConverterBorder(inv);

        // Info item at top
        ItemStack info = new ItemStack(Material.RAW_GOLD);
        ItemMeta infoMeta = info.getItemMeta();
        if (infoMeta != null) {
            infoMeta.setDisplayName("§6§l✦ Ore Converter ✦");
            infoMeta.setLore(Arrays.asList(
                    "§8§m                              ",
                    "",
                    "§7Place §fores§7, §fingots§7, and §fraw",
                    "§7materials §7in the empty slots.",
                    "",
                    "§7Click §a§lSELL ALL §7or close the",
                    "§7inventory to convert items.",
                    "",
                    "§8§m                              ",
                    "§7Top Values:",
                    "§f Netherite Ingot → §a200 Shards",
                    "§f Netherite Scrap → §a100 Shards",
                    "§f Ancient Debris  → §a80 Shards"
            ));
            infoMeta.addEnchant(Enchantment.UNBREAKING, 1, true);
            infoMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
            info.setItemMeta(infoMeta);
        }
        inv.setItem(INFO_SLOT, info);

        // Sell button
        ItemStack sellButton = new ItemStack(Material.EMERALD);
        ItemMeta sellMeta = sellButton.getItemMeta();
        if (sellMeta != null) {
            sellMeta.setDisplayName("§a§l✦ SELL ALL ✦");
            sellMeta.setLore(Arrays.asList(
                    "§8§m                              ",
                    "",
                    "§7Click to sell §fall items§7 in",
                    "§7the converter for §aShards§7.",
                    "",
                    "§7Non-sellable items will be",
                    "§7returned to your inventory.",
                    "",
                    "§8§m                              ",
                    "§e▸ Click to sell"
            ));
            sellMeta.addEnchant(Enchantment.UNBREAKING, 1, true);
            sellMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
            sellButton.setItemMeta(sellMeta);
        }
        inv.setItem(SELL_BUTTON_SLOT, sellButton);

        activeConverters.add(player.getUniqueId());
        player.openInventory(inv);
    }

    @EventHandler
    public void onConverterClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!activeConverters.contains(player.getUniqueId())) return;

        String title = event.getView().getTitle();
        if (!title.equals(converterTitle)) return;

        int slot = event.getRawSlot();

        // Allow interaction with non-border input slots (player can place items)
        if (slot >= 0 && slot < 54) {
            // Block clicks on border/info
            if (isBorderSlot(slot) || slot == INFO_SLOT) {
                event.setCancelled(true);
                return;
            }

            // Handle sell button click
            if (slot == SELL_BUTTON_SLOT) {
                event.setCancelled(true);
                processConversion(player, event.getInventory());
                return;
            }
        }
        // Allow player inventory clicks (for putting items in)
    }

    @EventHandler
    public void onConverterClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!activeConverters.remove(player.getUniqueId())) return;

        String title = event.getView().getTitle();
        if (!title.equals(converterTitle)) return;

        // Process any remaining items on close
        processConversion(player, event.getInventory());
    }

    /**
     * Process all items in the converter, award shards, return non-sellable.
     */
    private void processConversion(Player player, Inventory inv) {
        FileConfiguration convConfig = plugin.getConfigManager().getShopConfig("converter");
        if (convConfig == null) return;

        ConfigurationSection values = convConfig.getConfigurationSection("values");
        if (values == null) return;

        int totalShards = 0;
        int itemsSold = 0;
        List<ItemStack> returnItems = new ArrayList<>();

        for (int slot = 0; slot < 54; slot++) {
            if (isBorderSlot(slot) || slot == INFO_SLOT || slot == SELL_BUTTON_SLOT) continue;

            ItemStack item = inv.getItem(slot);
            if (item == null || item.getType() == Material.AIR) continue;
            // Skip deco glass panes
            if (item.getType().name().endsWith("STAINED_GLASS_PANE")) continue;

            String materialKey = item.getType().name();
            if (values.contains(materialKey)) {
                int valuePerItem = values.getInt(materialKey, 0);
                totalShards += valuePerItem * item.getAmount();
                itemsSold += item.getAmount();
                inv.setItem(slot, null);
            } else {
                // Non-sellable item — return to player
                returnItems.add(item.clone());
                inv.setItem(slot, null);
            }
        }

        // Award shards
        if (totalShards > 0) {
            economyManager.addShards(player.getUniqueId(), totalShards);
            player.sendMessage("§a§l[Kingdom] §7Converted §f" + itemsSold + " items §7for §a" + totalShards + " Shards§7!");
        } else if (returnItems.isEmpty()) {
            player.sendMessage("§e§l[Kingdom] §7No items to convert.");
        }

        // Return non-sellable items
        if (!returnItems.isEmpty()) {
            player.sendMessage("§e§l[Kingdom] §7Returned §f" + returnItems.size() + " §7non-convertible item(s).");
            for (ItemStack returnItem : returnItems) {
                if (player.getInventory().firstEmpty() != -1) {
                    player.getInventory().addItem(returnItem);
                } else {
                    player.getWorld().dropItemNaturally(player.getLocation(), returnItem);
                }
            }
        }
    }

    private void fillConverterBorder(Inventory inv) {
        ItemStack border = createDecoPane(Material.ORANGE_STAINED_GLASS_PANE, " ");
        ItemStack corner = createDecoPane(Material.YELLOW_STAINED_GLASS_PANE, " ");
        ItemStack accent = createDecoPane(Material.BLACK_STAINED_GLASS_PANE, " ");

        for (int i = 0; i < 54; i++) {
            if (isBorderSlot(i)) {
                if (isCornerSlot(i)) {
                    inv.setItem(i, corner);
                } else if (i == 3 || i == 5 || i == 48 || i == 50) {
                    inv.setItem(i, accent);
                } else {
                    inv.setItem(i, border);
                }
            }
        }
    }

    private boolean isBorderSlot(int slot) {
        int row = slot / 9;
        int col = slot % 9;
        return row == 0 || row == 5 || col == 0 || col == 8;
    }

    private boolean isCornerSlot(int slot) {
        return slot == 0 || slot == 8 || slot == 45 || slot == 53;
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
}
