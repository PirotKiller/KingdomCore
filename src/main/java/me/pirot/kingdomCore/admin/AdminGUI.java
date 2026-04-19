package me.pirot.kingdomCore.admin;

import me.pirot.kingdomCore.KingdomCore;
import me.pirot.kingdomCore.auction.AuctionManager;
import me.pirot.kingdomCore.bounty.BountyManager;
import me.pirot.kingdomCore.database.PlayerData;
import me.pirot.kingdomCore.economy.EconomyManager;
import me.pirot.kingdomCore.rpg.ClassManager;
import me.pirot.kingdomCore.rpg.RPGClass;
import me.pirot.kingdomCore.rpg.SpecialItems;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
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

import java.util.*;
import java.util.stream.Collectors;

/**
 * In-game admin panel GUI system.
 * Provides a comprehensive chest-based dashboard for server management.
 */
public class AdminGUI implements Listener {

    private final KingdomCore plugin;
    private final EconomyManager economyManager;
    private final ClassManager classManager;
    private final BountyManager bountyManager;
    private final AuctionManager auctionManager;
    private final SpecialItems specialItems;

    // GUI titles
    public static final String MAIN_TITLE = "§8§l[ §4§lAdmin Panel §8§l]";
    public static final String PLAYERS_TITLE = "§8§l[ §e§lPlayer Manager §8§l]";
    public static final String PLAYER_PROFILE_PREFIX = "§8§l[ §b§lProfile: ";
    public static final String ECONOMY_TITLE = "§8§l[ §a§lEconomy Control §8§l]";
    public static final String CLASS_TITLE = "§8§l[ §d§lClass Manager §8§l]";
    public static final String BOUNTY_TITLE = "§8§l[ §6§lBounty Overview §8§l]";
    public static final String AUCTION_TITLE = "§8§l[ §e§lAuction Mod §8§l]";
    public static final String CONFIG_TITLE = "§8§l[ §c§lConfig Editor §8§l]";

    // Track which player profile is being viewed
    private final Map<UUID, UUID> viewingProfile = new HashMap<>();

    public AdminGUI(KingdomCore plugin, EconomyManager economyManager,
                    ClassManager classManager, BountyManager bountyManager,
                    AuctionManager auctionManager, SpecialItems specialItems) {
        this.plugin = plugin;
        this.economyManager = economyManager;
        this.classManager = classManager;
        this.bountyManager = bountyManager;
        this.auctionManager = auctionManager;
        this.specialItems = specialItems;
    }

    // ============================================================
    // MAIN DASHBOARD
    // ============================================================

    public void openMainDashboard(Player admin) {
        Inventory inv = Bukkit.createInventory(null, 54, MAIN_TITLE);
        fillBorder(inv, Material.RED_STAINED_GLASS_PANE);

        // Dashboard Stats (Top Center)
        int onlinePlayers = Bukkit.getOnlinePlayers().size();
        int totalPlayers = economyManager.getCache().size();
        long totalShards = economyManager.getCache().values().stream().mapToLong(PlayerData::getShards).sum();
        long totalGems = economyManager.getCache().values().stream().mapToLong(PlayerData::getGems).sum();
        int activeListings = auctionManager.getActiveListings().size();

        // Class distribution
        Map<String, Integer> classDist = new HashMap<>();
        for (PlayerData data : economyManager.getCache().values()) {
            classDist.merge(data.getClassName(), 1, Integer::sum);
        }

        inv.setItem(4, createItem(Material.GOLD_BLOCK, "§6§l✦ Server Dashboard ✦", Arrays.asList(
                "§8§m                              ",
                "",
                "§7Online: §a" + onlinePlayers + " §7/ Total: §e" + totalPlayers,
                "§7Total Shards: §a" + String.format("%,d", totalShards),
                "§7Total Gems: §b" + String.format("%,d", totalGems),
                "§7Active Auctions: §e" + activeListings,
                "",
                "§7Class Distribution:",
                "§6  Archer: §f" + classDist.getOrDefault("ARCHER", 0),
                "§b  Knight: §f" + classDist.getOrDefault("KNIGHT", 0),
                "§5  Wizard: §f" + classDist.getOrDefault("WIZARD", 0),
                "§c  Rogue: §f" + classDist.getOrDefault("ROGUE", 0),
                "§e  Ronin: §f" + classDist.getOrDefault("RONIN", 0),
                "§7  None: §8" + classDist.getOrDefault("NONE", 0),
                "",
                "§8§m                              "
        )));

        // Category Buttons
        inv.setItem(20, createItem(Material.PLAYER_HEAD, "§e§lPlayer Manager",
                Arrays.asList("§7Browse and manage all players.", "", "§a▸ Click to open")));

        inv.setItem(21, createItem(Material.EMERALD, "§a§lEconomy Control",
                Arrays.asList("§7Manage shards & gems.", "§7Bulk operations.", "", "§a▸ Click to open")));

        inv.setItem(22, createItem(Material.DIAMOND_SWORD, "§d§lClass Manager",
                Arrays.asList("§7Force-set classes.", "§7View level distribution.", "", "§a▸ Click to open")));

        inv.setItem(23, createItem(Material.COMPASS, "§6§lBounty Overview",
                Arrays.asList("§7View all active bounties.", "§7Place server bounties.", "", "§a▸ Click to open")));

        inv.setItem(24, createItem(Material.CHEST, "§e§lAuction Moderation",
                Arrays.asList("§7View all listings.", "§7Remove items, return to sellers.", "", "§a▸ Click to open")));

        inv.setItem(30, createItem(Material.REDSTONE, "§c§lConfig Editor",
                Arrays.asList("§7Edit XP, weapon, bounty values.", "§7Changes apply live.", "", "§a▸ Click to open")));

        inv.setItem(31, createItem(Material.TNT, "§c§l⚠ Server Reset",
                Arrays.asList("§7Reset all player data.", "§cThis action is irreversible!", "", "§c▸ Click for /reset")));

        inv.setItem(32, createItem(Material.NETHER_STAR, "§b§lGive Special Items",
                Arrays.asList("§7Give yourself special items:", "§7Soul Fragment, Class Scroll, etc.", "", "§a▸ Click to open")));

        admin.openInventory(inv);
    }

