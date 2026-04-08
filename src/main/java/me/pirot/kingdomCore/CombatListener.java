package me.pirot.kingdomCore;

import me.pirot.kingdomCore.database.PlayerData;
import me.pirot.kingdomCore.economy.EconomyManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

/**
 * Tracks player combat statistics (kills and deaths) in real-time.
 */
public class CombatListener implements Listener {

    private final EconomyManager economyManager;

    public CombatListener(EconomyManager economyManager) {
        this.economyManager = economyManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        // Track death for victim
        PlayerData victimData = economyManager.getPlayerData(victim.getUniqueId());
        if (victimData != null) {
            victimData.addDeaths(1);
        }

        // Track kill for killer
        if (killer != null && !killer.equals(victim)) {
            PlayerData killerData = economyManager.getPlayerData(killer.getUniqueId());
            if (killerData != null) {
                killerData.addKills(1);
            }
        }
    }
}
