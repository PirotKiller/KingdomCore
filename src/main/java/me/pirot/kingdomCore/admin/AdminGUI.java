package me.pirot.kingdomCore.admin;

import me.pirot.kingdomCore.KingdomCore;
import me.pirot.kingdomCore.auction.AuctionManager;
import me.pirot.kingdomCore.bounty.BountyManager;
import me.pirot.kingdomCore.database.PlayerData;
import me.pirot.kingdomCore.economy.EconomyManager;
import me.pirot.kingdomCore.rpg.ClassManager;
import me.pirot.kingdomCore.rpg.RPGClass;
import me.pirot.kingdomCore.rpg.SpecialItems;
import me.pirot.kingdomCore.rpg.WeaponTier;
import me.pirot.kingdomCore.rpg.WeaponManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Premium In-game admin panel GUI system.
 * Provides a high-fidelity, comprehensive dashboard for server management.
 */
public class AdminGUI implements Listener {

    private final KingdomCore plugin;
    private final EconomyManager economyManager;
    private final ClassManager classManager;
    private final BountyManager bountyManager;
    private final AuctionManager auctionManager;
    private final SpecialItems specialItems;

    // GUI titles
    public static final String MAIN_TITLE = "§8§l[ §4§lAdmin Dashboard §8§l]";
    public static final String PLAYERS_TITLE = "§8§l[ §e§lPlayer Manager §8§l]";
    public static final String PLAYER_PROFILE_PREFIX = "§8§l[ §b§lProfile: ";
    public static final String ECONOMY_TITLE = "§8§l[ §a§lEconomy Control §8§l]";
    public static final String CLASS_TITLE = "§8§l[ §d§lClass Management §8§l]";
    public static final String BOUNTY_TITLE = "§8§l[ §6§lBounty Overview §8§l]";
    public static final String AUCTION_TITLE = "§8§l[ §e§lAuction Moderation §8§l]";
    public static final String WEAPONS_TITLE = "§8§l[ §d§lWeapon Arsenal §8§l]";
    public static final String SPECIAL_ITEMS_TITLE = "§8§l[ §b§lSpecial Items §8§l]";
    public static final String LOGS_TITLE = "§8§l[ §f§lRecent Logs §8§l]";

    private final Map<UUID, UUID> viewingProfile = new HashMap<>();
    private final Map<UUID, Integer> weaponPages = new HashMap<>();
    private final Map<UUID, Integer> logPages = new HashMap<>();
    
    private final NamespacedKey AUCTION_ID_KEY;

    public AdminGUI(KingdomCore plugin, EconomyManager economyManager,
                    ClassManager classManager, BountyManager bountyManager,
                    AuctionManager auctionManager, SpecialItems specialItems) {
        this.plugin = plugin;
        this.economyManager = economyManager;
        this.classManager = classManager;
        this.bountyManager = bountyManager;
        this.auctionManager = auctionManager;
        this.specialItems = specialItems;
        this.AUCTION_ID_KEY = new NamespacedKey(plugin, "auction_id");
    }

    // ============================================================
    // MAIN DASHBOARD
    // ============================================================

    public void openMainDashboard(Player admin) {
        Inventory inv = Bukkit.createInventory(null, 54, MAIN_TITLE);
        
        // Premium Checkered Border
        fillBorder(inv, Material.GRAY_STAINED_GLASS_PANE, Material.BLACK_STAINED_GLASS_PANE);
        inv.setItem(0, createPane(Material.RED_STAINED_GLASS_PANE));
        inv.setItem(8, createPane(Material.RED_STAINED_GLASS_PANE));
        inv.setItem(45, createPane(Material.RED_STAINED_GLASS_PANE));
        inv.setItem(53, createPane(Material.RED_STAINED_GLASS_PANE));

        // Stats Header
        int onlinePlayers = Bukkit.getOnlinePlayers().size();
        int totalPlayers = economyManager.getCache().size();
        long totalShards = economyManager.getCache().values().stream().mapToLong(PlayerData::getShards).sum();
        long totalGems = economyManager.getCache().values().stream().mapToLong(PlayerData::getGems).sum();

        inv.setItem(4, createItem(Material.BEACON, "§6§lKingdom Dashboard §7v1.0", Arrays.asList(
                "§8§m                              ",
                "§7Online: §a" + onlinePlayers + " §8/ §7Total: §e" + totalPlayers,
                "§7Economy Shards: §a" + String.format("%,d", totalShards),
                "§7Economy Gems: §b" + String.format("%,d", totalGems),
                "§8§m                              ",
                "§eAdministrative Override Access"
        )));

        // --- ROW 2: MANAGEMENT ---
        inv.setItem(19, createItem(Material.PLAYER_HEAD, "§e§lPlayer Manager", Arrays.asList("§7Browse and manage all players.", "§7Edit currency, level, and stats.", "§8§m                              ", "§a▸ Click to open")));
        inv.setItem(20, createItem(Material.NETHER_STAR, "§d§lClass Manager", Arrays.asList("§7Force-set classes and monitor xp.", "§7View class distributions.", "§8§m                              ", "§a▸ Click to open")));
        inv.setItem(21, createItem(Material.ENDER_CHEST, "§6§lAuction Moderation", Arrays.asList("§7Global marketplace supervision.", "§7Cancel listings or clear logs.", "§8§m                              ", "§a▸ Click to open")));
        
        // --- ROW 3: ECONOMY ---
        inv.setItem(28, createItem(Material.EMERALD_BLOCK, "§a§lEconomy Control", Arrays.asList("§7Network-wide finance tools.", "§7Airdrops and mass rewards.", "§8§m                              ", "§a▸ Click to open")));
        inv.setItem(29, createItem(Material.COMPASS, "§6§lBounty Overview", Arrays.asList("§7Monitor top wanted criminals.", "§7Reset or modify bounties.", "§8§m                              ", "§a▸ Click to open")));

        // --- ROW 4: SYSTEM & TOOLS ---
        inv.setItem(37, createItem(Material.BOOKSHELF, "§f§lSystem Logs", Arrays.asList("§7View recent server activity.", "§7Audit purchases and admin actions.", "§8§m                              ", "§b▸ Click to open")));
        inv.setItem(38, createItem(Material.DIAMOND_SWORD, "§d§lWeapon Arsenal", Arrays.asList("§7Access all RPG weapon tiers.", "§7Pull items for testing.", "§8§m                              ", "§a▸ Click to open")));
        inv.setItem(39, createItem(Material.ENCHANTING_TABLE, "§b§lSpecial Items", Arrays.asList("§7Summon fragments and scrolls.", "§7Utility system items.", "§8§m                              ", "§a▸ Click to open")));
        inv.setItem(40, createItem(Material.REPEATING_COMMAND_BLOCK, "§5§lSync Configuration", Arrays.asList("§7Sync YAML shops to MongoDB.", "§7Reload configurations live.", "§8§m                              ", "§e▸ Click to Sync")));

        // --- DANGER ZONE ---
        inv.setItem(43, createItem(Material.TNT, "§c§l⚠ Server Reset", Arrays.asList("§7Wipe all network player data.", "§cWarning: This is IRREVERSIBLE!", "§8§m                              ", "§c▸ Click for /reset cmd")));

        admin.openInventory(inv);
        admin.playSound(admin.getLocation(), Sound.BLOCK_CHEST_OPEN, 1f, 1f);
    }

