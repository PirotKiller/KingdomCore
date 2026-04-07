package me.pirot.kingdomCore.auction;

import me.pirot.kingdomCore.KingdomCore;
import me.pirot.kingdomCore.economy.EconomyManager;
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

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Handles /ah command and auction house GUI interaction.
 * Features strict 54-slot pagination (28 items per page) and sorting filters.
 */
public class AuctionCommand implements CommandExecutor, TabCompleter, Listener {

    private final KingdomCore plugin;
    private final AuctionManager auctionManager;
    private final EconomyManager economyManager;

    private final NamespacedKey LISTING_ID_KEY;

    public static final String AH_TITLE = "§8§l[ §6§lAuction House §8§l]";

    private static final int ITEMS_PER_PAGE = 28;

    // Pagination state
    private final Map<UUID, Integer> playerPages = new HashMap<>();
    private final Map<UUID, SortType> playerSorts = new HashMap<>();

    public enum SortType {
        NEWEST("§aNewest First"),
        EXPIRING_SOON("§cExpiring Soon"),
        HIGHEST_PRICE("§6Highest Price"),
        LOWEST_PRICE("§eLowest Price");

        private final String displayName;

        SortType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        public SortType next() {
            SortType[] vals = values();
            return vals[(this.ordinal() + 1) % vals.length];
        }
    }

    public AuctionCommand(KingdomCore plugin, AuctionManager auctionManager, EconomyManager economyManager) {
        this.plugin = plugin;
        this.auctionManager = auctionManager;
        this.economyManager = economyManager;
        this.LISTING_ID_KEY = new NamespacedKey(plugin, "ah_listing_id");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return true;
        }

        if (args.length >= 2 && args[0].equalsIgnoreCase("sell")) {
            handleSell(player, args);
            return true;
        }

        // Initialize state
        playerPages.putIfAbsent(player.getUniqueId(), 0);
        playerSorts.putIfAbsent(player.getUniqueId(), SortType.NEWEST);

