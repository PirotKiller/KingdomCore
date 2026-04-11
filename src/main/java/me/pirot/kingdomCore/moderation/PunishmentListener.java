package me.pirot.kingdomCore.moderation;

import org.bson.Document;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Date;
import java.util.UUID;

public class PunishmentListener implements Listener {

    private final ModerationManager moderationManager;

    public PunishmentListener(ModerationManager moderationManager) {
        this.moderationManager = moderationManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        UUID uuid = event.getUniqueId();
        Document ban = moderationManager.getActiveBan(uuid);

        if (ban != null) {
            String reason = ban.getString("reason");
            Date expireAt = ban.getDate("expireAt");

            String banMsg = ChatColor.RED + "You are banned from this server!\n\n" +
                    ChatColor.YELLOW + "Reason: " + ChatColor.WHITE + (reason != null ? reason : "No reason provided") + "\n" +
                    ChatColor.YELLOW + "Expires: " + ChatColor.WHITE + (expireAt == null ? "Never" : expireAt.toString());

            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, banMsg);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        Document mute = moderationManager.getActiveMute(uuid);

        if (mute != null) {
            event.setCancelled(true);
            String reason = mute.getString("reason");
            Date expireAt = mute.getDate("expireAt");

            String muteMsg = ChatColor.RED + "You are currently muted and cannot speak.\n" +
                    ChatColor.YELLOW + "Reason: " + ChatColor.WHITE + (reason != null ? reason : "No reason provided") + "\n" +
                    ChatColor.YELLOW + "Expires: " + ChatColor.WHITE + (expireAt == null ? "Never" : expireAt.toString());

            event.getPlayer().sendMessage(muteMsg);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Clean up mute cache when player leaves
        moderationManager.removeFromCache(event.getPlayer().getUniqueId());
    }
}
