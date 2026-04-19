package me.pirot.kingdomCore.rpg;

import me.pirot.kingdomCore.KingdomCore;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * GUI for selecting an RPG class.
 */
public class ClassSelectorGUI implements Listener {

    private final KingdomCore plugin;
    private final ClassManager classManager;
    private final NamespacedKey CLASS_KEY;

    public static final String TITLE = "§8§l[ §5§lSelect Your Class §8§l]";

    public ClassSelectorGUI(KingdomCore plugin, ClassManager classManager) {
        this.plugin = plugin;
        this.classManager = classManager;
        this.CLASS_KEY = new NamespacedKey(plugin, "selected_class");
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE);

        fillBorder(inv);

        // Knight Icon
        inv.setItem(11, createClassIcon(Material.IRON_CHESTPLATE, "§f§lKnight", RPGClass.KNIGHT,
                "§7A heavy frontline tank.",
                "§8 ▸ §eHigh defense",
                "§8 ▸ §eSlow movement",
                "§8 ▸ §eBlast resistance"));

        // Wizard Icon
        inv.setItem(12, createClassIcon(Material.ENCHANTED_BOOK, "§d§lWizard", RPGClass.WIZARD,
                "§7A powerful spellcaster.",
                "§8 ▸ §eHigh damage",
                "§8 ▸ §eLow defense",
                "§8 ▸ §eMana specialized"));

        // Archer Icon
        inv.setItem(13, createClassIcon(Material.BOW, "§a§lArcher", RPGClass.ARCHER,
                "§7A long-range assassin.",
                "§8 ▸ §eFast movement",
                "§8 ▸ §eRanged master",
                "§8 ▸ §eFrail in melee"));

        // Ronin Icon
        inv.setItem(14, createClassIcon(Material.NETHERITE_SWORD, "§6§lRonin", RPGClass.RONIN,
                "§7A swift masterless warrior.",
                "§8 ▸ §eVery high mobility",
                "§8 ▸ §eConsistent damage",
                "§8 ▸ §eVersatile playstyle"));

        // Rogue Icon
        inv.setItem(15, createClassIcon(Material.IRON_SWORD, "§7§lRogue", RPGClass.ROGUE,
                "§7A stealthy thief.",
                "§8 ▸ §eInvisibility skills",
                "§8 ▸ §eBackstab bonus",
                "§8 ▸ §eFast movement"));

        player.openInventory(inv);
        player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1f, 1.2f);
    }

    private ItemStack createClassIcon(Material mat, String name, RPGClass rpgClass, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            List<String> fullLore = new ArrayList<>();
            fullLore.add("§8§m                              ");
            fullLore.add("");
            fullLore.addAll(Arrays.asList(lore));
            fullLore.add("");
            fullLore.add("§a▸ Click to select");
            fullLore.add("§8§m                              ");
            meta.setLore(fullLore);
            meta.getPersistentDataContainer().set(CLASS_KEY, PersistentDataType.STRING, rpgClass.name());
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void fillBorder(Inventory inv) {
        ItemStack pane = new ItemStack(Material.PURPLE_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) { meta.setDisplayName(" "); pane.setItemMeta(meta); }
        for (int i = 0; i < 27; i++) {
            if (i < 9 || i >= 18 || i % 9 == 0 || i % 9 == 8) {
                inv.setItem(i, pane);
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(TITLE)) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) return;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (pdc.has(CLASS_KEY, PersistentDataType.STRING)) {
            String className = pdc.get(CLASS_KEY, PersistentDataType.STRING);
            RPGClass rpgClass = RPGClass.fromString(className);
            if (rpgClass != null) {
                classManager.setClass(player, rpgClass);
                player.closeInventory();
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
            }
        }
    }
}