    // ============================================================
    // PLAYER MANAGER
    // ============================================================

    public void openPlayerManager(Player admin) {
        Inventory inv = Bukkit.createInventory(null, 54, PLAYERS_TITLE);
        fillBorder(inv, Material.YELLOW_STAINED_GLASS_PANE);

        // Sort players by online first, then alphabetical
        List<PlayerData> players = new ArrayList<>(economyManager.getCache().values());
        players.sort((a, b) -> {
            boolean aOnline = Bukkit.getPlayer(a.getUuid()) != null;
            boolean bOnline = Bukkit.getPlayer(b.getUuid()) != null;
            if (aOnline != bOnline) return bOnline ? 1 : -1;
            return a.getLastKnownName().compareToIgnoreCase(b.getLastKnownName());
        });

        int[] slots = {
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34,
                37, 38, 39, 40, 41, 42, 43
        };

        int slotIdx = 0;
        for (PlayerData data : players) {
            if (slotIdx >= slots.length) break;

            boolean isOnline = Bukkit.getPlayer(data.getUuid()) != null;
            RPGClass rpgClass = RPGClass.fromString(data.getClassName());

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            if (meta != null) {
                OfflinePlayer offPlayer = Bukkit.getOfflinePlayer(data.getUuid());
                meta.setOwningPlayer(offPlayer);
                meta.setDisplayName((isOnline ? "§a" : "§7") + data.getLastKnownName());
                meta.setLore(Arrays.asList(
                        "§7Class: " + (rpgClass != null ? rpgClass.getColoredName() : "§8None"),
                        "§7Level: §e" + data.getLevel() + " §8(XP: " + data.getXp() + ")",
                        "§7Shards: §a" + data.getShards() + " §7| Gems: §b" + data.getGems(),
                        "§7Bounty: §c" + data.getBounty(),
                        "§7K/D: §f" + data.getKills() + "/" + data.getDeaths(),
                        "",
                        "§a▸ Click to manage"
                ));
                head.setItemMeta(meta);
            }
            inv.setItem(slots[slotIdx++], head);
        }

        // Back button
        inv.setItem(49, createItem(Material.ARROW, "§a§l← Back to Admin Panel", null));

        admin.openInventory(inv);
    }

    // ============================================================
    // PLAYER PROFILE
    // ============================================================

