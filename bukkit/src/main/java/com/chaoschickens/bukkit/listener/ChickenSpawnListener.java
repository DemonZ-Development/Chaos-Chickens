package com.chaoschickens.bukkit.listener;

import com.chaoschickens.bukkit.ChaosChickensBukkit;
import com.chaoschickens.bukkit.trait.BukkitTrait;
import com.chaoschickens.bukkit.util.ChickenDataUtil;
import com.chaoschickens.common.trait.TraitType;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Listener that handles chicken spawn events.
 * When a chicken spawns, rolls for chaos chance and assigns a random trait.
 */
public class ChickenSpawnListener implements Listener {

    private final ChaosChickensBukkit plugin;
    /** Tracks pending egg traits: player UUID -> trait type. Set when player uses a chaos egg. */
    private final Map<UUID, TraitType> pendingEggTraits = new ConcurrentHashMap<>();

    public ChickenSpawnListener(ChaosChickensBukkit plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        // Only handle chicken spawns
        if (!(event.getEntity() instanceof Chicken)) return;

        Chicken chicken = (Chicken) event.getEntity();

        // Handle spawn egg with chaos trait PDC
        if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.SPAWNER_EGG) {
            // Find the nearest player and check for a pending egg trait
            Player nearestPlayer = null;
            double nearestDist = Double.MAX_VALUE;
            for (org.bukkit.entity.Entity nearby : chicken.getNearbyEntities(10, 10, 10)) {
                if (nearby instanceof Player) {
                    double dist = nearby.getLocation().distanceSquared(chicken.getLocation());
                    if (dist < nearestDist) {
                        nearestDist = dist;
                        nearestPlayer = (Player) nearby;
                    }
                }
            }
            if (nearestPlayer != null) {
                TraitType pending = pendingEggTraits.remove(nearestPlayer.getUniqueId());
                if (pending != null && pending != TraitType.EMPTY) {
                    plugin.assignTrait(chicken, pending);
                    return;
                }
            }
        }

        // Check if only natural spawns should be affected
        if (plugin.getConfigManager().isOnlyNaturalSpawns()) {
            CreatureSpawnEvent.SpawnReason reason = event.getSpawnReason();
            if (reason != CreatureSpawnEvent.SpawnReason.NATURAL) {
                return;
            }
        }

        // Roll for chaos chance
        double chaosChance = plugin.getConfigManager().getChaosChance();
        if (plugin.getRandom().nextDouble() >= chaosChance) return;

        // Check for boss chicken first
        if (plugin.getConfigManager().isEnableBossChickens()) {
            double bossChance = plugin.getConfigManager().getBossChance();
            if (plugin.getRandom().nextDouble() < bossChance) {
                applyBossTrait(chicken);
                return;
            }
        }

        // Pick a random trait and apply it
        TraitType traitType = plugin.pickRandomTrait();
        if (traitType == TraitType.EMPTY) return;

        plugin.assignTrait(chicken, traitType);
    }

    /**
     * Detect when a player uses a chaos chicken spawn egg and register the pending trait.
     * This ensures the spawned chicken gets the correct trait from the egg's PDC data.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.CHICKEN_SPAWN_EGG) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        NamespacedKey eggTraitKey = new NamespacedKey(plugin, "egg_trait");
        if (!meta.getPersistentDataContainer().has(eggTraitKey, PersistentDataType.STRING)) return;

        String traitKey = meta.getPersistentDataContainer().get(eggTraitKey, PersistentDataType.STRING);
        if (traitKey != null) {
            TraitType traitType = TraitType.fromKey(traitKey);
            if (traitType != TraitType.EMPTY) {
                pendingEggTraits.put(event.getPlayer().getUniqueId(), traitType);
                // Auto-cleanup after 5 seconds in case the spawn fails
                plugin.getServer().getScheduler().runTaskLater(plugin,
                        () -> pendingEggTraits.remove(event.getPlayer().getUniqueId()), 100L);
            }
        }
    }

    /**
     * Apply boss traits to a chicken (multiple traits combined).
     */
    private void applyBossTrait(Chicken chicken) {
        plugin.assignTrait(chicken, TraitType.BOSS);
        // onApply is already called inside assignTrait(), no duplicate call needed
    }
}
