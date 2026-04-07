package me.pirot.kingdomCore.scoreboard;

import me.pirot.kingdomCore.KingdomCore;
import me.pirot.kingdomCore.economy.EconomyManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Handles the /sb command to toggle scoreboard visibility.
 */
public class ScoreboardCommand implements CommandExecutor {

    private final KingdomCore plugin;
    private final ScoreboardManager scoreboardManager;
    private final EconomyManager economyManager;

    public ScoreboardCommand(KingdomCore plugin, ScoreboardManager scoreboardManager,
                             EconomyManager economyManager) {
        this.plugin = plugin;
        this.scoreboardManager = scoreboardManager;
        this.economyManager = economyManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return true;
        }

        scoreboardManager.toggleBoard(player);

        // Save state to MongoDB
        economyManager.savePlayer(player.getUniqueId());

        return true;
    }
}