    public void openPlayerProfile(Player admin, UUID targetUUID) {
        PlayerData data = economyManager.getPlayerData(targetUUID);
        if (data == null) {
            admin.sendMessage("§c§l[Admin] §7Player data not found!");
            return;
        }

        viewingProfile.put(admin.getUniqueId(), targetUUID);

        String title = PLAYER_PROFILE_PREFIX + data.getLastKnownName() + " §8§l]";
        // Title max length is 32, truncate if needed
        if (title.length() > 32) title = title.substring(0, 32);

        Inventory inv = Bukkit.createInventory(null, 54, title);
        fillBorder(inv, Material.LIGHT_BLUE_STAINED_GLASS_PANE);

        RPGClass rpgClass = RPGClass.fromString(data.getClassName());

        // Player Head (center top)
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta headMeta = (SkullMeta) head.getItemMeta();
        if (headMeta != null) {
            headMeta.setOwningPlayer(Bukkit.getOfflinePlayer(targetUUID));
            headMeta.setDisplayName("§b§l" + data.getLastKnownName());
            headMeta.setLore(Arrays.asList(
                    "§7UUID: §8" + targetUUID.toString().substring(0, 8) + "...",
                    "§7Status: " + (Bukkit.getPlayer(targetUUID) != null ? "§aOnline" : "§cOffline")
            ));
            head.setItemMeta(headMeta);
        }
        inv.setItem(4, head);

        // Stats display
        inv.setItem(19, createItem(Material.DIAMOND_SWORD, "§d§lClass: " + (rpgClass != null ? rpgClass.getColoredName() : "§8None"),
                Arrays.asList("§7Level: §e" + data.getLevel(), "§7XP: §e" + data.getXp() + "/" + data.getXpNeeded(), "", "§a▸ Click to change class")));

        inv.setItem(20, createItem(Material.GOLD_INGOT, "§a§lShards: §f" + String.format("%,d", data.getShards()),
                Arrays.asList("§a▸ Left-click: +1000", "§c▸ Right-click: -1000")));

        inv.setItem(21, createItem(Material.EMERALD, "§b§lGems: §f" + String.format("%,d", data.getGems()),
                Arrays.asList("§a▸ Left-click: +100", "§c▸ Right-click: -100")));

        inv.setItem(22, createItem(Material.EXPERIENCE_BOTTLE, "§e§lLevel: §f" + data.getLevel(),
                Arrays.asList("§7XP: " + data.getXp() + "/" + data.getXpNeeded(), "", "§a▸ Left-click: +1 Level", "§c▸ Right-click: -1 Level")));

        inv.setItem(23, createItem(Material.COMPASS, "§6§lBounty: §f" + String.format("%,d", data.getBounty()),
                Arrays.asList("§a▸ Left-click: +500", "§c▸ Right-click: Reset to 0")));

        inv.setItem(24, createItem(Material.IRON_SWORD, "§7§lKills: §f" + data.getKills() + " §7| Deaths: §f" + data.getDeaths(),
                Arrays.asList("§a▸ Left-click: Reset K/D")));

        inv.setItem(25, createItem(Material.BARRIER, "§c§lReset Player",
                Arrays.asList("§7Reset all data for this player.", "§7Preserves gems.", "", "§c▸ Click to reset")));

        // Give items
        inv.setItem(38, createItem(Material.NETHER_STAR, "§b§lGive Soul Fragment",
                Arrays.asList("§7Give 1 Soul Fragment to this player.", "", "§a▸ Click to give")));

        inv.setItem(39, createItem(Material.PAPER, "§6§lGive Class Change Scroll",
                Arrays.asList("§7Give 1 Class Change Scroll.", "", "§a▸ Click to give")));

        inv.setItem(40, createItem(Material.ENCHANTED_BOOK, "§5§lGive Data Restore Tome",
                Arrays.asList("§7Give 1 Data Restore Tome.", "", "§a▸ Click to give")));

        // Back button
        inv.setItem(49, createItem(Material.ARROW, "§a§l← Back to Player Manager", null));

        admin.openInventory(inv);
    }

    // ============================================================
    // ECONOMY CONTROL
    // ============================================================

