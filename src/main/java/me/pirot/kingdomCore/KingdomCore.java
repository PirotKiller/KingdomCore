package me.pirot.kingdomCore;

import me.pirot.kingdomCore.admin.AdminCommand;
import me.pirot.kingdomCore.admin.AdminGUI;
import me.pirot.kingdomCore.auction.AuctionCommand;
import me.pirot.kingdomCore.auction.AuctionManager;
import me.pirot.kingdomCore.bounty.BountyCommand;
import me.pirot.kingdomCore.bounty.BountyListener;
import me.pirot.kingdomCore.bounty.BountyManager;
import me.pirot.kingdomCore.config.ConfigManager;
import me.pirot.kingdomCore.database.MongoManager;
import me.pirot.kingdomCore.database.WebCommandManager;
import me.pirot.kingdomCore.economy.EconomyManager;
import me.pirot.kingdomCore.reset.ResetManager;
import me.pirot.kingdomCore.rpg.*;
import me.pirot.kingdomCore.scoreboard.ScoreboardCommand;
import me.pirot.kingdomCore.scoreboard.ScoreboardManager;
import me.pirot.kingdomCore.shop.*;
import me.pirot.kingdomCore.web.VerifyCommand;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class KingdomCore extends JavaPlugin {

    private ConfigManager configManager;
    private MongoManager mongoManager;
    private EconomyManager economyManager;
    private ClassManager classManager;
    private WeaponManager weaponManager;
    private BountyManager bountyManager;
    private ScoreboardManager scoreboardManager;
    private AuctionManager auctionManager;
    private WebCommandManager webCommandManager;
    private ShopDataManager shopDataManager;
    private SpecialItems specialItems;
    private CraftingGUI craftingGUI;
    private AdminGUI adminGUI;

    @Override
    public void onEnable() {
        getLogger().info("=================================");
        getLogger().info("  KingdomCore - Loading...");
        getLogger().info("=================================");

        // 1. Load configurations
        configManager = new ConfigManager(this);
        configManager.load();

        // 2. Initialize MongoDB
        mongoManager = new MongoManager(getLogger());
        mongoManager.connect(
                configManager.getMongoUri(),
                configManager.getMongoDatabase(),
                configManager.getMongoCollection(),
                configManager.getAuctionCollection());

        // 3. Initialize Economy Manager
        economyManager = new EconomyManager(this, mongoManager);

        // 4. Initialize RPG Managers
        weaponManager = new WeaponManager(this);
        classManager = new ClassManager(this, configManager, economyManager);
        specialItems = new SpecialItems(this);

        // 5. Initialize Bounty Manager
        bountyManager = new BountyManager(economyManager);

        // 6. Initialize Scoreboard Manager
        scoreboardManager = new ScoreboardManager(this, configManager, economyManager);

        // 7. Initialize Auction Manager
        auctionManager = new AuctionManager(this, mongoManager);

        // 8. Initialize Shop System
        shopDataManager = new ShopDataManager(getLogger(), mongoManager);
        shopDataManager.loadAll();

        ShopGUI shopGUI = new ShopGUI(this);
        shopGUI.setShopDataManager(shopDataManager);
        ConverterShop converterShop = new ConverterShop(configManager, economyManager);
        ShopCommandHandler shopCommandHandler = new ShopCommandHandler(this, shopGUI, converterShop, shopDataManager);
        GUIListener guiListener = new GUIListener(this, shopGUI, economyManager, weaponManager, shopDataManager, shopCommandHandler);

        // 9. Initialize Crafting GUI
        craftingGUI = new CraftingGUI(this, weaponManager, economyManager, specialItems);

        // 10. Initialize Admin Panel
        adminGUI = new AdminGUI(this, economyManager, classManager, bountyManager, auctionManager, specialItems);

        // 11. Register Commands
        getCommand("shop").setExecutor(shopCommandHandler);
        getCommand("shop").setTabCompleter(shopCommandHandler);

        AuctionCommand auctionCommand = new AuctionCommand(this, auctionManager, economyManager);
        getCommand("ah").setExecutor(auctionCommand);
        getCommand("ah").setTabCompleter(auctionCommand);

        BountyCommand bountyCommand = new BountyCommand(this, bountyManager, configManager);
        getCommand("bounty").setExecutor(bountyCommand);
        getCommand("bounty").setTabCompleter(bountyCommand);

        ScoreboardCommand scoreboardCommand = new ScoreboardCommand(this, scoreboardManager, economyManager);
        getCommand("sb").setExecutor(scoreboardCommand);

        VerifyCommand verifyCommand = new VerifyCommand(this, mongoManager);
        getCommand("verify").setExecutor(verifyCommand);

        // Admin & Reset Commands
        AdminCommand adminCommand = new AdminCommand(adminGUI);
        getCommand("admin").setExecutor(adminCommand);

        ResetManager resetManager = new ResetManager(this, economyManager, configManager);
        getCommand("reset").setExecutor(resetManager);

        // Craft Command (opens Soul Forge)
        getCommand("craft").setExecutor((sender, cmd, label, args) -> {
            if (sender instanceof Player player) {
                craftingGUI.openCraftingGUI(player);
                return true;
            }
            return false;
        });

        getCommand("resourcepack").setExecutor((sender, cmd, label, args) -> {
            if (sender instanceof Player player) {
                sendResourcePack(player);
                return true;
            }
            return false;
        });

        // 12. Register Event Listeners
        getServer().getPluginManager().registerEvents(guiListener, this);
        getServer().getPluginManager().registerEvents(converterShop, this);
        getServer().getPluginManager().registerEvents(auctionCommand, this);
        getServer().getPluginManager().registerEvents(shopCommandHandler, this);
        getServer().getPluginManager().registerEvents(bountyCommand, this);
        getServer().getPluginManager().registerEvents(craftingGUI, this);
        getServer().getPluginManager().registerEvents(adminGUI, this);

        CombatListener combatListener = new CombatListener(economyManager, specialItems);
        getServer().getPluginManager().registerEvents(combatListener, this);

        ClassListener classListener = new ClassListener(configManager, classManager, weaponManager);
        getServer().getPluginManager().registerEvents(classListener, this);

        // Class Ability Listener (active abilities, XP awards)
        ClassAbilityListener classAbilityListener = new ClassAbilityListener(
                this, classManager, configManager, economyManager, weaponManager);
        getServer().getPluginManager().registerEvents(classAbilityListener, this);

        BountyListener bountyListener = new BountyListener(this, bountyManager, configManager);
        getServer().getPluginManager().registerEvents(bountyListener, this);

        PlayerJoinQuitListener joinQuitListener = new PlayerJoinQuitListener(
                this, economyManager, classManager, scoreboardManager, shopGUI);
        getServer().getPluginManager().registerEvents(joinQuitListener, this);

        // 13. Start Async Tasks
        economyManager.startSyncTasks();
        mongoManager.startPlayerWatchStream(economyManager);
        mongoManager.startPurchaseWatchStream(economyManager);
        scoreboardManager.startUpdateTask();
        bountyListener.startCompassTask();

        // Initialize Webstore Command Polling & Moderation
        this.webCommandManager = new WebCommandManager(this, mongoManager);
        
        me.pirot.kingdomCore.moderation.ModerationManager moderationManager = 
                new me.pirot.kingdomCore.moderation.ModerationManager(this, mongoManager);
        
        getServer().getPluginManager().registerEvents(
                new me.pirot.kingdomCore.moderation.PunishmentListener(moderationManager), this);
                
        getServer().getPluginManager().registerEvents(
                new me.pirot.kingdomCore.moderation.GameLogger(this, mongoManager), this);

        // 14. Automated AH Purge (Every 1 hour)
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            auctionManager.purgeExpired();
        }, 20 * 60 * 60L, 20 * 60 * 60L);

        getLogger().info("=================================");
        getLogger().info("  KingdomCore - Enabled!");
        getLogger().info("  New Systems: Crafting, Admin Panel, Abilities");
        getLogger().info("=================================");
    }

    @Override
    public void onDisable() {
        getLogger().info("[KingdomCore] Shutting down...");

        // Save all player data synchronously on shutdown
        if (economyManager != null) {
            try {
                economyManager.saveAll().get(); // Block until all saves complete
            } catch (Exception e) {
                getLogger().severe("[KingdomCore] Error saving player data on shutdown: " + e.getMessage());
            }
        }

        // Close MongoDB
        if (mongoManager != null) {
            mongoManager.close();
        }

        getLogger().info("[KingdomCore] Disabled.");
    }

    // ---- Getters for managers ----

    public ConfigManager getConfigManager() { return configManager; }
    public MongoManager getMongoManager() { return mongoManager; }
    public EconomyManager getEconomyManager() { return economyManager; }
    public ClassManager getClassManager() { return classManager; }
    public WeaponManager getWeaponManager() { return weaponManager; }
    public BountyManager getBountyManager() { return bountyManager; }
    public ScoreboardManager getScoreboardManager() { return scoreboardManager; }
    public AuctionManager getAuctionManager() { return auctionManager; }
    public SpecialItems getSpecialItems() { return specialItems; }
    public CraftingGUI getCraftingGUI() { return craftingGUI; }
    public AdminGUI getAdminGUI() { return adminGUI; }

    public void sendResourcePack(Player player) {
        String url = configManager.getConfig().getString("resource-pack.url");
        String hash = configManager.getConfig().getString("resource-pack.hash", "");
        boolean required = configManager.getConfig().getBoolean("resource-pack.required", false);

        if (url != null && !url.isEmpty()) {
            if (hash.isEmpty()) {
                player.setResourcePack(url);
            } else {
                player.setResourcePack(url, hash.getBytes(), required);
            }
        }
    }
}
