package me.pirot.kingdomCore.reset;

import me.pirot.kingdomCore.KingdomCore;
import me.pirot.kingdomCore.config.ConfigManager;
import me.pirot.kingdomCore.database.PlayerData;
import me.pirot.kingdomCore.economy.EconomyManager;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages server resets. Resets player data (XP, level, shards, kills, deaths, bounty, inventory)
 * while preserving gems (premium currency) and class choice.
 */
public class ResetManager implements CommandExecutor {

    private final KingdomCore plugin;
    private final EconomyManager economyManager;
    private final ConfigManager configManager;

    // Pending confirmations: UUID -> timestamp of /reset request
    private final Map<UUID, Long> pendingConfirmations = new ConcurrentHashMap<>();

    public ResetManager(KingdomCore plugin, EconomyManager economyManager, ConfigManager configManager) {
        this.plugin = plugin;
        this.economyManager = economyManager;
        this.configManager = configManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull [] args) {
        if (!sender.hasPermission("kingdomcore.admin")) {
            sender.sendMessage("§c§l[Kingdom] §7You don't have permission!");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("");
            sender.sendMessage("§c§l⚠ SERVER RESET ⚠");
            sender.sendMessage("§7This will reset ALL player data:");
            sender.sendMessage("§7 • XP, Levels, Kills, Deaths, Bounties");
            sender.sendMessage("§7 • Shards → rolled back to §e" + configManager.getResetShardMinimum());
            sender.sendMessage("§7 • Inventories cleared");
            sender.sendMessage("§7 • Auction listings removed");
            sender.sendMessage("");
            sender.sendMessage("§aPreserved: §fGems, Class choice");
            sender.sendMessage("");
            sender.sendMessage("§eType §6/reset confirm §eto proceed.");
            sender.sendMessage("");

            if (sender instanceof Player player) {
                pendingConfirmations.put(player.getUniqueId(), System.currentTimeMillis());
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("confirm")) {
            if (sender instanceof Player player) {
                Long timestamp = pendingConfirmations.remove(player.getUniqueId());
                if (timestamp == null || System.currentTimeMillis() - timestamp > 30000) {
                    player.sendMessage("§c§l[Kingdom] §7Confirmation expired. Type §e/reset §7first.");
                    return true;
                }
            }

            executeReset(sender);
            return true;
        }

        return true;
    }

    private void executeReset(CommandSender sender) {
        sender.sendMessage("§c§l[Kingdom] §7Executing server reset...");

        int shardMin = configManager.getResetShardMinimum();
        boolean preserveGems = configManager.isResetPreserveGems();

        // Broadcast warning
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage("§c§l§m     §c§l[ §f§lSERVER RESET §c§l]§c§l§m     ");
        Bukkit.broadcastMessage("§7The server has been reset by an admin.");
        Bukkit.broadcastMessage("§7All progress has been wiped.");
        Bukkit.broadcastMessage("§aGems and class choice have been preserved.");
        Bukkit.broadcastMessage("§c§l§m                              ");
        Bukkit.broadcastMessage("");

        // Reset all cached players
        for (Map.Entry<UUID, PlayerData> entry : economyManager.getCache().entrySet()) {
            PlayerData data = entry.getValue();

            int savedGems = data.getGems();
            String savedClass = data.getClassName();

            data.setXp(0);
            data.setLevel(1);
            data.setShards(shardMin);
            data.setBounty(0);
            data.setKills(0);
            data.setDeaths(0);
            data.setClassProgress(new HashMap<>());

            if (preserveGems) {
                data.setGems(savedGems);
            } else {
                data.setGems(0);
            }
            data.setClassName(savedClass);
        }

        // Clear inventories of online players
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.getInventory().clear();
            player.setLevel(0);
            player.setExp(0);
            player.setFoodLevel(20);
            player.setHealth(player.getMaxHealth());
            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_DEATH, 1f, 1f);
        }

        // Save to database
        economyManager.saveAll();

        sender.sendMessage("§a§l[Kingdom] §7Server reset complete. " + economyManager.getCache().size() + " players affected.");
    }
}
