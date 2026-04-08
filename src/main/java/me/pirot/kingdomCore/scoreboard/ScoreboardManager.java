package me.pirot.kingdomCore.scoreboard;

import fr.mrmicky.fastboard.FastBoard;
import me.pirot.kingdomCore.KingdomCore;
import me.pirot.kingdomCore.config.ConfigManager;
import me.pirot.kingdomCore.database.PlayerData;
import me.pirot.kingdomCore.economy.EconomyManager;
import me.pirot.kingdomCore.rpg.RPGClass;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages FastBoard scoreboards per player with auto-refresh.
 * Redesigned to match the premium KingdomSMP aesthetic.
 */
public class ScoreboardManager {

    private final KingdomCore plugin;
    private final ConfigManager configManager;
    private final EconomyManager economyManager;

    private final Map<UUID, FastBoard> boards = new ConcurrentHashMap<>();

    public ScoreboardManager(KingdomCore plugin, ConfigManager configManager,
                             EconomyManager economyManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.economyManager = economyManager;
    }

    /**
     * Create a scoreboard for a player (if their preference is enabled).
     */
    public void createBoard(Player player) {
        PlayerData data = economyManager.getPlayerData(player.getUniqueId());
        if (data == null || !data.isScoreboardEnabled()) return;

        FastBoard board = new FastBoard(player);
        board.updateTitle("§e§l¥ KingdomSMP ¥");
        boards.put(player.getUniqueId(), board);
        updateBoard(player);
    }

    /**
     * Update the scoreboard lines for a player.
     * Matches the design: Player, Class, Level, XP, Shards, Gems, K/D, Bounty, Online.
     */
    public void updateBoard(Player player) {
        FastBoard board = boards.get(player.getUniqueId());
        if (board == null || board.isDeleted()) return;

        PlayerData data = economyManager.getPlayerData(player.getUniqueId());
        if (data == null) return;

        RPGClass rpgClass = RPGClass.fromString(data.getClassName());
        String className = rpgClass != null ? rpgClass.getColoredName() : "§7None";

        int xp = data.getXp();
        int xpNeeded = data.getXpNeeded();
        int onlineCount = Bukkit.getOnlinePlayers().size();

        List<String> lines = new ArrayList<>();
        lines.add("§1 "); // 14
        lines.add("§fPlayer: §f" + player.getName()); // 13
        lines.add("§fClass: " + className); // 12
        lines.add("§fLevel: §b" + data.getLevel()); // 11
        lines.add("§fXP: §e" + xp + "/" + xpNeeded); // 10
        lines.add("§2 "); // 9
        lines.add("§a✦ Shards: §f" + formatNumber(data.getShards())); // 8
        lines.add("§b✦ Gems: §f" + formatNumber(data.getGems())); // 7
        lines.add("§3 "); // 6
        lines.add("§c⚔ K/D: §f" + data.getKills() + "/" + data.getDeaths()); // 5
        lines.add("§6☠ Bounty: §e" + formatNumber(data.getBounty())); // 4
        lines.add("§4 "); // 3
        lines.add("§fOnline: §f" + onlineCount); // 2
        lines.add("§7play.kingdom.com"); // 1

        board.updateLines(lines);
    }

    /**
     * Toggle scoreboard visibility for a player.
     */
    public void toggleBoard(Player player) {
        UUID uuid = player.getUniqueId();
        PlayerData data = economyManager.getPlayerData(uuid);
        if (data == null) return;

        if (boards.containsKey(uuid)) {
            removeBoard(player);
            data.setScoreboardEnabled(false);
            player.sendMessage("§a§l[Kingdom] §7Scoreboard §cdisabled§7.");
        } else {
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

    private String formatNumber(int number) {
        if (number >= 1000000) return String.format("%.1fM", number / 1000000.0);
        if (number >= 1000) return String.format("%.1fK", number / 1000.0);
        return String.valueOf(number);
    }
}
