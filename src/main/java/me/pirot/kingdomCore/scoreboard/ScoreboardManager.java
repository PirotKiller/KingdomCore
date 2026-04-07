package me.pirot.kingdomCore.scoreboard;

import fr.mrmicky.fastboard.FastBoard;
import me.pirot.kingdomCore.KingdomCore;
import me.pirot.kingdomCore.bounty.BountyManager;
import me.pirot.kingdomCore.config.ConfigManager;
import me.pirot.kingdomCore.database.PlayerData;
import me.pirot.kingdomCore.economy.EconomyManager;
import me.pirot.kingdomCore.rpg.RPGClass;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages FastBoard scoreboards per player with auto-refresh.
 */
public class ScoreboardManager {

    private final KingdomCore plugin;
    private final ConfigManager configManager;
    private final EconomyManager economyManager;
    private final BountyManager bountyManager;

    private final Map<UUID, FastBoard> boards = new ConcurrentHashMap<>();
    private final String title;

    public ScoreboardManager(KingdomCore plugin, ConfigManager configManager,
                             EconomyManager economyManager, BountyManager bountyManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.economyManager = economyManager;
        this.bountyManager = bountyManager;
        this.title = configManager.getScoreboardTitle();
    }

    /**
     * Create a scoreboard for a player (if their preference is enabled).
     */
    public void createBoard(Player player) {
        PlayerData data = economyManager.getPlayerData(player.getUniqueId());
        if (data == null || !data.isScoreboardEnabled()) return;

        FastBoard board = new FastBoard(player);
        board.updateTitle(title);
        boards.put(player.getUniqueId(), board);
        updateBoard(player);
    }

    /**
     * Update the scoreboard lines for a player.
     */
    public void updateBoard(Player player) {
        FastBoard board = boards.get(player.getUniqueId());
        if (board == null || board.isDeleted()) return;

        PlayerData data = economyManager.getPlayerData(player.getUniqueId());
        if (data == null) return;

        RPGClass rpgClass = RPGClass.fromString(data.getClassName());
        String className = rpgClass != null ? rpgClass.getColoredName() : "§7None";

        // Top bounty info
        Player topBounty = bountyManager.getTopBountyPlayer();

        // XP progress bar (Premium Look)
        int xpNeeded = data.getLevel() * 1000;
        if (xpNeeded <= 0) xpNeeded = 1000; // Default case
        int xpProgress = data.getXp();
        int totalBars = 10;
        int filledBars = (int) Math.min(totalBars, Math.max(0, ((double) xpProgress / xpNeeded) * totalBars));

        StringBuilder xpBar = new StringBuilder("§a");
        for (int i = 0; i < totalBars; i++) {
            if (i == filledBars) xpBar.append("§8");
            xpBar.append("■");
        }
        xpBar.append("§r");

        String topBountyName = "None";
        String topBountyVal = "0";
        if (topBounty != null) {
            topBountyName = topBounty.getName();
            topBountyVal = String.valueOf(bountyManager.getTopBountyAmount());
        }

        board.updateLines(
                "§8§m------------------------",
                "§6❖ §lPlayer Info",
                "  §7Class: " + className,
                "  §7Level: §a" + data.getLevel(),
                "  §7Prog:  " + xpBar,
                "",
                "§e❖ §lTreasury",
                "  §6Shards: §f" + data.getShards() + " ✦",
                "  §bGems:   §f" + data.getGems() + " ✦",
                " ",
                "§c❖ §lLeaderboard",
                "  §fTop Bnt: §c" + topBountyName,
                "  §7Value: §a" + topBountyVal,
                "§8§m------------------------",
                "  §epic.thekingdom.net"
        );
    }

    /**
     * Toggle scoreboard visibility for a player.
     */
    public void toggleBoard(Player player) {
        UUID uuid = player.getUniqueId();
        PlayerData data = economyManager.getPlayerData(uuid);
        if (data == null) return;

        if (boards.containsKey(uuid)) {
            // Currently showing — hide it
            removeBoard(player);
            data.setScoreboardEnabled(false);
            player.sendMessage("§a§l[Kingdom] §7Scoreboard §cdisabled§7.");
        } else {
            // Currently hidden — show it
            data.setScoreboardEnabled(true);
            createBoard(player);
            player.sendMessage("§a§l[Kingdom] §7Scoreboard §aenabled§7.");
        }
    }

    /**
     * Remove a player's scoreboard (on quit or toggle off).
     */
    public void removeBoard(Player player) {
        FastBoard board = boards.remove(player.getUniqueId());
        if (board != null && !board.isDeleted()) {
            board.delete();
        }
    }

    /**
     * Start the repeating update task.
     */
    public void startUpdateTask() {
        int ticks = configManager.getScoreboardUpdateTicks();
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (boards.containsKey(player.getUniqueId())) {
                    updateBoard(player);
                }
            }
        }, ticks, ticks);
    }
}
