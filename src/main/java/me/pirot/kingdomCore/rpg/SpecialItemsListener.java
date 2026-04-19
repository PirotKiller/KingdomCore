package me.pirot.kingdomCore.rpg;

import me.pirot.kingdomCore.KingdomCore;
import me.pirot.kingdomCore.database.PlayerData;
import me.pirot.kingdomCore.economy.EconomyManager;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Handles the logic for using special RPG items (Scrolls, Tomes, etc.)
 */
public class SpecialItemsListener implements Listener {

    private final KingdomCore plugin;
    private final SpecialItems specialItems;
    private final ClassSelectorGUI classSelectorGUI;
    private final EconomyManager economyManager;

    public SpecialItemsListener(KingdomCore plugin, SpecialItems specialItems, 
                                ClassSelectorGUI classSelectorGUI, EconomyManager economyManager) {
        this.plugin = plugin;
        this.specialItems = specialItems;
        this.classSelectorGUI = classSelectorGUI;
        this.economyManager = economyManager;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        
        ItemStack item = event.getItem();
        if (item == null || item.getType() == Material.AIR) return;
        
        if (!specialItems.isSpecialItem(item)) return;
        
        Player player = event.getPlayer();
        String id = specialItems.getSpecialItemId(item);
        
        if (id == null) return;
        
        switch (id) {
            case SpecialItems.CLASS_CHANGE_ID -> {
                event.setCancelled(true);
                classSelectorGUI.open(player);
                consumeItem(player, item);
                player.sendMessage("§a§l[Kingdom] §7You used a §6Class Change Scroll§7!");
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1f, 1.5f);
            }
            case SpecialItems.DATA_RESTORE_ID -> {
                event.setCancelled(true);
                PlayerData data = economyManager.getPlayerData(player.getUniqueId());
                if (data == null) return;
                
                String currentClass = data.getClassName();
                if (currentClass == null || currentClass.equals("NONE")) {
                    player.sendMessage("§c§l[Kingdom] §7You must select a class first!");
                    return;
                }
                
                if (data.restoreClassProgress(currentClass)) {
                    consumeItem(player, item);
                    player.sendMessage("§a§l[Kingdom] §7Successfully restored your §e" + 
                            RPGClass.fromString(currentClass).getColoredName() + " §7progress!");
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                    
                    // Force a database save to ensure persistence
                    economyManager.savePlayer(player.getUniqueId());
                } else {
                    player.sendMessage("§c§l[Kingdom] §7No saved progress found for your current class.");
                }
            }
        }
    }

    /**
     * Removes one item from the player's hand.
     */
    private void consumeItem(Player player, ItemStack item) {
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            player.getInventory().remove(item);
        }
    }
}