    public void openEconomyControl(Player admin) {
        Inventory inv = Bukkit.createInventory(null, 54, ECONOMY_TITLE);
        fillBorder(inv, Material.LIME_STAINED_GLASS_PANE);

        // Richest players
        List<PlayerData> richest = economyManager.getCache().values().stream()
                .sorted(Comparator.comparingInt(PlayerData::getShards).reversed())
                .limit(10)
                .collect(Collectors.toList());

        inv.setItem(4, createItem(Material.GOLD_BLOCK, "§a§l✦ Economy Dashboard ✦", Arrays.asList(
                "§8§m                              ",
                "§7Top 10 Richest Players:",
                ""
        )));

        int[] slots = {19, 20, 21, 22, 23, 24, 25, 28, 29, 30};
        for (int i = 0; i < richest.size() && i < slots.length; i++) {
            PlayerData d = richest.get(i);
            inv.setItem(slots[i], createItem(Material.GOLD_INGOT,
                    "§6#" + (i + 1) + " §f" + d.getLastKnownName(),
                    Arrays.asList("§7Shards: §a" + String.format("%,d", d.getShards()),
                            "§7Gems: §b" + String.format("%,d", d.getGems()))));
        }

        // Bulk operations
        inv.setItem(39, createItem(Material.EMERALD_BLOCK, "§a§lGive All +1000 Shards",
                Arrays.asList("§7Give 1000 shards to all online players.", "", "§a▸ Click to execute")));

        inv.setItem(40, createItem(Material.DIAMOND_BLOCK, "§b§lGive All +100 Gems",
                Arrays.asList("§7Give 100 gems to all online players.", "", "§a▸ Click to execute")));

        inv.setItem(49, createItem(Material.ARROW, "§a§l← Back to Admin Panel", null));

        admin.openInventory(inv);
    }

    // ============================================================
    // BOUNTY OVERVIEW
    // ============================================================

    public void openBountyOverview(Player admin) {
        Inventory inv = Bukkit.createInventory(null, 54, BOUNTY_TITLE);
        fillBorder(inv, Material.ORANGE_STAINED_GLASS_PANE);

        List<PlayerData> withBounty = economyManager.getCache().values().stream()
                .filter(d -> d.getBounty() > 0)
                .sorted(Comparator.comparingInt(PlayerData::getBounty).reversed())
                .collect(Collectors.toList());

        inv.setItem(4, createItem(Material.COMPASS, "§6§l✦ Active Bounties ✦",
                Arrays.asList("§7Total bounties: §e" + withBounty.size())));

        int[] slots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};
        for (int i = 0; i < withBounty.size() && i < slots.length; i++) {
            PlayerData d = withBounty.get(i);
            inv.setItem(slots[i], createItem(Material.GOLD_NUGGET,
                    "§c" + d.getLastKnownName() + " §7— §a" + d.getBounty() + " Shards",
                    Arrays.asList("§c▸ Click to remove bounty")));
        }

        // Place server bounty
        inv.setItem(48, createItem(Material.GOLD_BLOCK, "§6§lPlace Server Bounty",
                Arrays.asList("§7Add 1000 shards to the top player's bounty.", "", "§a▸ Click to place")));

        inv.setItem(49, createItem(Material.ARROW, "§a§l← Back to Admin Panel", null));

