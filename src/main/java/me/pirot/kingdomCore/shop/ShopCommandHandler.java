package me.pirot.kingdomCore.shop;

import me.pirot.kingdomCore.KingdomCore;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Handles the /shop command with subcommands for each shop type.
 * Opens an enchanted Main Menu GUI for easy navigation.
 */
public class ShopCommandHandler implements CommandExecutor, TabCompleter, Listener {

    private final KingdomCore plugin;
    private final ShopGUI shopGUI;
    private final ConverterShop converterShop;

    private static final String MAIN_SHOP_TITLE = "§8§l[ §5§lKingdom Shops §8§l]";
    private final NamespacedKey ACTION_KEY;

    // Valid subcommands
    private static final List<String> SUBCOMMANDS = Arrays.asList(
            "wood", "stone", "fisherman", "fletcher", "redstone", "farming",
            "classes",
            "blacksmith", "enchant", "potion", "nether", "end", "armor",
            "sell", "ah", "bounty"
    );

    public ShopCommandHandler(KingdomCore plugin, ShopGUI shopGUI, ConverterShop converterShop) {
        this.plugin = plugin;
        this.shopGUI = shopGUI;
        this.converterShop = converterShop;
        this.ACTION_KEY = new NamespacedKey(plugin, "shop_action");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return true;
        }

        if (args.length == 0) {
            openMainMenu(player);
            return true;
        }

        String sub = args[0].toLowerCase();

        // Handle special redirects
        if (sub.equals("sell") || sub.equals("convert")) {
            converterShop.openConverter(player);
            return true;
        }

        if (sub.equals("ah")) {
            player.performCommand("ah");
            return true;
        }

        if (sub.equals("bounty")) {
            player.performCommand("bounty");
            return true;
        }

        // Map subcommand to ShopType
        ShopType shopType = ShopType.fromString(sub);
        if (shopType == null) {
            player.sendMessage("§c§l[Kingdom] §7Unknown shop type: §c" + sub);
            openMainMenu(player);
            return true;
        }

        // Build and open the shop
        Inventory shopInv = shopGUI.buildShopInventory(shopType);
        player.openInventory(shopInv);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return SUBCOMMANDS.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    /**
     * Opens the visual Main Menu GUI.
     */
    private void openMainMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, MAIN_SHOP_TITLE);

        fillMainMenuBorder(inv);

        // Core / Classes
        inv.setItem(22, createIcon(Material.NETHER_STAR, "§d§lClass Selection", "classes",
                "§7Choose or switch your RPG class.", "§b✦ §eRequires Gems"));

        // Gear / Dual Shops
        inv.setItem(20, createIcon(Material.ANVIL, "§8§lBlacksmith", "blacksmith",
                "§7Buy high-tier weapons.", "§6✦ §eDual Currency"));
        inv.setItem(29, createIcon(Material.DIAMOND_CHESTPLATE, "§b§lArmor Shop", "armor",
                "§7Buy protective gear.", "§6✦ §eDual Currency"));
        inv.setItem(24, createIcon(Material.ENCHANTING_TABLE, "§9§lEnchantments", "enchant",
                "§7Buy magic books.", "§6✦ §eDual Currency"));
        inv.setItem(33, createIcon(Material.BREWING_STAND, "§d§lPotions", "potion",
                "§7Buy brewing supplies.", "§6✦ §eDual Currency"));

        // Resource / Shard Shops
        inv.setItem(38, createIcon(Material.WHEAT, "§e§lFarming Shop", "farming",
                "§7Crops and seeds.", "§6✦ §eRequires Shards"));
        inv.setItem(39, createIcon(Material.OAK_LOG, "§a§lWood Shop", "wood",
                "§7Logs and planks.", "§6✦ §eRequires Shards"));
        inv.setItem(40, createIcon(Material.COBBLESTONE, "§7§lStone Shop", "stone",
                "§7Building blocks.", "§6✦ §eRequires Shards"));
        inv.setItem(41, createIcon(Material.REDSTONE, "§c§lRedstone Shop", "redstone",
                "§7Mechanisms.", "§6✦ §eRequires Shards"));
        inv.setItem(42, createIcon(Material.FISHING_ROD, "§3§lFisherman", "fisherman",
                "§7Fishing supplies.", "§6✦ §eRequires Shards"));

        // Dimensions
        inv.setItem(11, createIcon(Material.NETHERRACK, "§4§lNether Shop", "nether",
                "§7Hellish resources.", "§6✦ §eDual Currency"));
        inv.setItem(15, createIcon(Material.END_STONE, "§e§lEnd Shop", "end",
                "§7Void materials.", "§6✦ §eDual Currency"));

        // Utilities
        inv.setItem(31, createIcon(Material.GOLD_INGOT, "§a§lOre Converter", "converter",
                "§7Convert ores to Shards.", "§6✦ §eEarn Shards"));
        inv.setItem(49, createIcon(Material.GOLDEN_HORSE_ARMOR, "§6§lAuction House", "ah",
                "§7Player marketplace.", "§ePvP Economy"));

        player.openInventory(inv);
    }

    private void fillMainMenuBorder(Inventory inv) {
        ItemStack border = createDecoPane(Material.MAGENTA_STAINED_GLASS_PANE);
        ItemStack corner = createDecoPane(Material.PURPLE_STAINED_GLASS_PANE);

        for (int i = 0; i < 54; i++) {
            if (isBorderSlot(i, 54)) {
                if (isCornerSlot(i, 54)) {
                    inv.setItem(i, corner);
                } else {
                    inv.setItem(i, border);
                }
            }
        }
    }

    private boolean isBorderSlot(int slot, int size) {
        int row = slot / 9;
        int col = slot % 9;
        int maxRow = (size / 9) - 1;
        return row == 0 || row == maxRow || col == 0 || col == 8;
    }

    private boolean isCornerSlot(int slot, int size) {
        int maxRow = (size / 9) - 1;
        return (slot == 0 || slot == 8 || slot == maxRow * 9 || slot == maxRow * 9 + 8);
    }

    private ItemStack createDecoPane(Material material) {
        ItemStack pane = new ItemStack(material);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            pane.setItemMeta(meta);
        }
        return pane;
    }

    private ItemStack createIcon(Material material, String name, String action, String... loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);

            List<String> lore = new ArrayList<>();
            lore.add("§8§m                              ");
            lore.add("");
            lore.addAll(Arrays.asList(loreLines));
            lore.add("");
            lore.add("§8§m                              ");
            lore.add("§e▸ Click to open");

            meta.setLore(lore);

            meta.getPersistentDataContainer().set(ACTION_KEY, PersistentDataType.STRING, action);

            // Add glow to special categories
            if (action.equals("classes") || action.equals("converter") || action.equals("ah")) {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }

            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onMainMenuClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String title = event.getView().getTitle();
        if (!title.equals(MAIN_SHOP_TITLE)) return;

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) return;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (!pdc.has(ACTION_KEY, PersistentDataType.STRING)) return;

        String action = pdc.get(ACTION_KEY, PersistentDataType.STRING);
        if (action == null) return;

        switch (action) {
            case "ah":
                player.performCommand("ah");
                break;
            case "converter":
                converterShop.openConverter(player);
                break;
            default:
                ShopType shopType = ShopType.fromString(action);
                if (shopType != null) {
                    player.openInventory(shopGUI.buildShopInventory(shopType));
                }
                break;
        }
    }
}
