package com.chaoschickens.bukkit.listener;

import com.chaoschickens.bukkit.ChaosChickensBukkit;
import com.chaoschickens.bukkit.util.ChickenDataUtil;
import com.chaoschickens.common.trait.TraitType;
import org.bukkit.ChatColor;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

/**
 * Listener that handles player interactions with chickens.
 * Right-clicking a chaos chicken with an empty hand shows trait info.
 */
public class ChickenInteractListener implements Listener {

    private final ChaosChickensBukkit plugin;

    public ChickenInteractListener(ChaosChickensBukkit plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        // Only handle chicken interactions
        if (!(event.getRightClicked() instanceof Chicken)) return;

        Player player = event.getPlayer();

        // Check permission
        if (!player.hasPermission("chaoschickens.see_traits")) return;

        // Check if player is holding nothing (empty hand)
        if (player.getInventory().getItem(event.getHand()) != null &&
                !player.getInventory().getItem(event.getHand()).getType().isAir()) {
            return;
        }

        Chicken chicken = (Chicken) event.getRightClicked();

        // Check if this chicken has a chaos trait
        if (!ChickenDataUtil.hasTrait(plugin, chicken)) return;

        TraitType traitType = ChickenDataUtil.getTrait(plugin, chicken);
        if (traitType == TraitType.EMPTY) return;

        // Display trait info to the player
        String traitName = traitType.getDisplayName();
        String description = "";

        // Get description from the trait implementation
        com.chaoschickens.bukkit.trait.BukkitTrait trait = plugin.getTraitMap().get(traitType);
        if (trait != null) {
            description = trait.getDescription();
        }

        player.sendMessage(ChatColor.GOLD + "=== Chaos Chicken Info ===");
        player.sendMessage(ChatColor.YELLOW + "Trait: " + ChatColor.WHITE + traitName);
        player.sendMessage(ChatColor.YELLOW + "Type: " + ChatColor.WHITE + traitType.getKey());
        if (!description.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "Description: " + ChatColor.GRAY + description);
        }
        player.sendMessage(ChatColor.YELLOW + "Hostile: " +
                (trait != null && trait.isHostile() ? ChatColor.RED + "Yes" : ChatColor.GREEN + "No"));
    }
}
