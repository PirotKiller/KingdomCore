package me.pirot.kingdomCore;

import me.pirot.kingdomCore.economy.EconomyManager;
import me.pirot.kingdomCore.rpg.ClassManager;
import me.pirot.kingdomCore.scoreboard.ScoreboardManager;
import me.pirot.kingdomCore.shop.ShopGUI;
import me.pirot.kingdomCore.shop.ShopType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Handles player join and quit events — loading/saving data, applying
 * class passives, and creating/removing scoreboards.
 */
public class PlayerJoinQuitListener implements Listener {

    private final KingdomCore plugin;
    private final EconomyManager economyManager;
    private final ClassManager classManager;
    private final ScoreboardManager scoreboardManager;
    private final ShopGUI shopGUI;

    public PlayerJoinQuitListener(KingdomCore plugin, EconomyManager economyManager,
                                  ClassManager classManager, ScoreboardManager scoreboardManager,
                                  ShopGUI shopGUI) {
        this.plugin = plugin;
        this.economyManager = economyManager;
        this.classManager = classManager;
        this.scoreboardManager = scoreboardManager;
        this.shopGUI = shopGUI;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Load player data from MongoDB asynchronously
        economyManager.loadPlayer(player.getUniqueId()).thenAccept(data -> {
            // Back on main thread for Bukkit API calls
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (!player.isOnline()) return;

                // Set online status and save instantly
                data.setOnline(true);
                data.setLastKnownName(player.getName());
                economyManager.savePlayer(player.getUniqueId());

                // Apply class passives
                classManager.applyPassives(player);

                // Create scoreboard if enabled
                scoreboardManager.createBoard(player);

                // Welcome message and class prompt
                String className = data.getClassName();
                if (className != null && !className.equals("NONE")) {
                    player.sendMessage("§a§l[Kingdom] §7Welcome back, " +
                            me.pirot.kingdomCore.rpg.RPGClass.fromString(className).getColoredName() + "§7!");
                } else {
                    player.sendMessage("§a§l[Kingdom] §eWelcome! Embark on your legendary quest today.");
                }

                // Prompt for resource pack if enabled
                if (plugin.getConfigManager().getConfig().getBoolean("resource-pack.prompt-on-join", true)) {
                    plugin.sendResourcePack(player);
                }
            });
        });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // Remove scoreboard
        scoreboardManager.removeBoard(player);

        // Remove class passives
        classManager.removePassives(player);

        // Set offline status, save, and unload
        me.pirot.kingdomCore.database.PlayerData data = economyManager.getPlayerData(player.getUniqueId());
        if (data != null) {
            data.setOnline(false);
        }

        economyManager.savePlayer(player.getUniqueId()).thenRun(() -> {
            economyManager.unloadPlayer(player.getUniqueId());
        });
    }
}
