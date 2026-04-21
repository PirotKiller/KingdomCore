package me.pirot.kingdomCore.bounty;

import me.pirot.kingdomCore.KingdomCore;
import me.pirot.kingdomCore.config.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Handles the /bounty command and the associated GUI.
 */
public class BountyCommand implements CommandExecutor, TabCompleter, Listener {

    private final KingdomCore plugin;
    private final BountyManager bountyManager;
    private final ConfigManager configManager;

    private static final String MAIN_TITLE = "§8§l[ §4§lActive Bounties §8§l]";
    private static final String SUB_TITLE_PREFIX = "§8§l[ §cPlace Bounty §8§l] ";

    private final NamespacedKey TARGET_UUID_KEY;
    private final NamespacedKey ACTION_KEY;
    private final NamespacedKey AMOUNT_KEY;

    public BountyCommand(KingdomCore plugin, BountyManager bountyManager, ConfigManager configManager) {
        this.plugin = plugin;
        this.bountyManager = bountyManager;
        this.configManager = configManager;
        this.TARGET_UUID_KEY = new NamespacedKey(plugin, "bounty_target");
        this.ACTION_KEY = new NamespacedKey(plugin, "bounty_action");
        this.AMOUNT_KEY = new NamespacedKey(plugin, "bounty_amount");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return true;
        }

        // Still support text command
        if (args.length >= 2) {
            handleTextCommand(player, args);
            return true;
        }

