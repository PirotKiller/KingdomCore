package me.pirot.kingdomCore.shop;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

/**
 * Custom InventoryHolder for the Main Shop Menu.
 */
public class MenuHolder implements InventoryHolder {
    @Override
    public @NotNull Inventory getInventory() {
        return null;
    }
}