        admin.openInventory(inv);
    }

    // ============================================================
    // SPECIAL ITEMS MENU
    // ============================================================

    public void openSpecialItemsMenu(Player admin) {
        Inventory inv = Bukkit.createInventory(null, 27, "§8§l[ §b§lSpecial Items §8§l]");
        fillBorder(inv, Material.CYAN_STAINED_GLASS_PANE);

        inv.setItem(11, specialItems.createSoulItem());
        inv.setItem(13, specialItems.createClassChangeScroll());
        inv.setItem(15, specialItems.createDataRestoreTome());

        admin.openInventory(inv);
    }

    // ============================================================
    // CLICK HANDLER
    // ============================================================

    @EventHandler
    public void onAdminClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player admin)) return;
        String title = event.getView().getTitle();

        // Check if it's an admin GUI
        if (!title.equals(MAIN_TITLE) && !title.equals(PLAYERS_TITLE) &&
                !title.startsWith(PLAYER_PROFILE_PREFIX) && !title.equals(ECONOMY_TITLE) &&
                !title.equals(BOUNTY_TITLE) && !title.equals("§8§l[ §b§lSpecial Items §8§l]")) return;

        if (!admin.hasPermission("kingdomcore.admin")) return;

        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        int slot = event.getRawSlot();

        // ---- MAIN DASHBOARD ----
        if (title.equals(MAIN_TITLE)) {
            switch (slot) {
                case 20 -> openPlayerManager(admin);
                case 21 -> openEconomyControl(admin);
                case 22 -> admin.sendMessage("§e§l[Admin] §7Use /class <player> <class> to force-set classes.");
                case 23 -> openBountyOverview(admin);
                case 24 -> admin.sendMessage("§e§l[Admin] §7Auction moderation coming soon.");
                case 30 -> admin.sendMessage("§e§l[Admin] §7Use config.yml for now. Live editor coming soon.");
                case 31 -> { admin.closeInventory(); admin.performCommand("reset"); }
                case 32 -> openSpecialItemsMenu(admin);
            }
            return;
        }

        // ---- PLAYER MANAGER ----
        if (title.equals(PLAYERS_TITLE)) {
            if (slot == 49) { openMainDashboard(admin); return; }
            if (clicked.getType() == Material.PLAYER_HEAD && clicked.hasItemMeta()) {
                String name = clicked.getItemMeta().getDisplayName().replaceAll("§[a-f0-9]", "");
                Player target = Bukkit.getPlayerExact(name);
                if (target != null) {
                    openPlayerProfile(admin, target.getUniqueId());
                } else {
                    // Find by name in cache
                    for (PlayerData data : economyManager.getCache().values()) {
                        if (data.getLastKnownName().equalsIgnoreCase(name)) {
                            openPlayerProfile(admin, data.getUuid());
                            return;
                        }
                    }
                    admin.sendMessage("§c§l[Admin] §7Player not found in cache.");
                }
            }
            return;
        }

        // ---- PLAYER PROFILE ----
        if (title.startsWith(PLAYER_PROFILE_PREFIX)) {
            UUID targetUUID = viewingProfile.get(admin.getUniqueId());
            if (targetUUID == null) return;
            PlayerData data = economyManager.getPlayerData(targetUUID);
            if (data == null) return;

            switch (slot) {
                case 19 -> { // Change class
                    RPGClass current = RPGClass.fromString(data.getClassName());
                    RPGClass[] classes = RPGClass.values();
                    int idx = current != null ? (current.ordinal() + 1) % classes.length : 0;
                    data.setClassName(classes[idx].name());
                    admin.sendMessage("§a§l[Admin] §7Set class to " + classes[idx].getColoredName());
                    Player target = Bukkit.getPlayer(targetUUID);
                    if (target != null) classManager.applyPassives(target);
                    openPlayerProfile(admin, targetUUID);
                }
                case 20 -> { // Shards
                    if (event.isLeftClick()) { data.addShards(1000); admin.sendMessage("§a+1000 Shards"); }
                    else { data.removeShards(1000); admin.sendMessage("§c-1000 Shards"); }
                    openPlayerProfile(admin, targetUUID);
                }
                case 21 -> { // Gems
                    if (event.isLeftClick()) { data.addGems(100); admin.sendMessage("§a+100 Gems"); }
                    else { data.removeGems(100); admin.sendMessage("§c-100 Gems"); }
                    openPlayerProfile(admin, targetUUID);
                }
                case 22 -> { // Level
                    if (event.isLeftClick()) { data.setLevel(data.getLevel() + 1); admin.sendMessage("§a+1 Level"); }
                    else if (data.getLevel() > 1) { data.setLevel(data.getLevel() - 1); admin.sendMessage("§c-1 Level"); }
                    openPlayerProfile(admin, targetUUID);
                }
                case 23 -> { // Bounty
                    if (event.isLeftClick()) { data.addBounty(500); admin.sendMessage("§a+500 Bounty"); }
                    else { data.setBounty(0); admin.sendMessage("§cBounty reset"); }
                    openPlayerProfile(admin, targetUUID);
                }
                case 24 -> { // Reset K/D
                    data.setKills(0); data.setDeaths(0);
                    admin.sendMessage("§c§l[Admin] §7K/D reset.");
                    openPlayerProfile(admin, targetUUID);
                }
                case 25 -> { // Full reset
                    data.setXp(0); data.setLevel(1); data.setShards(0);
                    data.setBounty(0); data.setKills(0); data.setDeaths(0);
                    admin.sendMessage("§c§l[Admin] §7Player fully reset (gems preserved).");
                    openPlayerProfile(admin, targetUUID);
                }
                case 38 -> { // Give Soul Fragment
                    Player target = Bukkit.getPlayer(targetUUID);
                    if (target != null) { target.getInventory().addItem(specialItems.createSoulItem()); admin.sendMessage("§a§l[Admin] §7Gave Soul Fragment."); }
                    else admin.sendMessage("§c§l[Admin] §7Player must be online.");
                }
                case 39 -> { // Give Class Change Scroll
                    Player target = Bukkit.getPlayer(targetUUID);
                    if (target != null) { target.getInventory().addItem(specialItems.createClassChangeScroll()); admin.sendMessage("§a§l[Admin] §7Gave Class Change Scroll."); }
                    else admin.sendMessage("§c§l[Admin] §7Player must be online.");
                }
                case 40 -> { // Give Data Restore Tome
                    Player target = Bukkit.getPlayer(targetUUID);
                    if (target != null) { target.getInventory().addItem(specialItems.createDataRestoreTome()); admin.sendMessage("§a§l[Admin] §7Gave Data Restore Tome."); }
                    else admin.sendMessage("§c§l[Admin] §7Player must be online.");
                }
                case 49 -> openPlayerManager(admin);
            }
            return;
        }

        // ---- ECONOMY CONTROL ----
        if (title.equals(ECONOMY_TITLE)) {
            if (slot == 49) { openMainDashboard(admin); return; }
            if (slot == 39) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    economyManager.addShards(p.getUniqueId(), 1000);
                    p.sendMessage("§a§l[Kingdom] §7Admin granted you §a1000 Shards§7!");
                }
                admin.sendMessage("§a§l[Admin] §7Gave 1000 shards to all online players.");
                admin.playSound(admin.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
            }
            if (slot == 40) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    economyManager.addGems(p.getUniqueId(), 100);
                    p.sendMessage("§a§l[Kingdom] §7Admin granted you §b100 Gems§7!");
                }
                admin.sendMessage("§a§l[Admin] §7Gave 100 gems to all online players.");
                admin.playSound(admin.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
            }
            return;
        }

        // ---- BOUNTY OVERVIEW ----
        if (title.equals(BOUNTY_TITLE)) {
            if (slot == 49) { openMainDashboard(admin); return; }
            if (slot == 48) {
                Player topPlayer = bountyManager.getTopBountyPlayer();
                if (topPlayer != null) {
                    PlayerData data = economyManager.getPlayerData(topPlayer.getUniqueId());
                    if (data != null) {
                        data.addBounty(1000);
                        Bukkit.broadcastMessage("§6§l[Bounty] §7A §cserver bounty §7of §a1000 Shards §7has been placed on §f" + topPlayer.getName() + "§7!");
                        admin.sendMessage("§a§l[Admin] §7Server bounty placed.");
                    }
                } else {
                    admin.sendMessage("§c§l[Admin] §7No players have bounties.");
                }
                openBountyOverview(admin);
                return;
            }
            // Click to remove bounty
            if (clicked.getType() == Material.GOLD_NUGGET && clicked.hasItemMeta()) {
                String displayName = clicked.getItemMeta().getDisplayName();
                String name = displayName.split(" §7—")[0].replaceAll("§[a-f0-9]", "");
                for (PlayerData data : economyManager.getCache().values()) {
                    if (data.getLastKnownName().equalsIgnoreCase(name)) {
                        data.setBounty(0);
                        admin.sendMessage("§a§l[Admin] §7Removed bounty from " + name);
                        break;
                    }
                }
                openBountyOverview(admin);
            }
            return;
        }

        // ---- SPECIAL ITEMS (just let them take items) ----
        if (title.equals("§8§l[ §b§lSpecial Items §8§l]")) {
            if (clicked.hasItemMeta() && specialItems.isSpecialItem(clicked)) {
                admin.getInventory().addItem(clicked.clone());
                admin.sendMessage("§a§l[Admin] §7Item added to your inventory.");
                admin.playSound(admin.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1f, 1f);
            }
        }
    }

    // ============================================================
    // HELPERS
    // ============================================================

    private void fillBorder(Inventory inv, Material borderMat) {
        ItemStack border = createPane(borderMat);
        ItemStack corner = createPane(Material.BLACK_STAINED_GLASS_PANE);
        int size = inv.getSize();
        for (int i = 0; i < size; i++) {
            int row = i / 9, col = i % 9, maxRow = (size / 9) - 1;
            if (row == 0 || row == maxRow || col == 0 || col == 8) {
                inv.setItem(i, (i == 0 || i == 8 || i == maxRow * 9 || i == maxRow * 9 + 8) ? corner : border);
            }
        }
    }

    private ItemStack createPane(Material m) {
        ItemStack item = new ItemStack(m);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) { meta.setDisplayName(" "); item.setItemMeta(meta); }
        return item;
    }

    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null) meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
}