        openMainBountyGUI(player);
        return true;
    }

    private void handleTextCommand(Player player, String[] args) {
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            player.sendMessage("§c§l[Kingdom] §7Player §f" + args[0] + " §7is not online!");
            return;
        }

        if (target.equals(player)) {
            player.sendMessage("§c§l[Kingdom] §7You can't place a bounty on yourself!");
            return;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage("§c§l[Kingdom] §7Invalid amount: §f" + args[1]);
            return;
        }

        int minBounty = configManager.getMinBounty();
        if (amount < minBounty) {
            player.sendMessage("§c§l[Kingdom] §7Minimum bounty is §a" + minBounty + " Shards§7!");
            return;
        }

        placeBounty(player, target, amount);
    }

    private void openMainBountyGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, MAIN_TITLE);

        fillBorder(inv, Material.RED_STAINED_GLASS_PANE, Material.BLACK_STAINED_GLASS_PANE);

        // Top Bounty Info
        Player topPlayer = bountyManager.getTopBountyPlayer();
        int topAmount = bountyManager.getTopBountyAmount();

        ItemStack info = new ItemStack(Material.WITHER_SKELETON_SKULL);
        ItemMeta infoMeta = info.getItemMeta();
        if (infoMeta != null) {
            infoMeta.setDisplayName("§4§l✦ Bounty Board ✦");
            List<String> lore = new ArrayList<>();
            lore.add("§8§m                              ");
            lore.add("");
            if (topPlayer != null && topAmount > 0) {
                lore.add("§7Most Wanted: §c" + topPlayer.getName());
                lore.add("§7Bounty: §6" + topAmount + " Shards");
            } else {
                lore.add("§7No active bounties.");
            }
            lore.add("");
            lore.add("§7Click a player below to");
            lore.add("§7place a new bounty.");
            lore.add("§8§m                              ");
            infoMeta.setLore(lore);
            info.setItemMeta(infoMeta);
        }
        inv.setItem(4, info);

        // List online players
        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        players.remove(player); // except self

        int slot = 10;
        for (Player p : players) {
            if (slot >= 53) break;
            while (isBorderSlot(slot, 54) && slot < 54) slot++;
            if (slot >= 53) break;

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            if (meta != null) {
                meta.setOwningPlayer(p);
                meta.setDisplayName("§f" + p.getName());

                int currentBounty = bountyManager.getBounty(p.getUniqueId());
                List<String> lore = new ArrayList<>();
                lore.add("§8§m                              ");
                lore.add("§7Current Bounty: §6" + currentBounty + " Shards");
                lore.add("");
                lore.add("§e▸ Click to add bounty");
                meta.setLore(lore);

                meta.getPersistentDataContainer().set(TARGET_UUID_KEY, PersistentDataType.STRING, p.getUniqueId().toString());
                meta.getPersistentDataContainer().set(ACTION_KEY, PersistentDataType.STRING, "select_target");

                head.setItemMeta(meta);
            }
            inv.setItem(slot, head);
            slot++;
        }

        if (players.isEmpty()) {
            ItemStack empty = new ItemStack(Material.BARRIER);
            ItemMeta emptyMeta = empty.getItemMeta();
            if (emptyMeta != null) {
                emptyMeta.setDisplayName("§c§lNo Targets");
                emptyMeta.setLore(Arrays.asList("§7There is nobody else online."));
                empty.setItemMeta(emptyMeta);
            }
            inv.setItem(22, empty);
        }

        player.openInventory(inv);
    }

    private void openAmountGUI(Player player, Player target) {
        Inventory inv = Bukkit.createInventory(null, 27, SUB_TITLE_PREFIX + target.getName());

        fillBorder(inv, Material.ORANGE_STAINED_GLASS_PANE, Material.BLACK_STAINED_GLASS_PANE);

        int minBounty = configManager.getMinBounty();
        int currentBounty = bountyManager.getBounty(target.getUniqueId());

        // Target Info
        ItemStack info = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta infoMeta = (SkullMeta) info.getItemMeta();
        if (infoMeta != null) {
            infoMeta.setOwningPlayer(target);
            infoMeta.setDisplayName("§c§lTarget: §f" + target.getName());
            infoMeta.setLore(Arrays.asList(
                    "§7Current Bounty: §6" + currentBounty + " Shards",
                    "§7Minimum Addition: §6" + minBounty + " Shards"
            ));
            info.setItemMeta(infoMeta);
        }
        inv.setItem(4, info);

        // Predefined Amounts
        inv.setItem(10, createAmountItem(target.getUniqueId(), 50, Material.IRON_NUGGET, "§f§l+50 Shards"));
        inv.setItem(12, createAmountItem(target.getUniqueId(), 100, Material.GOLD_NUGGET, "§e§l+100 Shards"));
        inv.setItem(14, createAmountItem(target.getUniqueId(), 500, Material.DIAMOND, "§b§l+500 Shards"));
        inv.setItem(16, createAmountItem(target.getUniqueId(), 1000, Material.NETHER_STAR, "§d§l+1000 Shards"));

        // Back Button
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        if (backMeta != null) {
            backMeta.setDisplayName("§c§lBack");
            backMeta.getPersistentDataContainer().set(ACTION_KEY, PersistentDataType.STRING, "back");
            back.setItemMeta(backMeta);
        }
        inv.setItem(22, back);

        player.openInventory(inv);
    }

    private ItemStack createAmountItem(UUID targetUuid, int amount, Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(Arrays.asList("§e▸ Click to add §6" + amount + " Shards"));
            meta.getPersistentDataContainer().set(TARGET_UUID_KEY, PersistentDataType.STRING, targetUuid.toString());
            meta.getPersistentDataContainer().set(ACTION_KEY, PersistentDataType.STRING, "add_bounty");
            meta.getPersistentDataContainer().set(AMOUNT_KEY, PersistentDataType.INTEGER, amount);
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void placeBounty(Player source, Player target, int amount) {
        int minBounty = configManager.getMinBounty();
        if (amount < minBounty) {
            source.sendMessage("§c§l[Kingdom] §7Minimum bounty is §a" + minBounty + " Shards§7!");
            return;
        }

        if (bountyManager.placeBounty(source, target, amount)) {
            int totalBounty = bountyManager.getBounty(target.getUniqueId());
            Bukkit.broadcastMessage("§6§l[Bounty] §f" + source.getName() + " §7added §a" +
                    amount + " Shards §7to the bounty on §f" + target.getName() + "§7!");

            Bukkit.broadcastMessage("§6§l[Bounty] §f" + target.getName() + "§7's new total bounty: §6" + totalBounty + " Shards");

            // --- LOGGING ---
            org.bson.Document log = new org.bson.Document("timestamp", new java.util.Date())
                    .append("source", "GAME")
                    .append("type", "BOUNTY_SET")
                    .append("player", new org.bson.Document("uuid", source.getUniqueId().toString())
                            .append("name", source.getName()))
                    .append("target", new org.bson.Document("uuid", target.getUniqueId().toString())
                            .append("name", target.getName()))
                    .append("summary", source.getName() + " placed a bounty of " + amount + " Shards on " + target.getName())
                    .append("currency", new org.bson.Document("shards", -amount))
                    .append("metadata", new org.bson.Document("amount", amount)
                            .append("newTotal", totalBounty));
            plugin.getMongoManager().logAction(log);
        } else {
            source.sendMessage("§c§l[Kingdom] §7Not enough Shards!");
        }
    }

    @EventHandler
    public void onBountyClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String title = event.getView().getTitle();
        if (!title.equals(MAIN_TITLE) && !title.startsWith(SUB_TITLE_PREFIX)) return;

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) return;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (!pdc.has(ACTION_KEY, PersistentDataType.STRING)) return;

        String action = pdc.get(ACTION_KEY, PersistentDataType.STRING);
        if (action == null) return;

        switch (action) {
            case "back":
                openMainBountyGUI(player);
                break;
            case "select_target":
                String uuidStr = pdc.get(TARGET_UUID_KEY, PersistentDataType.STRING);
                if (uuidStr != null) {
                    Player target = Bukkit.getPlayer(UUID.fromString(uuidStr));
                    if (target != null && target.isOnline()) {
                        openAmountGUI(player, target);
                    } else {
                        player.sendMessage("§c§l[Kingdom] §7Player is no longer online.");
                        openMainBountyGUI(player);
                    }
                }
                break;
            case "add_bounty":
                String targetStr = pdc.get(TARGET_UUID_KEY, PersistentDataType.STRING);
                Integer amount = pdc.get(AMOUNT_KEY, PersistentDataType.INTEGER);
                if (targetStr != null && amount != null) {
                    Player target = Bukkit.getPlayer(UUID.fromString(targetStr));
                    if (target != null && target.isOnline()) {
                        placeBounty(player, target, amount);
                        player.closeInventory();
                    } else {
                        player.sendMessage("§c§l[Kingdom] §7Player is no longer online.");
                        openMainBountyGUI(player);
                    }
                }
                break;
        }
    }

    private void fillBorder(Inventory inv, Material borderMat, Material cornerMat) {
        int size = inv.getSize();
        ItemStack border = new ItemStack(borderMat);
        ItemMeta bm = border.getItemMeta();
        if (bm != null) { bm.setDisplayName(" "); border.setItemMeta(bm); }

        ItemStack corner = new ItemStack(cornerMat);
        ItemMeta cm = corner.getItemMeta();
        if (cm != null) { cm.setDisplayName(" "); corner.setItemMeta(cm); }

        for (int i = 0; i < size; i++) {
            if (isBorderSlot(i, size)) {
                if (isCornerSlot(i, size)) {
                    inv.setItem(i, corner);
                } else {
                    inv.setItem(i, border);
                }
            }
        }
    }

    private boolean isBorderSlot(int slot, int size) {
        int row = slot / 9;
        int col = slot % 9;
        int maxRow = (size / 9) - 1;
        return row == 0 || row == maxRow || col == 0 || col == 8;
    }

    private boolean isCornerSlot(int slot, int size) {
        int maxRow = (size / 9) - 1;
        return (slot == 0 || slot == 8 || slot == maxRow * 9 || slot == maxRow * 9 + 8);
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 2) {
            return List.of("50", "100", "250", "500", "1000");
        }
        return new ArrayList<>();
    }
}
