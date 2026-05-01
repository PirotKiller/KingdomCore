package me.pirot.kingdomCore.shop;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

/**
 * Custom InventoryHolder to reliably identify Shop GUIs.
 */
public class ShopHolder implements InventoryHolder {
    private final ShopType shopType;
    private final int page;

    public ShopHolder(ShopType shopType, int page) {
        this.shopType = shopType;
        this.page = page;
    }

    public ShopType getShopType() {
        return shopType;
    }

    public int getPage() {
        return page;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return null; // The inventory is built by ShopGUI
    }
}