    // ============================================================
    // PLAYER MANAGER
    // ============================================================

    public void openPlayerManager(Player admin) {
        Inventory inv = Bukkit.createInventory(null, 54, PLAYERS_TITLE);
        fillBorder(inv, Material.YELLOW_STAINED_GLASS_PANE, Material.GRAY_STAINED_GLASS_PANE);

        List<PlayerData> players = new ArrayList<>(economyManager.getCache().values());
        players.sort((a, b) -> {
            boolean aOnline = Bukkit.getPlayer(a.getUuid()) != null;
            if (aOnline != (Bukkit.getPlayer(b.getUuid()) != null)) return aOnline ? -1 : 1;
            return a.getLastKnownName().compareToIgnoreCase(b.getLastKnownName());
        });

        int[] slots = {10,11,12,13,14,15,16,19,20,21,22,23,24,25,28,29,30,31,32,33,34,37,38,39,40,41,42,43};
        int idx = 0;
        for (PlayerData data : players) {
            if (idx >= slots.length) break;
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            if (meta != null) {
                meta.setOwningPlayer(Bukkit.getOfflinePlayer(data.getUuid()));
                boolean online = Bukkit.getPlayer(data.getUuid()) != null;
                meta.setDisplayName((online ? "§a§l● " : "§7○ ") + "§f" + data.getLastKnownName());
                List<String> lore = new ArrayList<>();
                lore.add("§8§m                              ");
                lore.add("§7Class: " + (data.getClassName().equals("NONE") ? "§7None" : "§b" + data.getClassName()));
                lore.add("§7Level: §e" + data.getLevel());
                lore.add("§7Shards: §a" + String.format("%,d", data.getShards()));
                lore.add("§8§m                              ");
                lore.add("§a▸ Click to View Profile");
                meta.setLore(lore);
                head.setItemMeta(meta);
            }
            inv.setItem(slots[idx++], head);
        }
        inv.setItem(49, createItem(Material.ARROW, "§c§l← Back", null));
        admin.openInventory(inv);
    }

    public void openPlayerProfile(Player admin, UUID targetUUID) {
        PlayerData data = economyManager.getPlayerData(targetUUID);
        if (data == null) return;
        viewingProfile.put(admin.getUniqueId(), targetUUID);
        String title = PLAYER_PROFILE_PREFIX + data.getLastKnownName() + " §8§l]";
        if (title.length() > 32) title = title.substring(0, 32);

        Inventory inv = Bukkit.createInventory(null, 54, title);
        fillBorder(inv, Material.CYAN_STAINED_GLASS_PANE, Material.GRAY_STAINED_GLASS_PANE);

        // Row 2: Editable stats
        inv.setItem(19, createItem(Material.NETHER_STAR, "§d§lRPG Class: §f" + data.getClassName(), Arrays.asList(
                "§7Cycle through available classes.", 
                "§8§m                              ", 
                "§eLeft: Next Class", 
                "§cRight: Set to NONE")));
        inv.setItem(20, createItem(Material.GOLD_INGOT, "§a§lShards: §f" + String.format("%,d", data.getShards()), Arrays.asList(
                "§7Modify shard total.", 
                "§8§m                              ", 
                "§eLeft: +1,000", 
                "§cRight: -1,000")));
        inv.setItem(21, createItem(Material.EMERALD, "§b§lGems: §f" + String.format("%,d", data.getGems()), Arrays.asList(
                "§7Modify gem total.", 
                "§8§m                              ", 
                "§eLeft: +100", 
                "§cRight: -100")));
        inv.setItem(22, createItem(Material.EXPERIENCE_BOTTLE, "§e§lLevel: §f" + data.getLevel(), Arrays.asList(
                "§7Set player level.", 
                "§8§m                              ", 
                "§eLeft: +1", 
                "§cRight: -1")));
        inv.setItem(23, createItem(Material.COMPASS, "§6§lBounty: §f" + String.format("%,d", data.getBounty()), Arrays.asList(
                "§7Set bounty reward.", 
                "§8§m                              ", 
                "§eLeft: +500", 
                "§cRight: Reset to 0")));
        inv.setItem(24, createItem(Material.IRON_SWORD, "§7§lCombat Stats", Arrays.asList(
                "§7Kills: §a" + data.getKills(), 
                "§7Deaths: §c" + data.getDeaths(), 
                "§8§m                              ", 
                "§c▸ Click to Wipe Stats")));
        inv.setItem(25, createItem(Material.BARRIER, "§c§lFull Account Reset", Arrays.asList(
                "§7Wipe ALL player progress.", 
                "§7Return to default state.", 
                "§8§m                              ", 
                "§4§lIRREVERSIBLE ACTION")));
        
        // Row 4: Give special items to this player
        inv.setItem(29, createItem(Material.NETHER_STAR, "§b§lGive Soul Fragment", Arrays.asList(
                "§7Give 1x Soul Fragment to", 
                "§7this player's inventory.", 
                "§8§m                              ", 
                "§a▸ Click to give")));
        inv.setItem(30, createItem(Material.PAPER, "§6§lGive Class Change Scroll", Arrays.asList(
                "§7Give 1x Class Change Scroll to", 
                "§7this player's inventory.", 
                "§8§m                              ", 
                "§a▸ Click to give")));
        inv.setItem(31, createItem(Material.ENCHANTED_BOOK, "§5§lGive Data Restore Tome", Arrays.asList(
                "§7Give 1x Data Restore Tome to", 
                "§7this player's inventory.", 
                "§8§m                              ", 
                "§a▸ Click to give")));
        
        inv.setItem(49, createItem(Material.ARROW, "§a§l← Back to Players", null));
        admin.openInventory(inv);
    }

    // ============================================================
    // CLASS MANAGER — Shows distribution + lets admin set any player's class
    // ============================================================

