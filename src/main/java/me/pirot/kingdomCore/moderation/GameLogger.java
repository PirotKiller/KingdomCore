package me.pirot.kingdomCore.moderation;

import com.mongodb.client.MongoCollection;
import me.pirot.kingdomCore.KingdomCore;
import me.pirot.kingdomCore.database.MongoManager;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Date;

public class GameLogger implements Listener {

    private final KingdomCore plugin;
    private final MongoCollection<Document> logsCollection;

    public GameLogger(KingdomCore plugin, MongoManager mongoManager) {
        this.plugin = plugin;
        this.logsCollection = mongoManager.getDatabase() != null ? mongoManager.getDatabase().getCollection("game_logs") : null;
    }

    private void logEvent(String eventType, String playerName, String uuid, String details) {
        if (logsCollection == null) return;

        Document logDoc = new Document("eventType", eventType)
                .append("playerName", playerName)
                .append("uuid", uuid)
                .append("details", details)
                .append("timestamp", new Date());

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                logsCollection.insertOne(logDoc);
            } catch (Exception e) {
                // Silently fail to avoid console spam, or log debug
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        logEvent("JOIN", event.getPlayer().getName(), event.getPlayer().getUniqueId().toString(), "Player joined the server.");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        logEvent("QUIT", event.getPlayer().getName(), event.getPlayer().getUniqueId().toString(), "Player left the server.");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        logEvent("CHAT", event.getPlayer().getName(), event.getPlayer().getUniqueId().toString(), event.getMessage());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        // Skip login/register commands to avoid storing passwords
        String msg = event.getMessage().toLowerCase();
        if (msg.startsWith("/login") || msg.startsWith("/l") || msg.startsWith("/register") || msg.startsWith("/reg")) {
            return;
        }
        logEvent("COMMAND", event.getPlayer().getName(), event.getPlayer().getUniqueId().toString(), event.getMessage());
    }
}
