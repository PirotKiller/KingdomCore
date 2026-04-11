package me.pirot.kingdomCore.moderation;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import me.pirot.kingdomCore.KingdomCore;
import me.pirot.kingdomCore.database.MongoManager;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.Date;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ModerationManager {

    private final KingdomCore plugin;
    private final MongoManager mongoManager;
    private MongoCollection<Document> queueCollection;
    private MongoCollection<Document> punishmentsCollection;

    // Cache for online players (UUID -> Active Mute Document)
    private final ConcurrentHashMap<UUID, Document> activeMutes = new ConcurrentHashMap<>();

    public ModerationManager(KingdomCore plugin, MongoManager mongoManager) {
        this.plugin = plugin;
        this.mongoManager = mongoManager;
        initCollections();
        startPolling();
    }

    private void initCollections() {
        if (mongoManager.getDatabase() != null) {
            this.queueCollection = mongoManager.getDatabase().getCollection("moderation_queue");
            this.punishmentsCollection = mongoManager.getDatabase().getCollection("punishments");
        } else {
            plugin.getLogger().warning("[KingdomCore] MongoDB Database is null. Cannot initialize ModerationManager.");
        }
    }

    private void startPolling() {
        // Poll every 5 seconds (100 ticks)
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            if (queueCollection == null) return;
            try {
                for (Document doc : queueCollection.find(Filters.eq("executed", false))) {
                    processAction(doc);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("[KingdomCore] Error polling moderation queue: " + e.getMessage());
            }
        }, 100L, 100L);
    }

    private void processAction(Document doc) {
        String action = doc.getString("action");
        String playerName = doc.getString("playerName");
        String reason = doc.getString("reason");
        if (reason == null || reason.isEmpty()) reason = "No reason provided.";
        String durationStr = doc.getString("duration");
        ObjectId id = doc.getObjectId("_id");

        if (action == null || playerName == null) {
            markExecuted(id);
            return;
        }

        @SuppressWarnings("deprecation")
        OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
        UUID uuid = target.getUniqueId();
        
        Date expireAt = null;
        if (durationStr != null && !durationStr.isEmpty()) {
            long durationMillis = parseDuration(durationStr);
            if (durationMillis > 0) {
                expireAt = new Date(System.currentTimeMillis() + durationMillis);
            }
        }

        try {
            switch (action.toLowerCase()) {
                case "kick":
                    kickPlayerSync(playerName, ChatColor.RED + "You have been kicked.\nReason: " + ChatColor.WHITE + reason);
                    break;
                case "ban":
                case "tempban":
                    Document banDoc = new Document("uuid", uuid.toString())
                            .append("playerName", target.getName())
                            .append("type", "BAN")
                            .append("reason", reason)
                            .append("expireAt", expireAt)
                            .append("issuedAt", new Date())
                            .append("active", true);
                    
                    // Upsert ban
                    punishmentsCollection.updateOne(
                            Filters.and(Filters.eq("uuid", uuid.toString()), Filters.eq("type", "BAN")),
                            new Document("$set", banDoc),
                            new UpdateOptions().upsert(true)
                    );

                    String banMsg = ChatColor.RED + "You are banned from this server!\n" +
                            ChatColor.YELLOW + "Reason: " + ChatColor.WHITE + reason + "\n" +
                            ChatColor.YELLOW + "Expires: " + ChatColor.WHITE + (expireAt == null ? "Never" : expireAt.toString());
                    kickPlayerSync(playerName, banMsg);
                    break;
                case "unban":
                    punishmentsCollection.updateMany(
                            Filters.and(Filters.eq("uuid", uuid.toString()), Filters.eq("type", "BAN")),
                            Updates.set("active", false)
                    );
                    break;
                case "mute":
                    Document muteDoc = new Document("uuid", uuid.toString())
                            .append("playerName", target.getName())
                            .append("type", "MUTE")
                            .append("reason", reason)
                            .append("expireAt", expireAt)
                            .append("issuedAt", new Date())
                            .append("active", true);

                    punishmentsCollection.updateOne(
                            Filters.and(Filters.eq("uuid", uuid.toString()), Filters.eq("type", "MUTE")),
                            new Document("$set", muteDoc),
                            new UpdateOptions().upsert(true)
                    );
                    
                    activeMutes.put(uuid, muteDoc);
                    sendMessageSync(playerName, ChatColor.RED + "You have been muted for: " + ChatColor.WHITE + reason);
                    break;
                case "unmute":
                    punishmentsCollection.updateMany(
                            Filters.and(Filters.eq("uuid", uuid.toString()), Filters.eq("type", "MUTE")),
                            Updates.set("active", false)
                    );
                    activeMutes.remove(uuid);
                    sendMessageSync(playerName, ChatColor.GREEN + "You have been unmuted.");
                    break;
                case "warn":
                    String warnMsg = ChatColor.RED + "" + ChatColor.BOLD + "WARNING!\n" +
                            ChatColor.YELLOW + reason;
                    // Kick them slightly? No just title
                    sendTitleSync(playerName, ChatColor.RED + "WARNING", ChatColor.YELLOW + reason);
                    sendMessageSync(playerName, ChatColor.RED + "[Moderation] You received a warning: " + ChatColor.WHITE + reason);
                    break;
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[KingdomCore] Failed to process moderation action: " + e.getMessage());
        } finally {
            markExecuted(id);
        }
    }

    private void kickPlayerSync(String playerName, String message) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            Player p = Bukkit.getPlayerExact(playerName);
            if (p != null && p.isOnline()) {
                p.kickPlayer(message);
            }
        });
    }

    private void sendMessageSync(String playerName, String message) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            Player p = Bukkit.getPlayerExact(playerName);
            if (p != null && p.isOnline()) {
                p.sendMessage(message);
            }
        });
    }

    private void sendTitleSync(String playerName, String title, String subtitle) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            Player p = Bukkit.getPlayerExact(playerName);
            if (p != null && p.isOnline()) {
                p.sendTitle(title, subtitle, 10, 100, 20);
            }
        });
    }

    private void markExecuted(ObjectId id) {
        try {
            queueCollection.updateOne(
                    Filters.eq("_id", id),
                    Updates.set("executed", true)
            );
        } catch (Exception e) {
            plugin.getLogger().warning("[KingdomCore] Failed to mark moderation action executed: " + id);
        }
    }

    public Document getActiveBan(UUID uuid) {
        if (punishmentsCollection == null) return null;
        Document ban = punishmentsCollection.find(
                Filters.and(
                        Filters.eq("uuid", uuid.toString()),
                        Filters.eq("type", "BAN"),
                        Filters.eq("active", true)
                )
        ).first();

        if (ban != null) {
            Date expireAt = ban.getDate("expireAt");
            if (expireAt != null && expireAt.before(new Date())) {
                // Expired
                punishmentsCollection.updateOne(Filters.eq("_id", ban.getObjectId("_id")), Updates.set("active", false));
                return null;
            }
            return ban;
        }
        return null;
    }

    public Document getActiveMute(UUID uuid) {
        // Check cache first for performance
        if (activeMutes.containsKey(uuid)) {
            Document mute = activeMutes.get(uuid);
            Date expireAt = mute.getDate("expireAt");
            if (expireAt != null && expireAt.before(new Date())) {
                activeMutes.remove(uuid);
                punishmentsCollection.updateOne(Filters.eq("_id", mute.getObjectId("_id")), Updates.set("active", false));
                return null;
            }
            return mute;
        }

        if (punishmentsCollection == null) return null;
        
        // Fetch from DB
        Document mute = punishmentsCollection.find(
                Filters.and(
                        Filters.eq("uuid", uuid.toString()),
                        Filters.eq("type", "MUTE"),
                        Filters.eq("active", true)
                )
        ).first();

        if (mute != null) {
            Date expireAt = mute.getDate("expireAt");
            if (expireAt != null && expireAt.before(new Date())) {
                punishmentsCollection.updateOne(Filters.eq("_id", mute.getObjectId("_id")), Updates.set("active", false));
                return null;
            }
            activeMutes.put(uuid, mute);
            return mute;
        }
        return null;
    }
    
    public void removeFromCache(UUID uuid) {
        activeMutes.remove(uuid);
    }

    private long parseDuration(String durationStr) {
        long millis = 0;
        Pattern pattern = Pattern.compile("(\\d+)(d|h|m|s)");
        Matcher matcher = pattern.matcher(durationStr.toLowerCase());
        
        while (matcher.find()) {
            long value = Long.parseLong(matcher.group(1));
            String unit = matcher.group(2);
            switch (unit) {
                case "d": millis += value * 24 * 60 * 60 * 1000; break;
                case "h": millis += value * 60 * 60 * 1000; break;
                case "m": millis += value * 60 * 1000; break;
                case "s": millis += value * 1000; break;
            }
        }
        return millis;
    }
}
