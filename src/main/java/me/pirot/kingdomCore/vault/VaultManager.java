package me.pirot.kingdomCore.vault;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import me.pirot.kingdomCore.KingdomCore;
import me.pirot.kingdomCore.database.MongoManager;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.*;

/**
 * Per-player Vault inventory stored in MongoDB.
 * Items in the vault survive server resets.
 * Players can freely deposit and withdraw items.
 *
 * Layout (54-slot double chest):
 *   Row 0 (0-8): Border + Info item at slot 4
 *   Rows 1-4 (9-44): Border on cols 0,8 — 28 usable slots
 *   Row 5 (45-53): Border
 */
public class VaultManager implements CommandExecutor, Listener {

    private final KingdomCore plugin;
    private final MongoManager mongoManager;

    private static final String VAULT_TITLE = "§8§l[ §e§lVault §8§l]";
    private static final int INFO_SLOT = 4;

    public VaultManager(KingdomCore plugin, MongoManager mongoManager) {
        this.plugin = plugin;
        this.mongoManager = mongoManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return true;
        }
        openVault(player);
        return true;
    }

    /**
     * Open the vault GUI for a player, loading their saved items from MongoDB.
     */
    public void openVault(Player player) {
        Inventory inv = Bukkit.createInventory(new VaultHolder(), 54, VAULT_TITLE);

        // Fill border
        fillBorder(inv);

        // Add info item
        ItemStack info = new ItemStack(Material.ENDER_CHEST);
        ItemMeta infoMeta = info.getItemMeta();
        if (infoMeta != null) {
            infoMeta.setDisplayName("§e§l✦ Player Vault ✦");
            infoMeta.setLore(Arrays.asList(
                    "§8§m                              ",
                    "",
                    "§7Store valuable items here.",
                    "§7Items in the vault §asurvive",
                    "§7server resets§7!",
                    "",
                    "§7Place items in the empty slots",
                    "§7to save them. Take them out",
                    "§7whenever you need them.",
                    "",
                    "§8§m                              ",
                    "§a✦ Items auto-save on close"
            ));
            infoMeta.addEnchant(Enchantment.UNBREAKING, 1, true);
            infoMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
            info.setItemMeta(infoMeta);
        }
        inv.setItem(INFO_SLOT, info);

        // Load saved items from MongoDB asynchronously, then populate on main thread
        UUID uuid = player.getUniqueId();
        mongoManager.getExecutor().submit(() -> {
            Map<Integer, ItemStack> savedItems = loadVaultItems(uuid);
            Bukkit.getScheduler().runTask(plugin, () -> {
                for (Map.Entry<Integer, ItemStack> entry : savedItems.entrySet()) {
                    int slot = entry.getKey();
                    if (slot >= 0 && slot < 54 && !isBorderSlot(slot) && slot != INFO_SLOT) {
                        inv.setItem(slot, entry.getValue());
                    }
                }
                player.openInventory(inv);
                player.playSound(player.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, 1f, 1f);
            });
        });
    }

    @EventHandler
    public void onVaultClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (!(event.getInventory().getHolder() instanceof VaultHolder)) return;

        int slot = event.getRawSlot();

        // Block clicks on border and info slots in the top inventory
        if (slot >= 0 && slot < 54) {
            if (isBorderSlot(slot) || slot == INFO_SLOT) {
                event.setCancelled(true);
            }
            // Allow interaction with grid slots (put in / take out)
        }
        // Allow clicks in player inventory
    }

    @EventHandler
    public void onVaultClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!(event.getInventory().getHolder() instanceof VaultHolder)) return;

        // Save all items in the vault to MongoDB
        Inventory inv = event.getInventory();
        Map<Integer, ItemStack> items = new LinkedHashMap<>();

        for (int slot = 0; slot < 54; slot++) {
            if (isBorderSlot(slot) || slot == INFO_SLOT) continue;

            ItemStack item = inv.getItem(slot);
            if (item != null && item.getType() != Material.AIR) {
                items.put(slot, item);
            }
        }

        UUID uuid = player.getUniqueId();
        mongoManager.getExecutor().submit(() -> saveVaultItems(uuid, items));
        player.playSound(player.getLocation(), Sound.BLOCK_ENDER_CHEST_CLOSE, 1f, 1f);
    }

    // ============================================================
    // MONGODB SERIALIZATION
    // ============================================================

    private void saveVaultItems(UUID uuid, Map<Integer, ItemStack> items) {
        MongoCollection<Document> col = mongoManager.getDatabase().getCollection("vaults");
        if (col == null) return;

        List<Document> itemDocs = new ArrayList<>();
        for (Map.Entry<Integer, ItemStack> entry : items.entrySet()) {
            String base64 = itemToBase64(entry.getValue());
            if (base64 != null) {
                itemDocs.add(new Document()
                        .append("slot", entry.getKey())
                        .append("data", base64));
            }
        }

        Document doc = new Document()
                .append("uuid", uuid.toString())
                .append("items", itemDocs)
                .append("lastUpdated", new java.util.Date());

        col.replaceOne(
                Filters.eq("uuid", uuid.toString()),
                doc,
                new ReplaceOptions().upsert(true)
        );
    }

    private Map<Integer, ItemStack> loadVaultItems(UUID uuid) {
        Map<Integer, ItemStack> items = new LinkedHashMap<>();
        MongoCollection<Document> col = mongoManager.getDatabase().getCollection("vaults");
        if (col == null) return items;

        Document doc = col.find(Filters.eq("uuid", uuid.toString())).first();
        if (doc == null) return items;

        List<Document> itemDocs = doc.getList("items", Document.class);
        if (itemDocs == null) return items;

        for (Document itemDoc : itemDocs) {
            int slot = itemDoc.getInteger("slot", -1);
            String base64 = itemDoc.getString("data");
            if (slot >= 0 && base64 != null) {
                ItemStack stack = itemFromBase64(base64);
                if (stack != null) {
                    items.put(slot, stack);
                }
            }
        }
        return items;
    }

    // ============================================================
    // BASE64 ITEM SERIALIZATION
    // ============================================================

    private String itemToBase64(ItemStack item) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
            dataOutput.writeObject(item);
            dataOutput.close();
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (Exception e) {
            plugin.getLogger().warning("[Vault] Failed to serialize item: " + e.getMessage());
            return null;
        }
    }

    private ItemStack itemFromBase64(String base64) {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(base64));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            ItemStack item = (ItemStack) dataInput.readObject();
            dataInput.close();
            return item;
        } catch (Exception e) {
            plugin.getLogger().warning("[Vault] Failed to deserialize item: " + e.getMessage());
            return null;
        }
    }

    // ============================================================
    // BORDER UTILITIES
    // ============================================================

    private void fillBorder(Inventory inv) {
        ItemStack border = createPane(Material.YELLOW_STAINED_GLASS_PANE);
        ItemStack corner = createPane(Material.ORANGE_STAINED_GLASS_PANE);
        ItemStack accent = createPane(Material.BLACK_STAINED_GLASS_PANE);

        for (int i = 0; i < 54; i++) {
            if (isBorderSlot(i)) {
                if (isCornerSlot(i)) {
                    inv.setItem(i, corner);
                } else if (i == 3 || i == 5) {
                    inv.setItem(i, accent);
                } else {
                    inv.setItem(i, border);
                }
            }
        }
    }

    private boolean isBorderSlot(int slot) {
        int row = slot / 9;
        int col = slot % 9;
        return row == 0 || row == 5 || col == 0 || col == 8;
    }

    private boolean isCornerSlot(int slot) {
        return slot == 0 || slot == 8 || slot == 45 || slot == 53;
    }

    private ItemStack createPane(Material material) {
        ItemStack pane = new ItemStack(material);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            pane.setItemMeta(meta);
        }
        return pane;
    }
}
