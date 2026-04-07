package me.pirot.kingdomCore.bounty;

import me.pirot.kingdomCore.KingdomCore;
import me.pirot.kingdomCore.config.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;

/**
 * Handles bounty claiming on kill and compass tracking updates.
 */
public class BountyListener implements Listener {

    private final KingdomCore plugin;
    private final BountyManager bountyManager;
    private final ConfigManager configManager;

    public BountyListener(KingdomCore plugin, BountyManager bountyManager, ConfigManager configManager) {
        this.plugin = plugin;
        this.bountyManager = bountyManager;
        this.configManager = configManager;
    }

    /**
     * On player death, if killed by another player and victim has a bounty,
     * award the bounty to the killer.
     */
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        if (killer == null) return;
        if (killer.equals(victim)) return;

        int claimed = bountyManager.claimBounty(killer, victim);
        if (claimed > 0) {
            Bukkit.broadcastMessage("§6§l[Bounty] §f" + killer.getName() + " §7claimed the §a" +
                    claimed + " Shard §7bounty on §f" + victim.getName() + "§7!");
            killer.sendMessage("§a§l[Kingdom] §7You earned §a" + claimed + " Shards §7from the bounty!");
        }
    }

    /**
     * Start the repeating task that updates all Bounty Compass items
     * to point toward the player with the highest bounty.
     */
    public void startCompassTask() {
        int ticks = configManager.getCompassUpdateTicks();
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            Player topPlayer = bountyManager.getTopBountyPlayer();
            if (topPlayer == null) return;

            Location targetLocation = topPlayer.getLocation();

            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.equals(topPlayer)) continue;

                for (ItemStack item : player.getInventory().getContents()) {
                    if (item == null) continue;
                    if (item.getType() != Material.COMPASS) continue;

                    // Check if it's a "Bounty Compass" by display name
                    if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()
                            && item.getItemMeta().getDisplayName().contains("Bounty")) {
                        CompassMeta compassMeta = (CompassMeta) item.getItemMeta();
                        compassMeta.setLodestone(targetLocation);
                        compassMeta.setLodestoneTracked(false); // Don't require actual lodestone
                        int topBounty = bountyManager.getTopBountyAmount();
                        compassMeta.setDisplayName("§6§lBounty Compass");
                        compassMeta.setLore(java.util.Arrays.asList(
                                "§7Tracking: §f" + topPlayer.getName(),
                                "§7Bounty: §a" + topBounty + " Shards"
                        ));
                        item.setItemMeta(compassMeta);
                    }
                }
            }
        }, ticks, ticks);
    }
}
