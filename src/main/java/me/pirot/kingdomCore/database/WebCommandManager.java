package me.pirot.kingdomCore.database;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import me.pirot.kingdomCore.KingdomCore;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.UUID;

public class WebCommandManager {

    private final KingdomCore plugin;
    private final MongoManager mongoManager;
    private MongoCollection<Document> pendingCommandsCollection;

    public WebCommandManager(KingdomCore plugin, MongoManager mongoManager) {
        this.plugin = plugin;
        this.mongoManager = mongoManager;
        initCollection();
        startPolling();
    }

    private void initCollection() {
        if (mongoManager.getDatabase() != null) {
            this.pendingCommandsCollection = mongoManager.getDatabase().getCollection("pending_commands");
        } else {
            plugin.getLogger().warning("[KingdomCore] MongoDB Database is null. Cannot initialize WebCommandManager.");
        }
    }

    private void startPolling() {
        // Poll every 5 seconds (100 ticks)
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            if (pendingCommandsCollection == null) return;
            try {
                // Fetch commands that haven't been executed
                for (Document doc : pendingCommandsCollection.find(Filters.eq("executed", false))) {
                    processCommand(doc);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("[KingdomCore] Error polling pending commands: " + e.getMessage());
            }
        }, 100L, 100L);
    }

    private void processCommand(Document doc) {
        String command = doc.getString("command");
        String uuidStr = doc.getString("uuid");
        String sessionId = doc.getString("stripeSessionId");
        ObjectId id = doc.getObjectId("_id");

        if (command == null) {
            markExecuted(id, sessionId);
            return;
        }

        try {
            String finalCommand = command;
            
            if (uuidStr != null && !uuidStr.equalsIgnoreCase("CONSOLE")) {
                UUID uuid = UUID.fromString(uuidStr);
                Player player = Bukkit.getPlayer(uuid);
                
                // If the command is player-specific and the player is offline, skip it for now.
                // It will be picked up again during the next poll when they are online.
                if (player == null || !player.isOnline()) {
                    return;
                }

                String playerName = player.getName();

                // Parse placeholders
                finalCommand = command
                        .replace("{player}", playerName)
                        .replace("{uuid}", uuidStr);
            }

            final String commandToExecute = finalCommand;

            // Execute on main thread
            Bukkit.getScheduler().runTask(plugin, () -> {
                plugin.getLogger().info("[KingdomCore-Webstore] Executing command for " + (uuidStr != null ? uuidStr : "CONSOLE") + ": " + commandToExecute);
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), commandToExecute);
                
                // Mark as executed asynchronously after dispatching
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> markExecuted(id, sessionId));
            });

        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("[KingdomCore] Invalid UUID in pending command: " + uuidStr);
            markExecuted(id, sessionId);
        }
    }

    private void markExecuted(ObjectId id, String sessionId) {
        try {
            // 1. Mark the command as executed
            pendingCommandsCollection.updateOne(
                    Filters.eq("_id", id),
                    Updates.set("executed", true)
            );

            // 2. If it is linked to a purchase, update the purchase status on the store
            if (sessionId != null && !sessionId.isEmpty()) {
                mongoManager.getDatabase().getCollection("purchases").updateOne(
                        Filters.eq("stripeSessionId", sessionId),
                        Updates.combine(
                                Updates.set("status", "delivered"),
                                Updates.set("deliveredAt", new java.util.Date())
                        )
                );
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[KingdomCore] Failed to finalize command execution: " + e.getMessage());
        }
    }
}
