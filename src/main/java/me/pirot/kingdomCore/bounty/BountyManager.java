package me.pirot.kingdomCore.bounty;

import me.pirot.kingdomCore.database.PlayerData;
import me.pirot.kingdomCore.economy.EconomyManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

/**
 * Manages the bounty system: placing bounties, tracking the top bounty,
 * and claiming bounties on kills.
 */
public class BountyManager {

    private final EconomyManager economyManager;

    public BountyManager(EconomyManager economyManager) {
        this.economyManager = economyManager;
    }

    /**
     * Place a bounty on a target player.
     *
     * @return true if successful, false if insufficient funds
     */
    public boolean placeBounty(Player source, Player target, int amount) {
        UUID sourceUuid = source.getUniqueId();

        // Deduct shards from source
        if (!economyManager.removeShards(sourceUuid, amount)) {
            return false;
        }

        // Add to target's bounty
        PlayerData targetData = economyManager.getPlayerData(target.getUniqueId());
        if (targetData != null) {
            targetData.addBounty(amount);
        }

        return true;
    }

    /**
     * Claim a bounty when a player is killed.
     *
     * @param killer the player who killed the target
     * @param victim the target with a bounty
     * @return the amount of shards claimed (0 if no bounty)
     */
    public int claimBounty(Player killer, Player victim) {
        PlayerData victimData = economyManager.getPlayerData(victim.getUniqueId());
        if (victimData == null || victimData.getBounty() <= 0) return 0;

        int bountyAmount = victimData.getBounty();
        victimData.setBounty(0);

        // Award shards to killer
        economyManager.addShards(killer.getUniqueId(), bountyAmount);

        return bountyAmount;
    }

    /**
     * Get the player with the highest bounty among online players.
     *
     * @return the Player with the highest bounty, or null if no one has a bounty
     */
    public Player getTopBountyPlayer() {
        Player topPlayer = null;
        int topBounty = 0;

        for (Map.Entry<UUID, PlayerData> entry : economyManager.getCache().entrySet()) {
            if (entry.getValue().getBounty() > topBounty) {
                Player player = Bukkit.getPlayer(entry.getKey());
                if (player != null && player.isOnline()) {
                    topBounty = entry.getValue().getBounty();
                    topPlayer = player;
                }
            }
        }

        return topPlayer;
    }

    /**
     * Get the highest bounty amount.
     */
    public int getTopBountyAmount() {
        int topBounty = 0;
        for (PlayerData data : economyManager.getCache().values()) {
            if (data.getBounty() > topBounty) {
                Player player = Bukkit.getPlayer(data.getUuid());
                if (player != null && player.isOnline()) {
                    topBounty = data.getBounty();
                }
            }
        }
        return topBounty;
    }

    /**
     * Get bounty amount for a specific player.
     */
    public int getBounty(UUID uuid) {
        PlayerData data = economyManager.getPlayerData(uuid);
        return data != null ? data.getBounty() : 0;
    }
}
