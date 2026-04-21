package me.pirot.kingdomCore.economy;

import me.pirot.kingdomCore.KingdomCore;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class EcoCommand implements CommandExecutor, TabCompleter, Listener {

    private final KingdomCore plugin;
    private final EconomyManager economyManager;
    private final NamespacedKey NOTE_VALUE_KEY;
    private final NamespacedKey NOTE_ISSUER_KEY;

    public EcoCommand(KingdomCore plugin, EconomyManager economyManager) {
        this.plugin = plugin;
        this.economyManager = economyManager;
        this.NOTE_VALUE_KEY = new NamespacedKey(plugin, "banknote_value");
        this.NOTE_ISSUER_KEY = new NamespacedKey(plugin, "banknote_issuer");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("§cConsole must use subcommands: /eco <give|take|set> <player> <amount> [shards|gems]");
                return true;
            }
            player.sendMessage("§e§l[Kingdom] §7Your Balance:");
            player.sendMessage("§6✦ Shards: §a" + String.format("%,d", economyManager.getShards(player.getUniqueId())));
            player.sendMessage("§b✦ Gems: §b" + String.format("%,d", economyManager.getGems(player.getUniqueId())));
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("note") || sub.equals("withdraw")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("§cOnly players can withdraw notes.");
                return true;
            }

            if (args.length < 2) {
                player.sendMessage("§cUsage: /eco note <amount>");
                return true;
            }

            String amountStr = args[1];
            int amount = parseAmount(amountStr);

            if (amount <= 0) {
                player.sendMessage("§cInvalid amount. Must be greater than 0.");
                return true;
            }

            int currentBalance = economyManager.getShards(player.getUniqueId());
            if (currentBalance < amount) {
                player.sendMessage("§cYou don't have enough Shards to withdraw " + String.format("%,d", amount) + "!");
                return true;
            }

            // Withdraw from player
            economyManager.removeShards(player.getUniqueId(), amount);

            // Create Note Item
            ItemStack note = new ItemStack(Material.PAPER);
            ItemMeta meta = note.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§6§lBank Note §8(§a" + String.format("%,d", amount) + " Shards§8)");
                
                List<String> lore = new ArrayList<>();
                lore.add("§8§m                              ");
                lore.add("§7Value: §6" + String.format("%,d", amount) + " Shards");
                lore.add("§7Signer: §f" + player.getName());
                lore.add("");
                lore.add("§a▸ Right-click to deposit into your balance");
                lore.add("§8§m                              ");
                meta.setLore(lore);

                PersistentDataContainer pdc = meta.getPersistentDataContainer();
                pdc.set(NOTE_VALUE_KEY, PersistentDataType.INTEGER, amount);
                pdc.set(NOTE_ISSUER_KEY, PersistentDataType.STRING, player.getName());

                note.setItemMeta(meta);
            }

            // Give to player
            if (player.getInventory().firstEmpty() != -1) {
                player.getInventory().addItem(note);
            } else {
                player.getWorld().dropItemNaturally(player.getLocation(), note);
                player.sendMessage("§eYour inventory was full, so the note was dropped on the ground!");
            }

            player.sendMessage("§aSuccessfully withdrew §6" + String.format("%,d", amount) + " Shards§a as a Bank Note.");
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 2.0f);
            return true;
        }

        // Admin Commands
        if (sub.equals("give") || sub.equals("take") || sub.equals("set")) {
            if (!sender.hasPermission("kingdomcore.admin")) {
                sender.sendMessage("§cYou don't have permission to use admin eco commands.");
                return true;
            }

            if (args.length < 3) {
                sender.sendMessage("§cUsage: /eco " + sub + " <player> <amount> [shards|gems]");
                return true;
            }

            Player target = org.bukkit.Bukkit.getPlayer(args[1]);
            if (target == null || !target.isOnline()) {
                sender.sendMessage("§cPlayer not found or offline.");
                return true;
            }

            int amount = parseAmount(args[2]);
            if (amount < 0) {
                sender.sendMessage("§cInvalid amount.");
                return true;
            }

            boolean isGems = args.length >= 4 && args[3].equalsIgnoreCase("gems");
            boolean isShards = !isGems;

            if (sub.equals("give")) {
                if (isShards) {
                    economyManager.addShards(target.getUniqueId(), amount);
                    sender.sendMessage("§a§l[Kingdom] §7Added §a" + amount + " Shards §7to §f" + target.getDisplayName());
                    
                    // --- LOGGING ---
                    plugin.getMongoManager().logAction(new org.bson.Document()
                            .append("source", "GAME")
                            .append("type", "ECO_ADMIN")
                            .append("player", new org.bson.Document("uuid", (sender instanceof Player p ? p.getUniqueId().toString() : "CONSOLE")).append("name", sender.getName()))
                            .append("target", new org.bson.Document("uuid", target.getUniqueId().toString()).append("name", target.getName()))
                            .append("summary", "Admin gave " + amount + " Shards")
                            .append("currency", new org.bson.Document("shards", amount).append("gems", 0))
                            .append("metadata", new org.bson.Document("action", "GIVE").append("type", "SHARDS")));
                } else {
                    economyManager.addGems(target.getUniqueId(), amount);
                    sender.sendMessage("§a§l[Kingdom] §7Added §b" + amount + " Gems §7to §f" + target.getDisplayName());
                    
                    // --- LOGGING ---
                    plugin.getMongoManager().logAction(new org.bson.Document()
                            .append("source", "GAME")
                            .append("type", "ECO_ADMIN")
                            .append("player", new org.bson.Document("uuid", (sender instanceof Player p ? p.getUniqueId().toString() : "CONSOLE")).append("name", sender.getName()))
                            .append("target", new org.bson.Document("uuid", target.getUniqueId().toString()).append("name", target.getName()))
                            .append("summary", "Admin gave " + amount + " Gems")
                            .append("currency", new org.bson.Document("shards", 0).append("gems", amount))
                            .append("metadata", new org.bson.Document("action", "GIVE").append("type", "GEMS")));
                }
            } else if (sub.equals("take")) {
                if (isShards) {
                    economyManager.removeShards(target.getUniqueId(), amount);
                    sender.sendMessage("§a§l[Kingdom] §7Removed §a" + amount + " Shards §7from §f" + target.getDisplayName());
                    
                    // --- LOGGING ---
                    plugin.getMongoManager().logAction(new org.bson.Document()
                            .append("source", "GAME")
                            .append("type", "ECO_ADMIN")
                            .append("player", new org.bson.Document("uuid", (sender instanceof Player p ? p.getUniqueId().toString() : "CONSOLE")).append("name", sender.getName()))
                            .append("target", new org.bson.Document("uuid", target.getUniqueId().toString()).append("name", target.getName()))
                            .append("summary", "Admin took " + amount + " Shards")
                            .append("currency", new org.bson.Document("shards", -amount).append("gems", 0))
                            .append("metadata", new org.bson.Document("action", "TAKE").append("type", "SHARDS")));
                } else {
                    economyManager.removeGems(target.getUniqueId(), amount);
                    sender.sendMessage("§a§l[Kingdom] §7Removed §b" + amount + " Gems §7from §f" + target.getDisplayName());
                    
                    // --- LOGGING ---
                    plugin.getMongoManager().logAction(new org.bson.Document()
                            .append("source", "GAME")
                            .append("type", "ECO_ADMIN")
                            .append("player", new org.bson.Document("uuid", (sender instanceof Player p ? p.getUniqueId().toString() : "CONSOLE")).append("name", sender.getName()))
                            .append("target", new org.bson.Document("uuid", target.getUniqueId().toString()).append("name", target.getName()))
                            .append("summary", "Admin took " + amount + " Gems")
                            .append("currency", new org.bson.Document("shards", 0).append("gems", -amount))
                            .append("metadata", new org.bson.Document("action", "TAKE").append("type", "GEMS")));
                }
            } else if (sub.equals("set")) {
                if (isShards) {
                    economyManager.setShards(target.getUniqueId(), amount);
                    sender.sendMessage("§a§l[Kingdom] §7Set §f" + target.getDisplayName() + "'s §7Shards to §a" + amount);
                    
                    // --- LOGGING ---
                    plugin.getMongoManager().logAction(new org.bson.Document()
                            .append("source", "GAME")
                            .append("type", "ECO_ADMIN")
                            .append("player", new org.bson.Document("uuid", (sender instanceof Player p ? p.getUniqueId().toString() : "CONSOLE")).append("name", sender.getName()))
                            .append("target", new org.bson.Document("uuid", target.getUniqueId().toString()).append("name", target.getName()))
                            .append("summary", "Admin set Shards to " + amount)
                            .append("currency", new org.bson.Document("shards", amount).append("gems", 0))
                            .append("metadata", new org.bson.Document("action", "SET").append("type", "SHARDS")));
                } else {
                    economyManager.setGems(target.getUniqueId(), amount);
                    sender.sendMessage("§a§l[Kingdom] §7Set §f" + target.getDisplayName() + "'s §7Gems to §b" + amount);
                    
                    // --- LOGGING ---
                    plugin.getMongoManager().logAction(new org.bson.Document()
                            .append("source", "GAME")
                            .append("type", "ECO_ADMIN")
                            .append("player", new org.bson.Document("uuid", (sender instanceof Player p ? p.getUniqueId().toString() : "CONSOLE")).append("name", sender.getName()))
                            .append("target", new org.bson.Document("uuid", target.getUniqueId().toString()).append("name", target.getName()))
                            .append("summary", "Admin set Gems to " + amount)
                            .append("currency", new org.bson.Document("shards", 0).append("gems", amount))
                            .append("metadata", new org.bson.Document("action", "SET").append("type", "GEMS")));
                }
            }
            return true;
        }

        if (sender instanceof Player) {
            sender.sendMessage("§cUnknown subcommand. Try /eco note <amount>");
        } else {
            sender.sendMessage("§cConsole must use subcommands: /eco <give|take|set> <player> <amount> [shards|gems]");
        }
        return true;
    }

    @EventHandler
    public void onNoteRedeem(org.bukkit.event.player.PlayerInteractEvent event) {
        if (!event.getAction().name().contains("RIGHT_CLICK")) return;
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null || item.getType() == org.bukkit.Material.AIR) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        
        org.bukkit.persistence.PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (pdc.has(NOTE_VALUE_KEY, org.bukkit.persistence.PersistentDataType.INTEGER)) {
            int amount = pdc.get(NOTE_VALUE_KEY, org.bukkit.persistence.PersistentDataType.INTEGER);
            
            // Consume item
            if (item.getAmount() > 1) item.setAmount(item.getAmount() - 1);
            else player.getInventory().setItemInMainHand(null);

            economyManager.addShards(player.getUniqueId(), amount);
            player.sendMessage("§a§l[Kingdom] §7Redeemed §a" + amount + " Shards §7from bank note!");
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
            
            // --- LOGGING ---
            plugin.getMongoManager().logAction(new org.bson.Document()
                    .append("source", "GAME")
                    .append("type", "ECO_DEPOSIT")
                    .append("player", new org.bson.Document("uuid", player.getUniqueId().toString()).append("name", player.getName()))
                    .append("summary", "Redeemed a bank note for " + amount + " Shards")
                    .append("currency", new org.bson.Document("shards", amount).append("gems", 0)));
        }
    }

    private int parseAmount(String input) {
        input = input.toLowerCase().trim();
        int multiplier = 1;

        if (input.endsWith("k")) {
            multiplier = 1000;
            input = input.substring(0, input.length() - 1);
        } else if (input.endsWith("m")) {
            multiplier = 1000000;
            input = input.substring(0, input.length() - 1);
        } else if (input.endsWith("b")) {
            multiplier = 1000000000;
            input = input.substring(0, input.length() - 1);
        }

        try {
            double base = Double.parseDouble(input);
            return (int) (base * multiplier);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.add("note");
            completions.add("withdraw");
            if (sender.hasPermission("kingdomcore.admin")) {
                completions.add("give");
                completions.add("take");
                completions.add("set");
            }
            return completions;
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("note") || sub.equals("withdraw")) {
                return List.of("10k", "100k", "1m");
            }
            if (sender.hasPermission("kingdomcore.admin") && (sub.equals("give") || sub.equals("take") || sub.equals("set"))) {
                return null; // returning null auto-completes online player names
            }
        } else if (args.length == 3) {
            String sub = args[0].toLowerCase();
            if (sender.hasPermission("kingdomcore.admin") && (sub.equals("give") || sub.equals("take") || sub.equals("set"))) {
                return List.of("100", "1k", "10k", "1m");
            }
        } else if (args.length == 4) {
            String sub = args[0].toLowerCase();
            if (sender.hasPermission("kingdomcore.admin") && (sub.equals("give") || sub.equals("take") || sub.equals("set"))) {
                return List.of("shards", "gems");
            }
        }
        return List.of();
    }
}