    public void openClassManager(Player admin) {
        Inventory inv = Bukkit.createInventory(null, 54, CLASS_TITLE);
        fillBorder(inv, Material.PURPLE_STAINED_GLASS_PANE, Material.GRAY_STAINED_GLASS_PANE);
        
        // Count class distributions
        Map<String, Integer> distribution = new HashMap<>();
        distribution.put("NONE", 0);
        for (RPGClass clazz : RPGClass.values()) distribution.put(clazz.name(), 0);
        for (PlayerData data : economyManager.getCache().values()) {
            distribution.merge(data.getClassName(), 1, Integer::sum);
        }
        int total = economyManager.getCache().size();
        
        // Header
        inv.setItem(4, createItem(Material.BEACON, "§d§lClass Distribution", Arrays.asList(
                "§8§m                              ",
                "§7Total Players: §f" + total,
                "§7Unclassed: §8" + distribution.getOrDefault("NONE", 0),
                "§8§m                              ",
                "§7Click a class below to force-set",
                "§7ALL online players to that class."
        )));
        
        int[] slots = {20, 21, 22, 23, 24};
        RPGClass[] classes = RPGClass.values();
        for (int i = 0; i < classes.length && i < slots.length; i++) {
            RPGClass clazz = classes[i];
            int count = distribution.getOrDefault(clazz.name(), 0);
            float pct = total > 0 ? (count * 100f / total) : 0;
            
            // Show the actual Gold-tier starting weapon as the icon
            WeaponManager wm = plugin.getWeaponManager();
            String weaponName = getWeaponName(clazz, WeaponTier.GOLD);
            ItemStack weaponIcon = wm.createWeapon(clazz, WeaponTier.GOLD, weaponName, null, 6.0, 1.4);
            ItemMeta weaponMeta = weaponIcon.getItemMeta();
            if (weaponMeta != null) {
                weaponMeta.setDisplayName(clazz.getColoredName());
                List<String> lore = new ArrayList<>();
                lore.add("§8§m                              ");
                lore.add("§7Players: §f" + count + " §8(§e" + String.format("%.1f", pct) + "%§8)");
                lore.add("§7Starting Weapon: §6" + weaponName);
                lore.add("§7Material: §8" + clazz.getWeaponMaterial().name());
                lore.add("§8§m                              ");
                lore.add("§a▸ Left: Set ALL online → " + clazz.getDisplayName());
                lore.add("§c▸ Right: Reset ALL online → NONE");
                weaponMeta.setLore(lore);
                weaponIcon.setItemMeta(weaponMeta);
            }
            inv.setItem(slots[i], weaponIcon);
        }

        // Reset All Classes button
        inv.setItem(31, createItem(Material.BARRIER, "§c§lReset All Classes", Arrays.asList(
                "§7Set ALL online players to NONE.",
                "§8§m                              ",
                "§c▸ Click to reset"
        )));

        inv.setItem(49, createItem(Material.ARROW, "§a§l← Back", null));
        admin.openInventory(inv);
    }

    // ============================================================
    // BOUNTY OVERVIEW — Shows top bounties with management
    // ============================================================

    public void openBountyOverview(Player admin) {
        Inventory inv = Bukkit.createInventory(null, 54, BOUNTY_TITLE);
        fillBorder(inv, Material.ORANGE_STAINED_GLASS_PANE, Material.GRAY_STAINED_GLASS_PANE);
        
        // Header
        long totalBounty = economyManager.getCache().values().stream().mapToLong(PlayerData::getBounty).sum();
        int bountiedPlayers = (int) economyManager.getCache().values().stream().filter(d -> d.getBounty() > 0).count();
        
        inv.setItem(4, createItem(Material.COMPASS, "§6§lBounty System", Arrays.asList(
                "§8§m                              ",
                "§7Active Bounties: §e" + bountiedPlayers,
                "§7Total Pool: §6" + String.format("%,d", totalBounty) + " shards",
                "§8§m                              ",
                "§7Click a player to manage their bounty."
        )));
        
        // Sorted bounty list (descending)
        List<PlayerData> bountied = economyManager.getCache().values().stream()
                .filter(d -> d.getBounty() > 0)
                .sorted((a, b) -> Integer.compare(b.getBounty(), a.getBounty()))
                .collect(Collectors.toList());
        
        int[] slots = {10,11,12,13,14,15,16,19,20,21,22,23,24,25,28,29,30,31,32,33,34};
        int idx = 0;
        int rank = 1;
        for (PlayerData data : bountied) {
            if (idx >= slots.length) break;
            boolean online = Bukkit.getPlayer(data.getUuid()) != null;
            
            Material icon;
            String rankColor;
            if (rank == 1) { icon = Material.GOLD_BLOCK; rankColor = "§6§l"; }
            else if (rank == 2) { icon = Material.IRON_BLOCK; rankColor = "§7§l"; }
            else if (rank == 3) { icon = Material.COPPER_BLOCK; rankColor = "§c§l"; }
            else { icon = Material.PAPER; rankColor = "§f"; }
            
            inv.setItem(slots[idx++], createItem(icon, rankColor + "#" + rank + " §f" + data.getLastKnownName(), Arrays.asList(
                    "§8§m                              ",
                    "§7Bounty: §6" + String.format("%,d", data.getBounty()) + " shards",
                    "§7Status: " + (online ? "§a§lONLINE" : "§7§lOFFLINE"),
                    "§7Class: §b" + data.getClassName(),
                    "§7K/D: §a" + data.getKills() + "§7/§c" + data.getDeaths(),
                    "§8§m                              ",
                    "§eLeft: +1,000 bounty",
                    "§cRight: Clear bounty"
            )));
            rank++;
        }
        
        if (bountied.isEmpty()) {
            inv.setItem(22, createItem(Material.STRUCTURE_VOID, "§7§lNo Active Bounties", Arrays.asList(
                    "§8No players currently have bounties."
            )));
        }
        
        // Clear All button
        inv.setItem(40, createItem(Material.BARRIER, "§c§lClear ALL Bounties", Arrays.asList(
                "§7Reset every player's bounty to 0.",
                "§8§m                              ",
                "§c▸ Click to wipe"
        )));

        inv.setItem(49, createItem(Material.ARROW, "§a§l← Back", null));
        admin.openInventory(inv);
    }

    // ============================================================
    // SPECIAL ITEMS — Click to receive items in YOUR inventory
    // ============================================================

