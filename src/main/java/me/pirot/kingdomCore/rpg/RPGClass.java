package me.pirot.kingdomCore.rpg;

import org.bukkit.Material;

/**
 * All available RPG classes with their weapon base material and CMD offset.
 */
public enum RPGClass {

    ARCHER("Archer", "§6", Material.BOW, 1000),
    KNIGHT("Knight", "§b", Material.SHIELD, 2000),
    WIZARD("Wizard", "§5", Material.STICK, 3000),
    ROGUE("Rogue", "§c", Material.IRON_SWORD, 4000),
    RONIN("Ronin", "§e", Material.WOODEN_SWORD, 5000);

    private final String displayName;
    private final String color;
    private final Material weaponMaterial;
    private final int cmdOffset;

    RPGClass(String displayName, String color, Material weaponMaterial, int cmdOffset) {
        this.displayName = displayName;
        this.color = color;
        this.weaponMaterial = weaponMaterial;
        this.cmdOffset = cmdOffset;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getColoredName() {
        return color + displayName;
    }

    public String getColor() {
        return color;
    }

    public Material getWeaponMaterial() {
        return weaponMaterial;
    }

    public int getCmdOffset() {
        return cmdOffset;
    }

    /**
     * Parse from string, returns null if not found.
     */
    public static RPGClass fromString(String name) {
        if (name == null || name.equalsIgnoreCase("NONE")) return null;
        try {
            return RPGClass.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
