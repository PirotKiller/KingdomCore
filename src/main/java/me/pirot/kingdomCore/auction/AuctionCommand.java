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
import org.bukkit.inventory.meta.SkullMeta;
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
    public static final String MY_AH_TITLE = "§8§l[ §e§lMy Listings §8§l]";

    private static final int ITEMS_PER_PAGE = 28;

    // Pagination & Filter state
    private final Map<UUID, Integer> playerPages = new HashMap<>();
    private final Map<UUID, SortType> playerSorts = new HashMap<>();
    private final Map<UUID, ItemCategory> playerCategories = new HashMap<>();

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

    public enum ItemCategory {
        ALL("§fAll Items", Material.NETHER_STAR),
        BLOCKS("§eBlocks", Material.GRASS_BLOCK),
        COMBAT("§cCombat", Material.DIAMOND_SWORD),
        TOOLS("§bTools", Material.DIAMOND_PICKAXE),
        MISC("§7Misc", Material.BARRIER);

        private final String displayName;
        private final Material icon;

        ItemCategory(String displayName, Material icon) {
            this.displayName = displayName;
            this.icon = icon;
        }

        public String getDisplayName() { return displayName; }
        public Material getIcon() { return icon; }

        public ItemCategory next() {
            ItemCategory[] vals = values();
            return vals[(this.ordinal() + 1) % vals.length];
        }

        public boolean matches(ItemStack item) {
            if (this == ALL) return true;
            Material m = item.getType();
            String name = m.name();
            
            switch (this) {
                case BLOCKS: return m.isBlock();
                case COMBAT: return name.contains("SWORD") || name.contains("ARMOR") || name.contains("HELMET") || 
                                    name.contains("CHESTPLATE") || name.contains("LEGGINGS") || name.contains("BOOTS") ||
                                    name.contains("BOW") || name.contains("SHIELD") || name.contains("TRIDENT") || name.contains("AXE");
                case TOOLS:  return (name.contains("PICKAXE") || name.contains("SHOVEL") || name.contains("HOE") || name.contains("AXE")) && !name.contains("BATTLE");
                case MISC:   return !m.isBlock() && !COMBAT.matches(item) && !TOOLS.matches(item);
                default:     return true;
            }
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
        playerCategories.putIfAbsent(player.getUniqueId(), ItemCategory.ALL);

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
        long durationMillis = TimeUnit.HOURS.toMillis(24); // Default 24h

        try {
            priceShards = Integer.parseInt(args[1]);
            if (args.length >= 3) {
                try {
                    priceGems = Integer.parseInt(args[2]);
                    if (args.length >= 4) {
                        durationMillis = parseDuration(args[3]);
                    }
                } catch (NumberFormatException e) {
                    durationMillis = parseDuration(args[2]);
                }
            }
        } catch (NumberFormatException e) {
            player.sendMessage("§c§l[Kingdom] §7Usage: /ah sell <shards> [gems] [time(e.g. 12h, 1d)]");
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
        
        if (durationMillis <= 0) {
            player.sendMessage("§c§l[Kingdom] §7Invalid duration! Use e.g. 2h, 12h, 1d");
            return;
        }

        // Create listing
        ItemStack listingItem = itemInHand.clone();
        auctionManager.createListing(
                player.getUniqueId(), player.getName(),
                listingItem, priceShards, priceGems, durationMillis
        );

        // Remove item from hand
        player.getInventory().setItemInMainHand(null);
        player.sendMessage("§a§l[Kingdom] §7Listed §f" + listingItem.getType().name() +
                " x" + listingItem.getAmount() + "§7 for §a" + priceShards + " Shards" +
                (priceGems > 0 ? " §7+ §b" + priceGems + " Gems" : "") + 
                " §7for §e" + formatDuration(durationMillis) + "§7!");
    }

    private long parseDuration(String input) {
        try {
            if (input.matches("\\d+")) return TimeUnit.HOURS.toMillis(Long.parseLong(input));
            
            long time = 0;
            String number = "";
            for (char c : input.toLowerCase().toCharArray()) {
                if (Character.isDigit(c)) {
                    number += c;
                } else {
                    if (number.isEmpty()) continue;
                    long val = Long.parseLong(number);
                    switch (c) {
                        case 'm' -> time += TimeUnit.MINUTES.toMillis(val);
                        case 'h' -> time += TimeUnit.HOURS.toMillis(val);
                        case 'd' -> time += TimeUnit.DAYS.toMillis(val);
                    }
                    number = "";
                }
            }
            return time;
        } catch (Exception e) {
            return -1;
        }
    }

    private String formatDuration(long millis) {
        long days = TimeUnit.MILLISECONDS.toDays(millis);
        long hours = TimeUnit.MILLISECONDS.toHours(millis) % 24;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        
        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0 && days == 0) sb.append(minutes).append("m");
        return sb.toString().trim();
    }

    private void openAuctionGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, AH_TITLE);
        fillAuctionBorder(inv, 54);

        int page = playerPages.getOrDefault(player.getUniqueId(), 0);
        SortType sort = playerSorts.getOrDefault(player.getUniqueId(), SortType.NEWEST);
        ItemCategory category = playerCategories.getOrDefault(player.getUniqueId(), ItemCategory.ALL);

        List<AuctionManager.AuctionListing> activeListings = auctionManager.getActiveListings().stream()
                .filter(listing -> {
                    ItemStack item = auctionManager.deserializeItem(listing.getSerializedItem());
                    return category.matches(item);
                })
                .sorted((a, b) -> {
                    return switch (sort) {
                        case NEWEST -> Long.compare(b.getExpireTime(), a.getExpireTime());
                        case EXPIRING_SOON -> Long.compare(a.getExpireTime(), b.getExpireTime());
                        case HIGHEST_PRICE -> Integer.compare(b.getPriceShards(), a.getPriceShards());
                        case LOWEST_PRICE -> Integer.compare(a.getPriceShards(), b.getPriceShards());
                    };
                })
                .collect(Collectors.toList());

        int totalItems = activeListings.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalItems / ITEMS_PER_PAGE));

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

        // Filters
        inv.setItem(48, createFilterItem(category));
        inv.setItem(49, createSortItem(sort));

        // Help Book
        inv.setItem(47, createHelpBook());

        // My Listings Head
        inv.setItem(51, createPlayerHead(player));

        // Navigation
        if (page > 0) inv.setItem(45, createDecoPane(Material.ARROW, "§a§l← Previous Page"));
        if (page < totalPages - 1) inv.setItem(53, createDecoPane(Material.ARROW, "§a§lNext Page →"));

        // Items
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
                lore.add("");
                lore.add("§7⏱ Expires In: " + getExpiryColor(remaining) + formatTime(remaining));
                lore.add("§8§m                              ");
                lore.add("");
                lore.add("§a▸ Left-click to purchase");

                displayMeta.setLore(lore);
                displayMeta.getPersistentDataContainer().set(LISTING_ID_KEY, PersistentDataType.STRING, listing.getListingId());
                display.setItemMeta(displayMeta);
            }
            inv.setItem(slots[slotIndex++], display);
        }

        if (totalItems == 0) {
            inv.setItem(22, createNoListingsItem());
        }

        player.openInventory(inv);
    }

    private void openMyListingsGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, MY_AH_TITLE);
        fillAuctionBorder(inv, 54);

        List<AuctionManager.AuctionListing> myListings = auctionManager.getPlayerListings(player.getUniqueId());

        int[] slots = {
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34,
                37, 38, 39, 40, 41, 42, 43
        };

        int slotIndex = 0;
        for (AuctionManager.AuctionListing listing : myListings) {
            if (slotIndex >= slots.length) break;

            ItemStack display = auctionManager.deserializeItem(listing.getSerializedItem());
            ItemMeta meta = display.getItemMeta();
            if (meta != null) {
                List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                lore.add("");
                lore.add("§8§m                              ");
                if (listing.getPriceShards() > 0) lore.add("§7Price: §e" + formatNumber(listing.getPriceShards()) + " Shards");
                if (listing.getPriceGems() > 0)   lore.add("§7Price: §b" + formatNumber(listing.getPriceGems()) + " Gems");
                lore.add("");
                if (listing.isExpired()) {
                    lore.add("§c§lEXPIRED");
                    lore.add("§e▸ Click to recover item");
                } else {
                    lore.add("§c▸ Click to Cancel Listing");
                }
                lore.add("§8§m                              ");
                meta.setLore(lore);
                meta.getPersistentDataContainer().set(LISTING_ID_KEY, PersistentDataType.STRING, listing.getListingId());
                display.setItemMeta(meta);
            }
            inv.setItem(slots[slotIndex++], display);
        }

        inv.setItem(49, createDecoPane(Material.ARROW, "§a§l← Back to Auction House"));
        player.openInventory(inv);
    }

    private void fillAuctionBorder(Inventory inv, int size) {
        ItemStack border = createDecoPane(Material.ORANGE_STAINED_GLASS_PANE, " ");
        ItemStack corner = createDecoPane(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < size; i++) {
            if (isBorderSlot(i, size)) {
                inv.setItem(i, isCornerSlot(i, size) ? corner : border);
            }
        }
    }

    @EventHandler
    public void onAuctionClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();
        if (!title.equals(AH_TITLE) && !title.equals(MY_AH_TITLE)) return;

        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        int slot = event.getRawSlot();

        if (title.equals(MY_AH_TITLE)) {
            if (slot == 49) { openAuctionGUI(player); return; }
            ItemMeta meta = clicked.getItemMeta();
            if (meta == null) return;
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            if (pdc.has(LISTING_ID_KEY, PersistentDataType.STRING)) {
                String listingId = pdc.get(LISTING_ID_KEY, PersistentDataType.STRING);
                AuctionManager.AuctionListing listing = auctionManager.getListing(listingId);
                if (listing != null) {
                    boolean expired = listing.isExpired();
                    auctionManager.claimListing(listingId).thenRun(() -> {
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            player.getInventory().addItem(auctionManager.deserializeItem(listing.getSerializedItem()));
                            player.sendMessage(expired ? "§e§l[Kingdom] §7Listing expired. Item recovered." : "§e§l[Kingdom] §7Listing cancelled. Item returned.");
                            openMyListingsGUI(player);
                        });
                    });
                }
            }
            return;
        }

        if (title.equals(AH_TITLE)) {
            if (clicked.getType() == Material.ARROW) {
                String name = clicked.getItemMeta().getDisplayName();
                int page = playerPages.getOrDefault(player.getUniqueId(), 0);
                playerPages.put(player.getUniqueId(), name.contains("Previous") ? Math.max(0, page - 1) : page + 1);
                openAuctionGUI(player);
                return;
            }
            if (slot == 48) {
                ItemCategory category = playerCategories.getOrDefault(player.getUniqueId(), ItemCategory.ALL).next();
                playerCategories.put(player.getUniqueId(), category);
                playerPages.put(player.getUniqueId(), 0);
                openAuctionGUI(player);
                return;
            }
            if (slot == 49) {
                SortType sort = playerSorts.getOrDefault(player.getUniqueId(), SortType.NEWEST).next();
                playerSorts.put(player.getUniqueId(), sort);
                playerPages.put(player.getUniqueId(), 0);
                openAuctionGUI(player);
                return;
            }
            if (slot == 51) { openMyListingsGUI(player); return; }
            if (slot < 9 || slot > 44 || isBorderSlot(slot, 54)) return;

            ItemMeta meta = clicked.getItemMeta();
            if (meta == null) return;
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            String listingId = pdc.get(LISTING_ID_KEY, PersistentDataType.STRING);
            AuctionManager.AuctionListing listing = auctionManager.getListing(listingId);
            if (listing == null) { player.sendMessage("§c§l[Kingdom] §7This listing no longer exists!"); openAuctionGUI(player); return; }
            if (listing.getSeller().equals(player.getUniqueId())) { player.sendMessage("§c§l[Kingdom] §7You can't buy your own listing!"); return; }

            UUID uuid = player.getUniqueId();
            if (listing.getPriceShards() > 0 && economyManager.getShards(uuid) < listing.getPriceShards()) { player.sendMessage("§c§l[Kingdom] §7Not enough Shards!"); return; }
            if (listing.getPriceGems() > 0 && economyManager.getGems(uuid) < listing.getPriceGems()) { player.sendMessage("§c§l[Kingdom] §7Not enough Gems!"); return; }

            if (listing.getPriceShards() > 0) economyManager.removeShards(uuid, listing.getPriceShards());
            if (listing.getPriceGems() > 0) economyManager.removeGems(uuid, listing.getPriceGems());

            ItemStack purchased = auctionManager.deserializeItem(listing.getSerializedItem());
            if (player.getInventory().firstEmpty() != -1) { player.getInventory().addItem(purchased); }
            else { player.getWorld().dropItemNaturally(player.getLocation(), purchased); player.sendMessage("§e§l[Kingdom] §7Inventory full! Item dropped."); }

            economyManager.addShards(listing.getSeller(), listing.getPriceShards());
            if (listing.getPriceGems() > 0) economyManager.addGems(listing.getSeller(), listing.getPriceGems());

            Player seller = Bukkit.getPlayer(listing.getSeller());
            if (seller != null && seller.isOnline()) seller.sendMessage("§a§l[Kingdom] §7Your auction was purchased by §f" + player.getName() + "§7! §a+" + listing.getPriceShards() + " Shards");

            auctionManager.removeListing(listingId);
            player.sendMessage("§a§l[Kingdom] §7Purchase successful!");
            openAuctionGUI(player);
        }
    }

    // Helper creators to avoid duplication and clutter
    private ItemStack createFilterItem(ItemCategory cat) {
        ItemStack item = new ItemStack(cat.getIcon());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§b§lCategory: §3" + cat.getDisplayName());
            meta.setLore(Arrays.asList("§7Current: " + cat.getDisplayName(), "", "§e▸ Click to cycle categories"));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createSortItem(SortType sort) {
        ItemStack item = new ItemStack(Material.HOPPER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§e§lSort: §6" + sort.getDisplayName());
            meta.setLore(Arrays.asList("§7Current: " + sort.getDisplayName(), "", "§a▸ Click to cycle sorting options"));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createHelpBook() {
        ItemStack item = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§e§l? How to Sell");
            meta.setLore(Arrays.asList("§8§m                              ", "", "§7§l1. §fHold item to sell", "§7§l2. §fType §a/ah sell <shards> [gems] [time]", "", "§7Example: §e/ah sell 100 5 12h", "§7Default expiry: §e24 hours", "§8§m                              "));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createPlayerHead(Player p) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§e§lManage My Listings");
            meta.setLore(Arrays.asList("§7Click to view and cancel", "§7your active auctions."));
            meta.setOwningPlayer(p);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createNoListingsItem() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§c§lNo Listings");
            meta.setLore(Arrays.asList("§7There are no active listings.", "§7Be the first to list an item!"));
            item.setItemMeta(meta);
        }
        return item;
    }

    private boolean isBorderSlot(int slot, int size) {
        int row = slot / 9, col = slot % 9, maxRow = (size / 9) - 1;
        return row == 0 || row == maxRow || col == 0 || col == 8;
    }

    private boolean isCornerSlot(int slot, int size) {
        int maxRow = (size / 9) - 1;
        return (slot == 0 || slot == 8 || slot == maxRow * 9 || slot == maxRow * 9 + 8);
    }

    private ItemStack createDecoPane(Material material, String name) {
        ItemStack pane = new ItemStack(material);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) { meta.setDisplayName(name); pane.setItemMeta(meta); }
        return pane;
    }

    private String getExpiryColor(long remaining) {
        if (remaining <= 0) return "§c";
        if (remaining < TimeUnit.HOURS.toMillis(1)) return "§c";
        if (remaining < TimeUnit.HOURS.toMillis(6)) return "§e";
        return "§a";
    }

    private String formatTime(long r) {
        if (r <= 0) return "Expired";
        long d = TimeUnit.MILLISECONDS.toDays(r), h = TimeUnit.MILLISECONDS.toHours(r) % 24, m = TimeUnit.MILLISECONDS.toMinutes(r) % 60, s = TimeUnit.MILLISECONDS.toSeconds(r) % 60;
        if (d > 0) return String.format("%dd %dh %dm", d, h, m);
        if (h > 0) return String.format("%dh %dm %ds", h, m, s);
        return String.format("%dm %ds", m, s);
    }

    private String formatNumber(int number) {
        return (number >= 1000) ? String.format("%,d", number) : String.valueOf(number);
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) return List.of("sell");
        if (args.length == 2 && args[0].equalsIgnoreCase("sell")) return List.of("<shards>");
        if (args.length == 3 && args[0].equalsIgnoreCase("sell")) return List.of("<gems>", "24h", "12h", "1d");
        if (args.length == 4 && args[0].equalsIgnoreCase("sell")) return List.of("24h", "12h", "1d");
        return List.of();
    }
}