    public void openSpecialItemsMenu(Player admin) {
        Inventory inv = Bukkit.createInventory(null, 27, SPECIAL_ITEMS_TITLE);
        fillBorder(inv, Material.CYAN_STAINED_GLASS_PANE, Material.GRAY_STAINED_GLASS_PANE);
        
        inv.setItem(10, createItem(Material.NETHER_STAR, "§b§l✦ Soul Fragment §b§l✦", Arrays.asList(
                "§8§m                              ",
                "§7Crafting ingredient for class weapons.",
                "§8§m                              ",
                "§eLeft: Get 1x",
                "§aShift+Left: Get 16x"
        )));
        inv.setItem(13, createItem(Material.PAPER, "§6§l✦ Class Change Scroll §6§l✦", Arrays.asList(
                "§8§m                              ",
                "§7Allows switching RPG class.",
                "§8§m                              ",
                "§a▸ Click to receive 1x"
        )));
        inv.setItem(16, createItem(Material.ENCHANTED_BOOK, "§5§l✦ Data Restore Tome §5§l✦", Arrays.asList(
                "§8§m                              ",
                "§7Restores saved class progress.",
                "§8§m                              ",
                "§a▸ Click to receive 1x"
        )));
        
        inv.setItem(22, createItem(Material.ARROW, "§a§l← Back", null));
        admin.openInventory(inv);
    }

    // ============================================================
    // ECONOMY CONTROL — Enhanced with more tools
    // ============================================================

    public void openEconomyControl(Player admin) {
        Inventory inv = Bukkit.createInventory(null, 54, ECONOMY_TITLE);
        fillBorder(inv, Material.LIME_STAINED_GLASS_PANE, Material.GRAY_STAINED_GLASS_PANE);
        
        // Header
        long totalShards = economyManager.getCache().values().stream().mapToLong(PlayerData::getShards).sum();
        long totalGems = economyManager.getCache().values().stream().mapToLong(PlayerData::getGems).sum();
        
        inv.setItem(4, createItem(Material.GOLD_BLOCK, "§a§lEconomy Overview", Arrays.asList(
                "§8§m                              ",
                "§7Total Shards in Circulation: §a" + String.format("%,d", totalShards),
                "§7Total Gems in Circulation: §b" + String.format("%,d", totalGems),
                "§7Online Players: §e" + Bukkit.getOnlinePlayers().size(),
                "§8§m                              "
        )));
        
        // --- Airdrops Row ---
        inv.setItem(19, createItem(Material.GOLD_BLOCK, "§6§lAirdrop: 5,000 Shards", Arrays.asList(
                "§7Reward ALL online players.", 
                "§8§m                              ", 
                "§a▸ Click to execute")));
        inv.setItem(20, createItem(Material.GOLD_INGOT, "§6§lAirdrop: 1,000 Shards", Arrays.asList(
                "§7Reward ALL online players.", 
                "§8§m                              ", 
                "§a▸ Click to execute")));
        inv.setItem(21, createItem(Material.EMERALD_BLOCK, "§b§lAirdrop: 500 Gems", Arrays.asList(
                "§7Reward ALL online players.", 
                "§8§m                              ", 
                "§a▸ Click to execute")));
        inv.setItem(22, createItem(Material.EMERALD, "§b§lAirdrop: 100 Gems", Arrays.asList(
                "§7Reward ALL online players.", 
                "§8§m                              ", 
                "§a▸ Click to execute")));
        
        // --- Management Row ---
        inv.setItem(28, createItem(Material.LAVA_BUCKET, "§c§lWipe ALL Shards", Arrays.asList(
                "§7Set every player's shards to 0.", 
                "§8§m                              ", 
                "§4§lDANGEROUS!", 
                "§c▸ Click to execute")));
        inv.setItem(29, createItem(Material.WATER_BUCKET, "§c§lWipe ALL Gems", Arrays.asList(
                "§7Set every player's gems to 0.", 
                "§8§m                              ", 
                "§4§lDANGEROUS!", 
                "§c▸ Click to execute")));
        inv.setItem(30, createItem(Material.EXPERIENCE_BOTTLE, "§e§lReset ALL Levels", Arrays.asList(
                "§7Set every player's level to 1.", 
                "§8§m                              ", 
                "§4§lDANGEROUS!", 
                "§c▸ Click to execute")));
        
        // --- Utility Row ---
        inv.setItem(37, createItem(Material.PAPER, "§f§lLog Export (Console)", Arrays.asList(
                "§7Export economy statistics.", 
                "§8§m                              ", 
                "§e▸ Click to trigger")));
        inv.setItem(38, createItem(Material.SUNFLOWER, "§6§lDrop Physical Shards", Arrays.asList(
                "§7Drop 500 physical shards at", 
                "§7your current location.", 
                "§8§m                              ", 
                "§a▸ Click to drop")));

        inv.setItem(49, createItem(Material.ARROW, "§a§l← Back", null));
        admin.openInventory(inv);
    }

    // ============================================================
    // WEAPON ARSENAL
    // ============================================================

    public void openWeaponGUI(Player admin, int page) {
        weaponPages.put(admin.getUniqueId(), page);
        Inventory inv = Bukkit.createInventory(null, 54, WEAPONS_TITLE);
        fillBorder(inv, Material.MAGENTA_STAINED_GLASS_PANE, Material.GRAY_STAINED_GLASS_PANE);
        
        WeaponManager wm = plugin.getWeaponManager();
        int idx = 0;
        int skip = page * 28;
        int count = 0;

        int[] slots = {10,11,12,13,14,15,16,19,20,21,22,23,24,25,28,29,30,31,32,33,34,37,38,39,40,41,42,43};

        for (RPGClass clazz : RPGClass.values()) {
            for (WeaponTier tier : WeaponTier.values()) {
                if (count < skip) { count++; continue; }
                if (idx >= slots.length) break;

                ItemStack weapon = wm.createWeapon(clazz, tier, tier.getColoredName() + " " + clazz.getDisplayName(), null, 10.0, 1.6);
                inv.setItem(slots[idx++], weapon);
            }
        }

        inv.setItem(48, createItem(Material.ARROW, "§7Previous Page", null));
        inv.setItem(49, createItem(Material.ARROW, "§c§l← Back", null));
        inv.setItem(50, createItem(Material.ARROW, "§7Next Page", null));
        
        admin.openInventory(inv);
    }

    // ============================================================
    // AUCTION MODERATION
    // ============================================================

    public void openAuctionModeration(Player admin) {
        Inventory inv = Bukkit.createInventory(null, 54, AUCTION_TITLE);
        fillBorder(inv, Material.YELLOW_STAINED_GLASS_PANE, Material.GRAY_STAINED_GLASS_PANE);
        inv.setItem(49, createItem(Material.ARROW, "§a§l← Back", null));
        admin.openInventory(inv);
    }

    // ============================================================
    // LOGS VIEWER
    // ============================================================

