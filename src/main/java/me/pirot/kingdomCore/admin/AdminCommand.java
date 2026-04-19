package me.pirot.kingdomCore.admin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * /admin command — opens the in-game admin panel.
 */
public class AdminCommand implements CommandExecutor {

    private final AdminGUI adminGUI;

    public AdminCommand(AdminGUI adminGUI) {
        this.adminGUI = adminGUI;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return true;
        }

        if (!player.hasPermission("kingdomcore.admin")) {
            player.sendMessage("§c§l[Kingdom] §7You don't have permission!");
            return true;
        }

        if (args.length >= 2 && args[0].equalsIgnoreCase("player")) {
            // /admin player <name> — open specific player profile
            org.bukkit.entity.Player target = org.bukkit.Bukkit.getPlayerExact(args[1]);
            if (target != null) {
                adminGUI.openPlayerProfile(player, target.getUniqueId());
            } else {
                player.sendMessage("§c§l[Admin] §7Player not found or offline.");
            }
            return true;
        }

        adminGUI.openMainDashboard(player);
        return true;
    }
}
