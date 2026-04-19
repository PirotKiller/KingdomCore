package me.pirot.kingdomCore.rpg;

import me.pirot.kingdomCore.KingdomCore;
import me.pirot.kingdomCore.database.PlayerData;
import me.pirot.kingdomCore.economy.EconomyManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

/**
 * Custom crafting interface for class weapons.
 * Opens a chest GUI showing available recipes based on the player's class and level.
 */
public class CraftingGUI implements Listener {

    private final KingdomCore plugin;
    private final WeaponManager weaponManager;
    private final EconomyManager economyManager;
    private final SpecialItems specialItems;

    public static final String CRAFTING_TITLE = "§8§l[ §d§lSoul Forge §8§l]";

    public CraftingGUI(KingdomCore plugin, WeaponManager weaponManager,
                       EconomyManager economyManager, SpecialItems specialItems) {
        this.plugin = plugin;
        this.weaponManager = weaponManager;
        this.economyManager = economyManager;
        this.specialItems = specialItems;
    }

    /**
     * Open the crafting GUI for a player showing their class weapons.
     */
    public void openCraftingGUI(Player player) {
        PlayerData data = economyManager.getPlayerData(player.getUniqueId());
        if (data == null) return;

        RPGClass rpgClass = RPGClass.fromString(data.getClassName());
        if (rpgClass == null) {
            player.sendMessage("§c§l[Kingdom] §7You must choose a class first!");
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 54, CRAFTING_TITLE);

        // Fill border
        ItemStack border = createPane(Material.PURPLE_STAINED_GLASS_PANE, " ");
        ItemStack corner = createPane(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            int row = i / 9, col = i % 9;
            if (row == 0 || row == 5 || col == 0 || col == 8) {
                inv.setItem(i, (i == 0 || i == 8 || i == 45 || i == 53) ? corner : border);
            }
        }

        // Title info
        ItemStack info = new ItemStack(Material.ENCHANTING_TABLE);
        ItemMeta infoMeta = info.getItemMeta();
        if (infoMeta != null) {
            infoMeta.setDisplayName("§d§l✦ Soul Forge ✦");
            infoMeta.setLore(Arrays.asList(
                    "§8§m                              ",
                    "",
                    "§7Class: " + rpgClass.getColoredName(),
                    "§7Level: §e" + data.getLevel(),
                    "",
                    "§7Craft your class weapons here.",
                    "§7Higher tiers require higher levels.",
                    "§8§m                              "
            ));
            infoMeta.addEnchant(Enchantment.UNBREAKING, 1, true);
            infoMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            info.setItemMeta(infoMeta);
        }
        inv.setItem(4, info);

        // Place weapon recipes
        List<WeaponRecipe> recipes = WeaponRecipe.getRecipesForClass(rpgClass);
        int[] recipeSlots = {20, 21, 22, 23, 24}; // 5 tiers in a row

        for (int i = 0; i < recipes.size() && i < recipeSlots.length; i++) {
            WeaponRecipe recipe = recipes.get(i);
            boolean canCraft = data.getLevel() >= recipe.getRequiredLevel();
            boolean hasIngredients = canCraft && playerHasIngredients(player, recipe);

            // Create display item
            ItemStack display = weaponManager.createWeapon(
                    recipe.getRpgClass(), recipe.getTier(),
                    recipe.getWeaponName(),
                    null,
                    plugin.getConfigManager().getWeaponDamage(recipe.getTier().name()),
                    plugin.getConfigManager().getWeaponSpeed(recipe.getTier().name())
            );

            ItemMeta meta = display.getItemMeta();
            if (meta != null) {
                List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                lore.add("");
                lore.add("§8§m                              ");
                lore.add("§7Required Level: " + (canCraft ? "§a" : "§c") + recipe.getRequiredLevel());
                lore.add("§7Soul Fragments: §b" + recipe.getSoulItemsRequired());
                lore.add("");
                lore.add("§7Ingredients:");
                for (Map.Entry<Material, Integer> ing : recipe.getIngredients().entrySet()) {
                    String matName = formatMaterialName(ing.getKey());
                    int has = countMaterial(player, ing.getKey());
                    String color = has >= ing.getValue() ? "§a" : "§c";
                    lore.add("§8  ▸ " + color + matName + " §7x" + ing.getValue() + " §8(" + has + ")");
                }

                // Soul item count
                int soulCount = countSpecialItem(player, SpecialItems.SOUL_ITEM_ID);
                String soulColor = soulCount >= recipe.getSoulItemsRequired() ? "§a" : "§c";
                lore.add("§8  ▸ " + soulColor + "Soul Fragment §7x" + recipe.getSoulItemsRequired() + " §8(" + soulCount + ")");

                lore.add("");
                if (!canCraft) {
                    lore.add("§c✗ Level too low!");
                } else if (!hasIngredients) {
                    lore.add("§c✗ Missing ingredients!");
                } else {
                    lore.add("§a▸ Click to craft!");
                }
                lore.add("§8§m                              ");

                meta.setLore(lore);

                if (!canCraft) {
                    // Grey out unavailable
                    meta.addEnchant(Enchantment.UNBREAKING, 0, true);
                    meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                }

                display.setItemMeta(meta);
            }

            inv.setItem(recipeSlots[i], display);
        }

        player.openInventory(inv);
    }