    public void openLogsViewer(Player admin, int page) {
        logPages.put(admin.getUniqueId(), page);
        Inventory inv = Bukkit.createInventory(null, 54, LOGS_TITLE);
        fillBorder(inv, Material.WHITE_STAINED_GLASS_PANE, Material.GRAY_STAINED_GLASS_PANE);
        
        inv.setItem(22, createItem(Material.CLOCK, "§e§lLoading Audit Logs...", Arrays.asList("§7Querying MongoDB cluster...", "§7Page: §f" + (page + 1))));
        admin.openInventory(inv);

        plugin.getMongoManager().getRecentLogs(28, page * 28).thenAccept(logs -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!admin.getOpenInventory().getTitle().equals(LOGS_TITLE)) return;
                inv.clear();
                fillBorder(inv, Material.WHITE_STAINED_GLASS_PANE, Material.GRAY_STAINED_GLASS_PANE);
                
                int[] slots = {10,11,12,13,14,15,16,19,20,21,22,23,24,25,28,29,30,31,32,33,34,37,38,39,40,41,42,43};
                int idx = 0;
                for (org.bson.Document log : logs) {
                    if (idx >= slots.length) break;
                    String type = log.getString("type");
                    String summary = log.getString("summary");
                    java.util.Date timestamp = log.getDate("timestamp");
                    
                    Material icon = switch (type != null ? type : "") {
                        case "SHOP_PURCHASE" -> Material.GOLD_INGOT;
                        case "ADMIN_ACTION" -> Material.COMMAND_BLOCK;
                        case "STORE_PURCHASE_COMPLETE", "STORE_DELIVERY" -> Material.EMERALD;
                        default -> Material.PAPER;
                    };

                    ItemStack item = new ItemStack(icon);
                    ItemMeta meta = item.getItemMeta();
                    if (meta != null) {
                        meta.setDisplayName("§e§l" + (type != null ? type.replace("_", " ") : "LOG"));
                        List<String> lore = new ArrayList<>();
                        lore.add("§8§m                              ");
                        if (summary != null) {
                            if (summary.length() > 38) {
                                lore.add("§f" + summary.substring(0, 38));
                                lore.add("§f" + summary.substring(38));
                            } else lore.add("§f" + summary);
                        }
                        lore.add("§7Time: §8" + (timestamp != null ? timestamp.toString() : "Unknown"));
                        lore.add("§8§m                              ");
                        meta.setLore(lore);
                        item.setItemMeta(meta);
                    }
                    inv.setItem(slots[idx++], item);
                }

                inv.setItem(48, createItem(Material.ARROW, "§a§l← Newer Logs", Arrays.asList("§7Click for previous page.")));
                inv.setItem(49, createItem(Material.ARROW, "§c§l← Back", null));
                inv.setItem(50, createItem(Material.ARROW, "§a§lOlder Logs →", Arrays.asList("§7Click for next page.")));
                inv.setItem(4, createAuditStat(logs.size(), page));
            });
        });
    }

    private ItemStack createAuditStat(int count, int page) {
        return createItem(Material.BOOK, "§f§lAudit Statistics", Arrays.asList("§7Page: §e" + (page + 1), "§7Entries Loaded: §a" + count));
    }

    // ============================================================
    // CLICK HANDLER
    // ============================================================

    @EventHandler
    public void onAdminClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player admin)) return;
        
        String title = event.getView().getTitle();
        if (!isAdminInventory(title)) return;
        if (!admin.hasPermission("kingdomcore.admin")) return;
        
        // Prevent clicking outside or bottom inventory
        if (event.getClickedInventory() == null || event.getClickedInventory() == admin.getInventory()) {
            event.setCancelled(true);
            return;
        }
        
        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        
        int slot = event.getRawSlot();
        boolean isLeft = event.getClick().isLeftClick();
        boolean isShift = event.getClick().isShiftClick();

        // Universal back button (slot 49 for 6-row, slot 22 for 3-row special items)
        if (title.equals(SPECIAL_ITEMS_TITLE) && slot == 22) { openMainDashboard(admin); return; }
        // Player Profile "Back" goes to Player Manager, not dashboard
        if (title.startsWith(PLAYER_PROFILE_PREFIX) && slot == 49) { openPlayerManager(admin); return; }
        // All other menus slot 49 = back to dashboard
        if (slot == 49 && !title.startsWith(PLAYER_PROFILE_PREFIX)) { openMainDashboard(admin); return; }

        // ---- MAIN DASHBOARD ----
        if (title.equals(MAIN_TITLE)) {
            switch (slot) {
                case 19 -> openPlayerManager(admin);
                case 20 -> openClassManager(admin);
                case 21 -> openAuctionModeration(admin);
                case 28 -> openEconomyControl(admin);
                case 29 -> openBountyOverview(admin);
                case 37 -> openLogsViewer(admin, 0);
                case 38 -> openWeaponGUI(admin, 0);
                case 39 -> openSpecialItemsMenu(admin);
                case 40 -> executeSyncShops(admin);
                case 43 -> admin.performCommand("reset");
            }
        }
        // ---- PLAYER LIST ----
        else if (title.equals(PLAYERS_TITLE)) {
            if (clicked.getType() == Material.PLAYER_HEAD && clicked.hasItemMeta()) {
                SkullMeta meta = (SkullMeta) clicked.getItemMeta();
                if (meta != null && meta.getOwningPlayer() != null) {
                    UUID targetUUID = meta.getOwningPlayer().getUniqueId();
                    openPlayerProfile(admin, targetUUID);
                    admin.playSound(admin.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
                }
            }
        }
        // ---- PLAYER PROFILE ----
        else if (title.startsWith(PLAYER_PROFILE_PREFIX)) {
            handleProfileClick(admin, slot, isLeft);
        }
        // ---- SPECIAL ITEMS ----
        else if (title.equals(SPECIAL_ITEMS_TITLE)) {
            handleSpecialItemsClick(admin, slot, isShift);
        }
        // ---- CLASS MANAGER ----
        else if (title.equals(CLASS_TITLE)) {
            handleClassManagerClick(admin, slot, isLeft);
        }
        // ---- BOUNTY OVERVIEW ----
        else if (title.equals(BOUNTY_TITLE)) {
            handleBountyClick(admin, slot, isLeft, event.getClickedInventory());
        }
        // ---- ECONOMY CONTROL ----
        else if (title.equals(ECONOMY_TITLE)) {
            handleEconomyClick(admin, slot);
        }
        // ---- LOGS ----
        else if (title.equals(LOGS_TITLE)) {
            int page = logPages.getOrDefault(admin.getUniqueId(), 0);
            if (slot == 48 && page > 0) openLogsViewer(admin, page - 1);
            else if (slot == 50) openLogsViewer(admin, page + 1);
        }
        // ---- WEAPONS ----
        else if (title.equals(WEAPONS_TITLE)) {
            if (slot >= 10 && slot <= 43) {
                admin.getInventory().addItem(clicked.clone());
                admin.sendMessage("§a§l[Kingdom] §7Weapon added to your inventory.");
                admin.playSound(admin.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.5f, 1.5f);
            }
            int page = weaponPages.getOrDefault(admin.getUniqueId(), 0);
            if (slot == 48 && page > 0) openWeaponGUI(admin, page - 1);
            else if (slot == 50) openWeaponGUI(admin, page + 1);
        }
    }

    // ============================================================
    // PROFILE CLICK HANDLER
    // ============================================================

    private void handleProfileClick(Player admin, int slot, boolean left) {
        UUID targetUUID = viewingProfile.get(admin.getUniqueId());
        if (targetUUID == null) return;
        PlayerData data = economyManager.getPlayerData(targetUUID);
        if (data == null) return;
        
        String targetName = data.getLastKnownName();
        Player targetPlayer = Bukkit.getPlayer(targetUUID);

        switch (slot) {
            case 19 -> {
                // Class cycling
                if (left) {
                    RPGClass[] classes = RPGClass.values();
                    RPGClass current = RPGClass.fromString(data.getClassName());
                    int nextIdx = 0;
                    if (current != null) {
                        nextIdx = (current.ordinal() + 1) % classes.length;
                    }
                    String newClass = classes[nextIdx].name();
                    economyManager.setClassName(targetUUID, newClass);
                    admin.sendMessage("§a§l[Kingdom] §7Set §f" + targetName + "§7's class to §b" + newClass);
                    if (targetPlayer != null) {
                        targetPlayer.sendMessage("§e§l[Kingdom] §7Your class has been set to §b" + newClass + " §7by an admin.");
                    }
                    logAdminAction(admin, targetName, "set class to " + newClass);
                } else {
                    economyManager.setClassName(targetUUID, "NONE");
                    admin.sendMessage("§a§l[Kingdom] §7Reset §f" + targetName + "§7's class to §7NONE");
                    if (targetPlayer != null) {
                        targetPlayer.sendMessage("§e§l[Kingdom] §7Your class has been reset by an admin.");
                    }
                    logAdminAction(admin, targetName, "reset class to NONE");
                }
            }
            case 20 -> { 
                int amount = left ? 1000 : -1000;
                economyManager.addShards(targetUUID, amount); 
                admin.sendMessage("§a§l[Kingdom] §7" + (amount > 0 ? "Added" : "Removed") + " §e" + String.format("%,d", Math.abs(amount)) + " shards §7" + (amount > 0 ? "to" : "from") + " §f" + targetName);
                if (targetPlayer != null) {
                    targetPlayer.sendMessage("§e§l[Kingdom] §7An admin " + (amount > 0 ? "added" : "removed") + " §e" + String.format("%,d", Math.abs(amount)) + " shards §7" + (amount > 0 ? "to" : "from") + " your account.");
                }
                logAdminAction(admin, targetName, (amount > 0 ? "added " : "removed ") + Math.abs(amount) + " shards");
            }
            case 21 -> {
                int amount = left ? 100 : -100;
                economyManager.addGems(targetUUID, amount);
                admin.sendMessage("§a§l[Kingdom] §7" + (amount > 0 ? "Added" : "Removed") + " §b" + String.format("%,d", Math.abs(amount)) + " gems §7" + (amount > 0 ? "to" : "from") + " §f" + targetName);
                if (targetPlayer != null) {
                    targetPlayer.sendMessage("§e§l[Kingdom] §7An admin " + (amount > 0 ? "added" : "removed") + " §b" + String.format("%,d", Math.abs(amount)) + " gems §7" + (amount > 0 ? "to" : "from") + " your account.");
                }
                logAdminAction(admin, targetName, (amount > 0 ? "added " : "removed ") + Math.abs(amount) + " gems");
            }
            case 22 -> {
                int amount = left ? 1 : -1;
                int newLevel = Math.max(1, data.getLevel() + amount);
                economyManager.setLevel(targetUUID, newLevel);
                admin.sendMessage("§a§l[Kingdom] §7Set §f" + targetName + "§7's level to §e" + newLevel);
                if (targetPlayer != null) {
                    targetPlayer.sendMessage("§e§l[Kingdom] §7Your level has been set to §e" + newLevel + " §7by an admin.");
                }
                logAdminAction(admin, targetName, "set level to " + newLevel);
            }
            case 23 -> {
                if (left) {
                    economyManager.setBounty(targetUUID, 500, true);
                    admin.sendMessage("§a§l[Kingdom] §7Added §6500 bounty §7to §f" + targetName);
                } else {
                    economyManager.setBounty(targetUUID, 0, false);
                    admin.sendMessage("§a§l[Kingdom] §7Cleared bounty from §f" + targetName);
                }
                logAdminAction(admin, targetName, "modified bounty");
            }
            case 24 -> { 
                data.setKills(0); 
                data.setDeaths(0);
                economyManager.savePlayer(targetUUID); 
                admin.sendMessage("§a§l[Kingdom] §7Wiped combat stats for §f" + targetName);
                logAdminAction(admin, targetName, "wiped combat stats"); 
            }
            case 25 -> { 
                economyManager.fullReset(targetUUID); 
                admin.sendMessage("§c§l[Kingdom] §7Full account reset performed for §f" + targetName);
                if (targetPlayer != null) {
                    targetPlayer.sendMessage("§c§l[Kingdom] §7Your account has been fully reset by an admin.");
                }
                logAdminAction(admin, targetName, "full account wipe"); 
            }
            // Give special items to target player
            case 29 -> {
                giveItemToTarget(admin, targetUUID, targetName, specialItems.createSoulItem(), "Soul Fragment");
            }
            case 30 -> {
                giveItemToTarget(admin, targetUUID, targetName, specialItems.createClassChangeScroll(), "Class Change Scroll");
            }
            case 31 -> {
                giveItemToTarget(admin, targetUUID, targetName, specialItems.createDataRestoreTome(), "Data Restore Tome");
            }
        }
        // Refresh the profile view (unless back was clicked)
        if (slot >= 19 && slot <= 31) {
            openPlayerProfile(admin, targetUUID);
            admin.playSound(admin.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.8f, 1.2f);
        }
    }

    private void giveItemToTarget(Player admin, UUID targetUUID, String targetName, ItemStack item, String itemName) {
        Player targetPlayer = Bukkit.getPlayer(targetUUID);
        if (targetPlayer != null && targetPlayer.isOnline()) {
            targetPlayer.getInventory().addItem(item);
            targetPlayer.sendMessage("§e§l[Kingdom] §7You received a §b" + itemName + " §7from an admin.");
            admin.sendMessage("§a§l[Kingdom] §7Gave §b" + itemName + " §7to §f" + targetName);
        } else {
            admin.sendMessage("§c§l[Kingdom] §7Player §f" + targetName + " §7is offline! Item was NOT given.");
        }
        logAdminAction(admin, targetName, "gave " + itemName);
    }

    // ============================================================
    // SPECIAL ITEMS CLICK HANDLER
    // ============================================================

    private void handleSpecialItemsClick(Player admin, int slot, boolean shift) {
        switch (slot) {
            case 10 -> {
                int amount = shift ? 16 : 1;
                admin.getInventory().addItem(specialItems.createSoulItem(amount));
                admin.sendMessage("§a§l[Kingdom] §7Received §b" + amount + "x Soul Fragment§7.");
                admin.playSound(admin.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.5f, 1.2f);
            }
            case 13 -> {
                admin.getInventory().addItem(specialItems.createClassChangeScroll());
                admin.sendMessage("§a§l[Kingdom] §7Received §b1x Class Change Scroll§7.");
                admin.playSound(admin.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.5f, 1.2f);
            }
            case 16 -> {
                admin.getInventory().addItem(specialItems.createDataRestoreTome());
                admin.sendMessage("§a§l[Kingdom] §7Received §b1x Data Restore Tome§7.");
                admin.playSound(admin.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.5f, 1.2f);
            }
        }
    }

    // ============================================================
    // CLASS MANAGER CLICK HANDLER
    // ============================================================

    private void handleClassManagerClick(Player admin, int slot, boolean left) {
        int[] classSlots = {20, 21, 22, 23, 24};
        RPGClass[] classes = RPGClass.values();
        
        for (int i = 0; i < classSlots.length && i < classes.length; i++) {
            if (slot == classSlots[i]) {
                if (left) {
                    // Set ALL online players to this class
                    String className = classes[i].name();
                    int count = 0;
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        economyManager.setClassName(p.getUniqueId(), className);
                        p.sendMessage("§e§l[Kingdom] §7Your class has been set to §b" + classes[i].getColoredName() + " §7by an admin.");
                        count++;
                    }
                    admin.sendMessage("§a§l[Kingdom] §7Set §e" + count + " §7online players to §b" + classes[i].getColoredName());
                    logAdminAction(admin, "ALL_ONLINE", "mass set class to " + className);
                } else {
                    // Reset ALL online to NONE
                    int count = 0;
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        economyManager.setClassName(p.getUniqueId(), "NONE");
                        p.sendMessage("§e§l[Kingdom] §7Your class has been reset by an admin.");
                        count++;
                    }
                    admin.sendMessage("§a§l[Kingdom] §7Reset §e" + count + " §7online players to §7NONE");
                    logAdminAction(admin, "ALL_ONLINE", "mass reset all classes");
                }
                openClassManager(admin);
                admin.playSound(admin.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.8f, 1.2f);
                return;
            }
        }
        
        // Reset ALL button
        if (slot == 31) {
            int count = 0;
            for (Player p : Bukkit.getOnlinePlayers()) {
                economyManager.setClassName(p.getUniqueId(), "NONE");
                p.sendMessage("§e§l[Kingdom] §7Your class has been reset by an admin.");
                count++;
            }
            admin.sendMessage("§c§l[Kingdom] §7Reset §e" + count + " §7online players' classes to §7NONE");
            logAdminAction(admin, "ALL_ONLINE", "reset all classes");
            openClassManager(admin);
            admin.playSound(admin.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.8f, 1.2f);
        }
    }

    // ============================================================
    // BOUNTY CLICK HANDLER
    // ============================================================

    private void handleBountyClick(Player admin, int slot, boolean left, Inventory inv) {
        // Clear all bounties
        if (slot == 40) {
            int count = 0;
            for (PlayerData data : economyManager.getCache().values()) {
                if (data.getBounty() > 0) {
                    economyManager.setBounty(data.getUuid(), 0, false);
                    count++;
                }
            }
            admin.sendMessage("§c§l[Kingdom] §7Cleared bounties from §e" + count + " §7players.");
            logAdminAction(admin, "ALL_PLAYERS", "cleared all bounties");
            openBountyOverview(admin);
            admin.playSound(admin.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.8f, 1.2f);
            return;
        }
        
        // Player bounty slots
        int[] slots = {10,11,12,13,14,15,16,19,20,21,22,23,24,25,28,29,30,31,32,33,34};
        int idx = -1;
        for (int i = 0; i < slots.length; i++) {
            if (slot == slots[i]) { idx = i; break; }
        }
        if (idx < 0) return;
        
        // Get the sorted bounty list and find the matching player
        List<PlayerData> bountied = economyManager.getCache().values().stream()
                .filter(d -> d.getBounty() > 0)
                .sorted((a, b) -> Integer.compare(b.getBounty(), a.getBounty()))
                .collect(Collectors.toList());
        
        if (idx >= bountied.size()) return;
        PlayerData target = bountied.get(idx);
        
        if (left) {
            economyManager.setBounty(target.getUuid(), 1000, true);
            admin.sendMessage("§a§l[Kingdom] §7Added §61,000 bounty §7to §f" + target.getLastKnownName() + " §7(new total: §6" + (target.getBounty()) + "§7)");
            logAdminAction(admin, target.getLastKnownName(), "added 1000 bounty");
        } else {
            economyManager.setBounty(target.getUuid(), 0, false);
            admin.sendMessage("§a§l[Kingdom] §7Cleared bounty from §f" + target.getLastKnownName());
            logAdminAction(admin, target.getLastKnownName(), "cleared bounty");
        }
        openBountyOverview(admin);
        admin.playSound(admin.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.8f, 1.2f);
    }

    // ============================================================
    // ECONOMY CLICK HANDLER
    // ============================================================

    private void handleEconomyClick(Player admin, int slot) {
        switch (slot) {
            case 19 -> executeAirdrop(admin, "SHARDS", 5000);
            case 20 -> executeAirdrop(admin, "SHARDS", 1000);
            case 21 -> executeAirdrop(admin, "GEMS", 500);
            case 22 -> executeAirdrop(admin, "GEMS", 100);
            case 28 -> {
                for (PlayerData data : economyManager.getCache().values()) {
                    data.setShards(0);
                    data.updateLocal();
                    economyManager.savePlayer(data.getUuid());
                }
                admin.sendMessage("§c§l[Kingdom] §7Wiped ALL player shards.");
                logAdminAction(admin, "ALL_PLAYERS", "wiped all shards");
                admin.playSound(admin.getLocation(), Sound.ENTITY_WITHER_DEATH, 0.3f, 1.5f);
                openEconomyControl(admin);
            }
            case 29 -> {
                for (PlayerData data : economyManager.getCache().values()) {
                    data.setGems(0);
                    data.updateLocal();
                    economyManager.savePlayer(data.getUuid());
                }
                admin.sendMessage("§c§l[Kingdom] §7Wiped ALL player gems.");
                logAdminAction(admin, "ALL_PLAYERS", "wiped all gems");
                admin.playSound(admin.getLocation(), Sound.ENTITY_WITHER_DEATH, 0.3f, 1.5f);
                openEconomyControl(admin);
            }
            case 30 -> {
                for (PlayerData data : economyManager.getCache().values()) {
                    data.setLevel(1);
                    data.setXp(0);
                    data.updateLocal();
                    economyManager.savePlayer(data.getUuid());
                }
                admin.sendMessage("§c§l[Kingdom] §7Reset ALL player levels to 1.");
                logAdminAction(admin, "ALL_PLAYERS", "reset all levels");
                admin.playSound(admin.getLocation(), Sound.ENTITY_WITHER_DEATH, 0.3f, 1.5f);
                openEconomyControl(admin);
            }
            case 37 -> {
                admin.sendMessage("§e§l[Kingdom] §7=== Economy Report ===");
                admin.sendMessage("§7Total Players: §f" + economyManager.getCache().size());
                long ts = economyManager.getCache().values().stream().mapToLong(PlayerData::getShards).sum();
                long tg = economyManager.getCache().values().stream().mapToLong(PlayerData::getGems).sum();
                admin.sendMessage("§7Total Shards: §a" + String.format("%,d", ts));
                admin.sendMessage("§7Total Gems: §b" + String.format("%,d", tg));
                plugin.getLogger().info("[Economy Export] Players=" + economyManager.getCache().size() + " Shards=" + ts + " Gems=" + tg);
                admin.playSound(admin.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.8f, 1.2f);
            }
            case 38 -> {
                ItemStack shard = specialItems.createPhysicalShard(500);
                admin.getWorld().dropItemNaturally(admin.getLocation(), shard);
                admin.sendMessage("§a§l[Kingdom] §7Dropped §6500 physical shards §7at your location.");
                logAdminAction(admin, "SYSTEM", "dropped 500 physical shards");
                admin.playSound(admin.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1f, 1f);
            }
        }
    }

    // ============================================================
    // UTILITY METHODS
    // ============================================================

    private void executeSyncShops(Player admin) {
        admin.sendMessage("§e§l[Kingdom] §7Starting background shop synchronization...");
        admin.playSound(admin.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1f, 1f);
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                plugin.getShopDataManager().migrateYAMLtoMongoDB();
                plugin.getShopDataManager().reload();
                
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (admin.isOnline()) {
                        admin.sendMessage("§a§l[Kingdom] §7Background sync complete! Webstore updated.");
                        admin.playSound(admin.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.5f);
                    }
                    logAdminAction(admin, "SYSTEM", "synced shop configurations (background)");
                });
            } catch (Exception e) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (admin.isOnline()) {
                        admin.sendMessage("§c§l[Kingdom] §7Sync failed! Check console for errors.");
                    }
                });
                plugin.getLogger().severe("Shop migration failed: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private void executeAirdrop(Player admin, String type, int amount) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (type.equals("SHARDS")) economyManager.addShards(p.getUniqueId(), amount);
            else economyManager.addGems(p.getUniqueId(), amount);
        }
        Bukkit.broadcastMessage("§8§m                              ");
        Bukkit.broadcastMessage("§6§lNETWORK AIRDROP!");
        Bukkit.broadcastMessage("§7Admin §f" + admin.getName() + " §7has gifted §e" + String.format("%,d", amount) + " " + type + " §7to everyone!");
        Bukkit.broadcastMessage("§8§m                              ");
        admin.playSound(admin.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        logAdminAction(admin, "ALL_PLAYERS", "issued network airdrop: " + amount + " " + type);
    }

    private void logAdminAction(Player admin, String targetName, String action) {
        org.bson.Document log = new org.bson.Document()
                .append("source", "GAME")
                .append("type", "ADMIN_ACTION")
                .append("executor", new org.bson.Document("uuid", admin.getUniqueId().toString()).append("name", admin.getName()))
                .append("target", targetName)
                .append("summary", "Admin " + admin.getName() + " " + action + " for " + targetName)
                .append("timestamp", new java.util.Date());
        plugin.getMongoManager().logAction(log);
    }

    // ============================================================
    // GUI HELPERS
    // ============================================================

    private void fillBorder(Inventory inv, Material m1, Material m2) {
        ItemStack p1 = createPane(m1);
        ItemStack p2 = createPane(m2);
        for (int i = 0; i < inv.getSize(); i++) {
            int row = i / 9, col = i % 9;
            if (row == 0 || row == (inv.getSize()/9)-1 || col == 0 || col == 8) {
                inv.setItem(i, (i % 2 == 0) ? p1 : p2);
            }
        }
    }

    private ItemStack createPane(Material m) {
        ItemStack item = new ItemStack(m);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) { meta.setDisplayName(" "); item.setItemMeta(meta); }
        return item;
    }

    private ItemStack createItem(Material m, String name, List<String> lore) {
        ItemStack item = new ItemStack(m);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore != null) meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Get the standardized weapon name for a class and tier.
     * E.g., ARCHER + GOLD → "Gold Bow", RONIN + NETHER → "Nether Katana"
     */
    private String getWeaponName(RPGClass rpgClass, WeaponTier tier) {
        String weaponType = switch (rpgClass) {
            case ARCHER -> "Bow";
            case KNIGHT -> "Shield";
            case WIZARD -> "Staff";
            case ROGUE -> "Dagger";
            case RONIN -> "Katana";
        };
        return tier.getColoredName() + " " + weaponType;
    }
    /**
     * Helper to verify if an inventory title belongs to the Admin Panel.
     */
    private boolean isAdminInventory(String title) {
        if (title == null) return false;
        return title.equals(MAIN_TITLE) || 
               title.equals(PLAYERS_TITLE) || 
               title.startsWith(PLAYER_PROFILE_PREFIX) || 
               title.equals(ECONOMY_TITLE) || 
               title.equals(CLASS_TITLE) || 
               title.equals(BOUNTY_TITLE) || 
               title.equals(AUCTION_TITLE) || 
               title.equals(WEAPONS_TITLE) || 
               title.equals(SPECIAL_ITEMS_TITLE) || 
               title.equals(LOGS_TITLE);
    }
}
