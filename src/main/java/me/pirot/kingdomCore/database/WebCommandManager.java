package me.pirot.kingdomCore.database;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import me.pirot.kingdomCore.KingdomCore;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

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
        ObjectId id = doc.getObjectId("_id");

        if (command == null || uuidStr == null) {
            markExecuted(id);
            return;
        }

        try {
            UUID uuid = UUID.fromString(uuidStr);
            OfflinePlayer target = Bukkit.getOfflinePlayer(uuid);
            String playerName = target.getName() != null ? target.getName() : "UnknownPlayer";

            // Parse placeholders
            String finalCommand = command
                    .replace("{player}", playerName)
                    .replace("{uuid}", uuidStr);

            // Execute on main thread
            Bukkit.getScheduler().runTask(plugin, () -> {
                plugin.getLogger().info("[KingdomCore-Webstore] Executing command: " + finalCommand);
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand);
                
                // Mark as executed asynchronously after dispatching
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> markExecuted(id));
            });

        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("[KingdomCore] Invalid UUID in pending command: " + uuidStr);
            markExecuted(id);
        }
    }

    private void markExecuted(ObjectId id) {
        try {
            pendingCommandsCollection.updateOne(
                    Filters.eq("_id", id),
                    Updates.set("executed", true)
            );
        } catch (Exception e) {
            plugin.getLogger().warning("[KingdomCore] Failed to mark command as executed: " + id);
        }
    }
}