        openAuctionGUI(player);
        return true;
    }

    private void handleSell(Player player, String[] args) {
        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        if (itemInHand.getType() == Material.AIR) {
            player.sendMessage("§c§l[Kingdom] §7Hold an item to sell it!");
            return;
        }

        int priceShards;
        int priceGems = 0;
        try {
            priceShards = Integer.parseInt(args[1]);
            if (args.length >= 3) {
                priceGems = Integer.parseInt(args[2]);
            }
        } catch (NumberFormatException e) {
            player.sendMessage("§c§l[Kingdom] §7Usage: /ah sell <shards> [gems]");
            return;
        }

        if (priceShards < 0 || priceGems < 0) {
            player.sendMessage("§c§l[Kingdom] §7Prices must be positive!");
            return;
        }

        if (priceShards == 0 && priceGems == 0) {
            player.sendMessage("§c§l[Kingdom] §7Price must be at least 1!");
            return;
        }

        // Create listing
        ItemStack listingItem = itemInHand.clone();
        auctionManager.createListing(
                player.getUniqueId(), player.getName(),
                listingItem, priceShards, priceGems
        );

        // Remove item from hand
        player.getInventory().setItemInMainHand(null);
        player.sendMessage("§a§l[Kingdom] §7Listed §f" + listingItem.getType().name() +
                " x" + listingItem.getAmount() + "§7 for §a" + priceShards + " Shards" +
                (priceGems > 0 ? " §7+ §b" + priceGems + " Gems" : "") + "§7!");
    }

    private void openAuctionGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, AH_TITLE);

        fillAuctionBorder(inv, 54);

        int page = playerPages.getOrDefault(player.getUniqueId(), 0);
        SortType sort = playerSorts.getOrDefault(player.getUniqueId(), SortType.NEWEST);

        List<AuctionManager.AuctionListing> activeListings = new ArrayList<>(auctionManager.getActiveListings());

        // Sort listings
        activeListings.sort((a, b) -> {
            return switch (sort) {
                case NEWEST -> Long.compare(b.getExpireTime(), a.getExpireTime()); // newest = furthest expire time
                case EXPIRING_SOON -> Long.compare(a.getExpireTime(), b.getExpireTime());
                case HIGHEST_PRICE -> Integer.compare(b.getPriceShards(), a.getPriceShards());
                case LOWEST_PRICE -> Integer.compare(a.getPriceShards(), b.getPriceShards());
            };
        });

        int totalItems = activeListings.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalItems / ITEMS_PER_PAGE));

        // Ensure page is valid
        if (page >= totalPages) {
            page = totalPages - 1;
            playerPages.put(player.getUniqueId(), page);
        }

        // Info item (Slot 4)
        ItemStack info = new ItemStack(Material.GOLD_INGOT);
        ItemMeta infoMeta = info.getItemMeta();
        if (infoMeta != null) {
            infoMeta.setDisplayName("§6§l✦ Auction House ✦");
            infoMeta.setLore(Arrays.asList(
                    "§8§m                              ",
                    "",
                    "§7Active Listings: §e" + totalItems,
                    "§7Page: §e" + (page + 1) + " §7/ §e" + totalPages,
                    "",
                    "§8§m                              "
            ));
            infoMeta.addEnchant(Enchantment.UNBREAKING, 1, true);
            infoMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
            info.setItemMeta(infoMeta);
        }
        inv.setItem(4, info);

        // Sorting Filter Button (Slot 49)
        ItemStack filter = new ItemStack(Material.HOPPER);
        ItemMeta filterMeta = filter.getItemMeta();
        if (filterMeta != null) {
            filterMeta.setDisplayName("§e§lSort Filter");
            filterMeta.setLore(Arrays.asList(
                    "§7Current: " + sort.getDisplayName(),
                    "",
                    "§e▸ Click to change sort order"
            ));
            filter.setItemMeta(filterMeta);
        }
        inv.setItem(49, filter);

        // "How to Sell" book (Slot 50)
        ItemStack helpItem = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta helpMeta = helpItem.getItemMeta();
        if (helpMeta != null) {
            helpMeta.setDisplayName("§e§l? How to Sell");
            helpMeta.setLore(Arrays.asList(
                    "§8§m                              ",
                    "",
                    "§7§l1. §fHold item to sell",
                    "§7§l2. §fType §a/ah sell <shards>",
                    "",
                    "§7Listings expire after §e24 hours",
                    "§8§m                              "
            ));
            helpItem.setItemMeta(helpMeta);
        }
        inv.setItem(50, helpItem);

        // Navigation buttons
        if (page > 0) {
            ItemStack prev = createDecoPane(Material.ARROW, "§a§l← Previous Page");
            inv.setItem(45, prev);
        }
        if (page < totalPages - 1) {
            ItemStack next = createDecoPane(Material.ARROW, "§a§lNext Page →");
            inv.setItem(53, next);
        }

        // Place items
        int[] slots = {
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34,
                37, 38, 39, 40, 41, 42, 43
        };

        int startIndex = page * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, totalItems);

        int slotIndex = 0;
        for (int i = startIndex; i < endIndex; i++) {
            AuctionManager.AuctionListing listing = activeListings.get(i);
            ItemStack display = auctionManager.deserializeItem(listing.getSerializedItem());
            ItemMeta displayMeta = display.getItemMeta();
            if (displayMeta != null) {
                List<String> lore = displayMeta.hasLore() ? new ArrayList<>(displayMeta.getLore()) : new ArrayList<>();
                lore.add("");
                lore.add("§8§m                              ");
                lore.add("§7Seller: §f" + listing.getSellerName());
                lore.add("");

                if (listing.getPriceShards() > 0) lore.add("§6✦ §eShards: §a" + formatNumber(listing.getPriceShards()));
                if (listing.getPriceGems() > 0)   lore.add("§b✦ §eGems:   §b" + formatNumber(listing.getPriceGems()));

                long remaining = listing.getExpireTime() - System.currentTimeMillis();
                long hours = TimeUnit.MILLISECONDS.toHours(remaining);
                long minutes = TimeUnit.MILLISECONDS.toMinutes(remaining) % 60;
                String timeColor = hours >= 12 ? "§a" : hours >= 3 ? "§e" : "§c";
                lore.add("");
                lore.add("§7⏱ Expires: " + timeColor + hours + "h " + minutes + "m");

                lore.add("§8§m                              ");
                lore.add("");
                lore.add("§a▸ Left-click to purchase");

                displayMeta.setLore(lore);
                displayMeta.getPersistentDataContainer().set(LISTING_ID_KEY, PersistentDataType.STRING, listing.getListingId());
                display.setItemMeta(displayMeta);
            }
            inv.setItem(slots[slotIndex], display);
            slotIndex++;
        }

        if (totalItems == 0) {
            ItemStack empty = new ItemStack(Material.BARRIER);
            ItemMeta emptyMeta = empty.getItemMeta();
            if (emptyMeta != null) {
                emptyMeta.setDisplayName("§c§lNo Listings");
                emptyMeta.setLore(Arrays.asList("§7There are no active listings.", "§7Be the first to list an item!"));
                empty.setItemMeta(emptyMeta);
            }
            inv.setItem(22, empty);
        }

        player.openInventory(inv);
    }

    private void fillAuctionBorder(Inventory inv, int size) {
        ItemStack border = createDecoPane(Material.ORANGE_STAINED_GLASS_PANE, " ");
        ItemStack corner = createDecoPane(Material.BLACK_STAINED_GLASS_PANE, " ");

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

    @EventHandler
    public void onAuctionClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String title = event.getView().getTitle();
        if (!title.equals(AH_TITLE)) return;

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        // Navigation and Filters
        if (clicked.getType() == Material.ARROW) {
            String name = clicked.getItemMeta().getDisplayName();
            int page = playerPages.getOrDefault(player.getUniqueId(), 0);
            if (name.contains("Previous")) {
                playerPages.put(player.getUniqueId(), Math.max(0, page - 1));
            } else if (name.contains("Next")) {
                playerPages.put(player.getUniqueId(), page + 1);
            }
            openAuctionGUI(player);
            return;
        }

        if (clicked.getType() == Material.HOPPER) {
            SortType currentSort = playerSorts.getOrDefault(player.getUniqueId(), SortType.NEWEST);
            playerSorts.put(player.getUniqueId(), currentSort.next());
            playerPages.put(player.getUniqueId(), 0); // reset page on filter change
            openAuctionGUI(player);
            return;
        }

        // Decor blocks
        if (clicked.getType().name().endsWith("STAINED_GLASS_PANE") || clicked.getType() == Material.GOLD_INGOT || clicked.getType() == Material.WRITABLE_BOOK || clicked.getType() == Material.BARRIER) {
            return;
        }

        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) return;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (!pdc.has(LISTING_ID_KEY, PersistentDataType.STRING)) return;

        String listingId = pdc.get(LISTING_ID_KEY, PersistentDataType.STRING);
        AuctionManager.AuctionListing listing = auctionManager.getListing(listingId);
        if (listing == null) {
            player.sendMessage("§c§l[Kingdom] §7This listing no longer exists!");
            openAuctionGUI(player);
            return;
        }

        // Can't buy your own listing
        if (listing.getSeller().equals(player.getUniqueId())) {
            player.sendMessage("§c§l[Kingdom] §7You can't buy your own listing!");
            return;
        }

        UUID uuid = player.getUniqueId();

        // Check funds
        if (listing.getPriceShards() > 0 && economyManager.getShards(uuid) < listing.getPriceShards()) {
            player.sendMessage("§c§l[Kingdom] §7Not enough Shards!");
            return;
        }
        if (listing.getPriceGems() > 0 && economyManager.getGems(uuid) < listing.getPriceGems()) {
            player.sendMessage("§c§l[Kingdom] §7Not enough Gems!");
            return;
        }

        // Deduct funds
        if (listing.getPriceShards() > 0) economyManager.removeShards(uuid, listing.getPriceShards());
        if (listing.getPriceGems() > 0) economyManager.removeGems(uuid, listing.getPriceGems());

        // Give item
        ItemStack purchased = auctionManager.deserializeItem(listing.getSerializedItem());
        if (player.getInventory().firstEmpty() != -1) {
            player.getInventory().addItem(purchased);
        } else {
            player.getWorld().dropItemNaturally(player.getLocation(), purchased);
            player.sendMessage("§e§l[Kingdom] §7Inventory full! Item dropped.");
        }

        // Pay seller
        economyManager.addShards(listing.getSeller(), listing.getPriceShards());
        Player seller = Bukkit.getPlayer(listing.getSeller());
        if (seller != null && seller.isOnline()) {
            seller.sendMessage("§a§l[Kingdom] §7Your auction was purchased by §f" + player.getName() + "§7! §a+" + listing.getPriceShards() + " Shards");
        }

        // Remove listing
        auctionManager.removeListing(listingId);
        player.sendMessage("§a§l[Kingdom] §7Purchase successful!");
        openAuctionGUI(player); // refresh
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
        if (number >= 1000) return String.format("%,d", number);
        return String.valueOf(number);
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) return List.of("sell");
        if (args.length == 2 && args[0].equalsIgnoreCase("sell")) return List.of("<shards>");
        if (args.length == 3 && args[0].equalsIgnoreCase("sell")) return List.of("<gems>");
        return List.of();
    }
}