    @EventHandler
    public void onCraftingClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!event.getView().getTitle().equals(CRAFTING_TITLE)) return;

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        // Only process weapon slots
        int slot = event.getRawSlot();
        int[] validSlots = {20, 21, 22, 23, 24};
        boolean isValid = false;
        int recipeIndex = -1;
        for (int i = 0; i < validSlots.length; i++) {
            if (validSlots[i] == slot) { isValid = true; recipeIndex = i; break; }
        }
        if (!isValid) return;

        PlayerData data = economyManager.getPlayerData(player.getUniqueId());
        if (data == null) return;

        RPGClass rpgClass = RPGClass.fromString(data.getClassName());
        if (rpgClass == null) return;

        List<WeaponRecipe> recipes = WeaponRecipe.getRecipesForClass(rpgClass);
        if (recipeIndex >= recipes.size()) return;

        WeaponRecipe recipe = recipes.get(recipeIndex);

        // Verify requirements
        if (data.getLevel() < recipe.getRequiredLevel()) {
            player.sendMessage("§c§l[Kingdom] §7You need Level §e" + recipe.getRequiredLevel() + " §7to craft this!");
            return;
        }

        if (!playerHasIngredients(player, recipe)) {
            player.sendMessage("§c§l[Kingdom] §7You don't have the required ingredients!");
            return;
        }

        // Remove ingredients
        for (Map.Entry<Material, Integer> ing : recipe.getIngredients().entrySet()) {
            removeMaterial(player, ing.getKey(), ing.getValue());
        }
        removeSpecialItem(player, SpecialItems.SOUL_ITEM_ID, recipe.getSoulItemsRequired());

        // Create weapon
        List<String> weaponLore = new ArrayList<>();
        // Add final-tier special effect description
        if (recipe.getTier() == WeaponTier.CHAOS) {
            switch (rpgClass) {
                case ARCHER -> weaponLore.add("§c§l⚔ Life Steal §7on hit");
                case KNIGHT -> weaponLore.add("§b§l⚔ Defensive AOE §7on right-click");
                case WIZARD -> weaponLore.add("§5§l⚔ Empowered Fireball §7on right-click");
                case ROGUE -> weaponLore.add("§2§l⚔ Poison Damage §7on hit");
                case RONIN -> weaponLore.add("§4§l⚔ Bleed Damage §7on hit");
            }
        }

        ItemStack weapon = weaponManager.createWeapon(
                recipe.getRpgClass(), recipe.getTier(),
                recipe.getWeaponName(), weaponLore,
                plugin.getConfigManager().getWeaponDamage(recipe.getTier().name()),
                plugin.getConfigManager().getWeaponSpeed(recipe.getTier().name())
        );

        player.getInventory().addItem(weapon);
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1.2f);
        player.sendMessage("§a§l[Kingdom] §7Crafted " + recipe.getWeaponName() + "§7!");

        // Refresh GUI
        openCraftingGUI(player);
    }

    // ---- Ingredient Helpers ----

    private boolean playerHasIngredients(Player player, WeaponRecipe recipe) {
        for (Map.Entry<Material, Integer> ing : recipe.getIngredients().entrySet()) {
            if (countMaterial(player, ing.getKey()) < ing.getValue()) return false;
        }
        int soulCount = countSpecialItem(player, SpecialItems.SOUL_ITEM_ID);
        return soulCount >= recipe.getSoulItemsRequired();
    }

    private int countMaterial(Player player, Material material) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) count += item.getAmount();
        }
        return count;
    }

    private int countSpecialItem(Player player, String specialId) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && specialItems.isSpecialItem(item)) {
                String id = specialItems.getSpecialItemId(item);
                if (specialId.equals(id)) count += item.getAmount();
            }
        }
        return count;
    }

    private void removeMaterial(Player player, Material material, int amount) {
        int remaining = amount;
        for (int i = 0; i < player.getInventory().getSize() && remaining > 0; i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && item.getType() == material) {
                int remove = Math.min(item.getAmount(), remaining);
                item.setAmount(item.getAmount() - remove);
                remaining -= remove;
            }
        }
    }

    private void removeSpecialItem(Player player, String specialId, int amount) {
        int remaining = amount;
        for (int i = 0; i < player.getInventory().getSize() && remaining > 0; i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && specialItems.isSpecialItem(item)) {
                String id = specialItems.getSpecialItemId(item);
                if (specialId.equals(id)) {
                    int remove = Math.min(item.getAmount(), remaining);
                    item.setAmount(item.getAmount() - remove);
                    remaining -= remove;
                }
            }
        }
    }

    private ItemStack createPane(Material material, String name) {
        ItemStack pane = new ItemStack(material);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) { meta.setDisplayName(name); pane.setItemMeta(meta); }
        return pane;
    }

    private String formatMaterialName(Material material) {
        String name = material.name().replace("_", " ");
        String[] words = name.split(" ");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            result.append(word.charAt(0)).append(word.substring(1).toLowerCase()).append(" ");
        }
        return result.toString().trim();
    }
}
