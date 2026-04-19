package me.pirot.kingdomCore.rpg;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * Defines crafting recipes for all class weapons across all tiers.
 * Each recipe specifies the required materials and the minimum level to craft.
 */
public class WeaponRecipe {

    private final RPGClass rpgClass;
    private final WeaponTier tier;
    private final String weaponName;
    private final int requiredLevel;
    private final Map<Material, Integer> ingredients;
    private final int soulItemsRequired;

    public WeaponRecipe(RPGClass rpgClass, WeaponTier tier, String weaponName,
                        int requiredLevel, Map<Material, Integer> ingredients, int soulItemsRequired) {
        this.rpgClass = rpgClass;
        this.tier = tier;
        this.weaponName = weaponName;
        this.requiredLevel = requiredLevel;
        this.ingredients = ingredients;
        this.soulItemsRequired = soulItemsRequired;
    }

    public RPGClass getRpgClass() { return rpgClass; }
    public WeaponTier getTier() { return tier; }
    public String getWeaponName() { return weaponName; }
    public int getRequiredLevel() { return requiredLevel; }
    public Map<Material, Integer> getIngredients() { return ingredients; }
    public int getSoulItemsRequired() { return soulItemsRequired; }

    /**
     * Get all registered weapon recipes.
     */
    public static List<WeaponRecipe> getAllRecipes() {
        List<WeaponRecipe> recipes = new ArrayList<>();

        // ========== ARCHER (Bows) ==========
        recipes.add(new WeaponRecipe(RPGClass.ARCHER, WeaponTier.GOLD, "§6Gold Bow", 1,
                Map.of(Material.GOLD_INGOT, 8, Material.STRING, 3), 1));
        recipes.add(new WeaponRecipe(RPGClass.ARCHER, WeaponTier.DIAMOND, "§bDiamond Bow", 3,
                Map.of(Material.DIAMOND, 5, Material.STRING, 3, Material.GOLD_INGOT, 4), 1));
        recipes.add(new WeaponRecipe(RPGClass.ARCHER, WeaponTier.NETHER, "§4Nether Bow", 5,
                Map.of(Material.NETHERITE_SCRAP, 3, Material.BLAZE_ROD, 4, Material.STRING, 3), 2));
        recipes.add(new WeaponRecipe(RPGClass.ARCHER, WeaponTier.BONE, "§fBone Bow", 7,
                Map.of(Material.BONE_BLOCK, 8, Material.PHANTOM_MEMBRANE, 4, Material.STRING, 3), 2));
        recipes.add(new WeaponRecipe(RPGClass.ARCHER, WeaponTier.CHAOS, "§4§lChaos Bow", 10,
                Map.of(Material.NETHERITE_INGOT, 4, Material.NETHER_STAR, 1, Material.DRAGON_BREATH, 8), 3));

        // ========== KNIGHT (Shields) ==========
        recipes.add(new WeaponRecipe(RPGClass.KNIGHT, WeaponTier.GOLD, "§6Gold Shield", 1,
                Map.of(Material.GOLD_INGOT, 8, Material.IRON_INGOT, 4), 1));
        recipes.add(new WeaponRecipe(RPGClass.KNIGHT, WeaponTier.DIAMOND, "§bDiamond Shield", 3,
                Map.of(Material.DIAMOND, 5, Material.IRON_BLOCK, 2), 1));
        recipes.add(new WeaponRecipe(RPGClass.KNIGHT, WeaponTier.NETHER, "§4Nether Shield", 5,
                Map.of(Material.NETHERITE_SCRAP, 3, Material.OBSIDIAN, 8), 2));
        recipes.add(new WeaponRecipe(RPGClass.KNIGHT, WeaponTier.BONE, "§fBone Shield", 7,
                Map.of(Material.BONE_BLOCK, 8, Material.TURTLE_SCUTE, 5), 2));
        recipes.add(new WeaponRecipe(RPGClass.KNIGHT, WeaponTier.CHAOS, "§4§lChaos Shield", 10,
                Map.of(Material.NETHERITE_INGOT, 4, Material.TOTEM_OF_UNDYING, 1), 3));

        // ========== WIZARD (Staffs) ==========
        recipes.add(new WeaponRecipe(RPGClass.WIZARD, WeaponTier.GOLD, "§6Gold Staff", 1,
                Map.of(Material.GOLD_INGOT, 6, Material.BLAZE_ROD, 2), 1));
        recipes.add(new WeaponRecipe(RPGClass.WIZARD, WeaponTier.DIAMOND, "§bDiamond Staff", 3,
                Map.of(Material.DIAMOND, 4, Material.BLAZE_ROD, 2, Material.AMETHYST_SHARD, 8), 1));
        recipes.add(new WeaponRecipe(RPGClass.WIZARD, WeaponTier.NETHER, "§4Nether Staff", 5,
                Map.of(Material.NETHERITE_SCRAP, 3, Material.BLAZE_ROD, 4, Material.ENDER_PEARL, 4), 2));
        recipes.add(new WeaponRecipe(RPGClass.WIZARD, WeaponTier.BONE, "§fBone Staff", 7,
                Map.of(Material.BONE_BLOCK, 8, Material.BREEZE_ROD, 4), 2));
        recipes.add(new WeaponRecipe(RPGClass.WIZARD, WeaponTier.CHAOS, "§4§lChaos Staff", 10,
                Map.of(Material.NETHERITE_INGOT, 4, Material.END_CRYSTAL, 2, Material.DRAGON_BREATH, 8), 3));

        // ========== ROGUE (Daggers) ==========
        recipes.add(new WeaponRecipe(RPGClass.ROGUE, WeaponTier.GOLD, "§6Gold Dagger", 1,
                Map.of(Material.GOLD_INGOT, 6, Material.FLINT, 4), 1));
        recipes.add(new WeaponRecipe(RPGClass.ROGUE, WeaponTier.DIAMOND, "§bDiamond Dagger", 3,
                Map.of(Material.DIAMOND, 4, Material.PRISMARINE_SHARD, 8), 1));
        recipes.add(new WeaponRecipe(RPGClass.ROGUE, WeaponTier.NETHER, "§4Nether Dagger", 5,
                Map.of(Material.NETHERITE_SCRAP, 3, Material.SPIDER_EYE, 8), 2));
        recipes.add(new WeaponRecipe(RPGClass.ROGUE, WeaponTier.BONE, "§fBone Dagger", 7,
                Map.of(Material.BONE_BLOCK, 8, Material.PHANTOM_MEMBRANE, 4), 2));
        recipes.add(new WeaponRecipe(RPGClass.ROGUE, WeaponTier.CHAOS, "§4§lChaos Dagger", 10,
                Map.of(Material.NETHERITE_INGOT, 4, Material.FERMENTED_SPIDER_EYE, 8), 3));

        // ========== RONIN (Katanas) ==========
        recipes.add(new WeaponRecipe(RPGClass.RONIN, WeaponTier.GOLD, "§6Gold Katana", 1,
                Map.of(Material.GOLD_INGOT, 8, Material.BAMBOO, 4), 1));
        recipes.add(new WeaponRecipe(RPGClass.RONIN, WeaponTier.DIAMOND, "§bDiamond Katana", 3,
                Map.of(Material.DIAMOND, 5, Material.BAMBOO, 4, Material.IRON_INGOT, 4), 1));
        recipes.add(new WeaponRecipe(RPGClass.RONIN, WeaponTier.NETHER, "§4Nether Katana", 5,
                Map.of(Material.NETHERITE_SCRAP, 3, Material.BAMBOO, 4, Material.MAGMA_CREAM, 4), 2));
        recipes.add(new WeaponRecipe(RPGClass.RONIN, WeaponTier.BONE, "§fBone Katana", 7,
                Map.of(Material.BONE_BLOCK, 8, Material.WITHER_SKELETON_SKULL, 1), 2));
        recipes.add(new WeaponRecipe(RPGClass.RONIN, WeaponTier.CHAOS, "§4§lChaos Katana", 10,
                Map.of(Material.NETHERITE_INGOT, 4, Material.WITHER_SKELETON_SKULL, 2, Material.DRAGON_BREATH, 4), 3));

        return recipes;
    }

    /**
     * Get recipes for a specific class.
     */
    public static List<WeaponRecipe> getRecipesForClass(RPGClass rpgClass) {
        List<WeaponRecipe> result = new ArrayList<>();
        for (WeaponRecipe recipe : getAllRecipes()) {
            if (recipe.getRpgClass() == rpgClass) result.add(recipe);
        }
        return result;
    }
}
