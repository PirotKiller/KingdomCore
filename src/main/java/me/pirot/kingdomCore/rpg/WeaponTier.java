package me.pirot.kingdomCore.rpg;

/**
 * Weapon tiers from lowest to highest, with CMD sub-offsets.
 * Gold = +1, Diamond = +2, Nether = +3, Bone = +4, Chaos = +5
 */
public enum WeaponTier {

    GOLD("Gold", "§6", 1),
    DIAMOND("Diamond", "§b", 2),
    NETHER("Nether", "§4", 3),
    BONE("Bone", "§f", 4),
    CHAOS("Chaos", "§4§l", 5);

    private final String displayName;
    private final String color;
    private final int cmdSubOffset;

    WeaponTier(String displayName, String color, int cmdSubOffset) {
        this.displayName = displayName;
        this.color = color;
        this.cmdSubOffset = cmdSubOffset;
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

    /**
     * The full Custom Model Data value = RPGClass.cmdOffset + this.cmdSubOffset.
     */
    public int getCmdSubOffset() {
        return cmdSubOffset;
    }

    public static WeaponTier fromString(String name) {
        try {
            return WeaponTier.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
