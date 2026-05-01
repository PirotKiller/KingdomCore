package me.pirot.kingdomCore.web;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import me.pirot.kingdomCore.KingdomCore;
import me.pirot.kingdomCore.database.MongoManager;
import org.bson.Document;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Handles /verify <code> command for linking Minecraft accounts to the
 * webstore.
 */
public class VerifyCommand implements CommandExecutor {

    private final KingdomCore plugin;
    private final MongoManager mongoManager;

    public VerifyCommand(KingdomCore plugin, MongoManager mongoManager) {
        this.plugin = plugin;
        this.mongoManager = mongoManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return true;
        }

        if (args.length < 1) {
            player.sendMessage("§c§l[Kingdom] §7Usage: §f/verify <code>");
            player.sendMessage("§7Get your code from the webstore at §bhttps://shop.thekingdomsmp.com/account");
            return true;
        }

        String code = args[0].toUpperCase();

        // Run async to avoid blocking main thread
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                MongoCollection<Document> verifications = mongoManager.getDatabase().getCollection("verifications");
                MongoCollection<Document> webusers = mongoManager.getDatabase().getCollection("webusers");

                // Find the verification code
                Document verification = verifications.find(Filters.eq("code", code)).first();

                if (verification == null) {
                    plugin.getServer().getScheduler().runTask(plugin, () -> player.sendMessage(
                            "§c§l[Kingdom] §7Invalid or expired code! Generate a new one from the webstore."));
                    return;
                }

                String discordId = verification.getString("discordId");

                // Link the Minecraft account to the web user
                webusers.updateOne(
                        Filters.eq("discordId", discordId),
                        new Document("$set", new Document()
                                .append("minecraftUuid", player.getUniqueId().toString())
                                .append("minecraftUsername", player.getName())));

                // Delete the used verification code
                verifications.deleteOne(Filters.eq("code", code));

                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    player.sendMessage("§a§l[Kingdom] §7Account linked successfully!");
                    player.sendMessage("§a§l[Kingdom] §7Discord account connected. You can now buy from the webstore!");
                });
            } catch (Exception e) {
                plugin.getLogger().warning("[KingdomCore] Verification error: " + e.getMessage());
                plugin.getServer().getScheduler().runTask(plugin,
                        () -> player.sendMessage("§c§l[Kingdom] §7An error occurred. Please try again."));
            }
        });

        return true;
    }
}
