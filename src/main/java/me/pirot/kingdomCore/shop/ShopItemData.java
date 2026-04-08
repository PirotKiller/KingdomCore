package me.pirot.kingdomCore.shop;

import java.util.List;

/**
 * POJO representing a single shop item loaded from MongoDB.
 * Replaces the previous YAML ConfigurationSection approach.
 */
public class ShopItemData {

    private final String itemKey;
    private final String name;
    private final String material;
    private final int amount;
    private final List<String> lore;
    private final int priceShards;
    private final int priceGems;
    private final String enchant;
    private final int enchantLevel;
    private final double damage;
    private final double speed;
    private final String rpgClass;
    private final String tier;
    private final int cmd;
    private final int order;
    private final boolean active;

    public ShopItemData(String itemKey, String name, String material, int amount,
                        List<String> lore, int priceShards, int priceGems,
                        String enchant, int enchantLevel, double damage, double speed,
                        String rpgClass, String tier, int cmd, int order, boolean active) {
        this.itemKey = itemKey;
        this.name = name;
        this.material = material;
        this.amount = amount;
        this.lore = lore;
        this.priceShards = priceShards;
        this.priceGems = priceGems;
        this.enchant = enchant;
        this.enchantLevel = enchantLevel;
        this.damage = damage;
        this.speed = speed;
        this.rpgClass = rpgClass;
        this.tier = tier;
        this.cmd = cmd;
        this.order = order;
        this.active = active;
    }

    public String getItemKey() { return itemKey; }
    public String getName() { return name; }
    public String getMaterial() { return material; }
    public int getAmount() { return amount; }
    public List<String> getLore() { return lore; }
    public int getPriceShards() { return priceShards; }
    public int getPriceGems() { return priceGems; }
    public String getEnchant() { return enchant; }
    public int getEnchantLevel() { return enchantLevel; }
    public double getDamage() { return damage; }
    public double getSpeed() { return speed; }
    public String getRpgClass() { return rpgClass; }
    public String getTier() { return tier; }
    public int getCmd() { return cmd; }
    public int getOrder() { return order; }
    public boolean isActive() { return active; }

    /**
     * Check if this item is a weapon (has damage + class + tier).
     */
    public boolean isWeapon() {
        return damage > 0 && rpgClass != null && tier != null;
    }

    /**
     * Check if this item is an enchanted book.
     */
    public boolean isEnchantedBook() {
        return enchant != null && !enchant.isEmpty() && "ENCHANTED_BOOK".equals(material);
    }
}
