package com.chaoschickens.bukkit.command;

import com.chaoschickens.bukkit.ChaosChickensBukkit;
import com.chaoschickens.bukkit.trait.BukkitTrait;
import com.chaoschickens.bukkit.util.ChickenDataUtil;
import com.chaoschickens.common.trait.TraitType;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Command handler for /chaoschickens (alias /cc).
 * Supports reload, give, list, spawn, toggle, and update subcommands.
 */
public class ChaosCommand implements CommandExecutor, TabCompleter {

    private final ChaosChickensBukkit plugin;

    public ChaosCommand(ChaosChickensBukkit plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "reload":
                return handleReload(sender);
            case "give":
                return handleGive(sender, args);
            case "list":
                return handleList(sender);
            case "spawn":
                return handleSpawn(sender, args);
            case "toggle":
                return handleToggle(sender, args);
            case "update":
                return handleUpdate(sender);
            default:
                sendHelp(sender);
                return true;
        }
    }

    /**
     * /cc reload - Reload the plugin configuration.
     */
    private boolean handleReload(CommandSender sender) {
        plugin.reloadConfig();
        plugin.loadConfig();
        sender.sendMessage(ChatColor.GREEN + "[ChaosChickens] Configuration reloaded! (Config version: "
                + plugin.getConfigManager().getConfigVersion() + ")");
        return true;
    }

    /**
     * /cc give [trait] - Give the player a chaos chicken spawn egg.
     * Bug #5 fix: Checks if inventory is full before adding.
     */
    private boolean handleGive(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players!");
            return true;
        }

        Player player = (Player) sender;

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /cc give <trait>");
            return true;
        }

        TraitType traitType = TraitType.fromKey(args[1]);
        if (traitType == TraitType.EMPTY && !args[1].equalsIgnoreCase("empty")) {
            player.sendMessage(ChatColor.RED + "Unknown trait: " + args[1]);
            return true;
        }

        // Create a chicken spawn egg with trait info in the lore and PDC
        ItemStack egg = new ItemStack(Material.CHICKEN_SPAWN_EGG, 1);
        ItemMeta meta = egg.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + "Chaos Chicken Egg (" + traitType.getDisplayName() + ")");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Trait: " + ChatColor.YELLOW + traitType.getKey());
            lore.add(ChatColor.GRAY + "Place to spawn a chaos chicken!");
            meta.setLore(lore);
            // Store trait key in PDC so the spawn listener can read it
            NamespacedKey eggTraitKey = new NamespacedKey(plugin, "egg_trait");
            meta.getPersistentDataContainer().set(eggTraitKey, PersistentDataType.STRING, traitType.getKey());
            egg.setItemMeta(meta);
        }

        // Bug #5 fix: Check inventory full — drop leftover items, not the original
        java.util.Map<Integer, ItemStack> leftover = player.getInventory().addItem(egg);
        if (!leftover.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "[ChaosChickens] Your inventory is full! " +
                    "The spawn egg was dropped on the ground.");
            for (ItemStack drop : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }
        } else {
            player.sendMessage(ChatColor.GREEN + "[ChaosChickens] Given " +
                    ChatColor.YELLOW + traitType.getDisplayName() +
                    ChatColor.GREEN + " spawn egg!");
        }
        return true;
    }

    /**
     * /cc list - List all traits and their status.
     */
    private boolean handleList(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== Chaos Chicken Traits ===");

        Map<TraitType, BukkitTrait> traitMap = plugin.getTraitMap();
        for (Map.Entry<TraitType, BukkitTrait> entry : traitMap.entrySet()) {
            TraitType type = entry.getKey();
            BukkitTrait trait = entry.getValue();
            boolean enabled = com.chaoschickens.api.ChaosChickensAPI.isTraitEnabled(type);
            double weight = com.chaoschickens.api.ChaosChickensAPI.getTraitWeight(type);

            String status = enabled ? ChatColor.GREEN + "Enabled" : ChatColor.RED + "Disabled";
            sender.sendMessage(ChatColor.YELLOW + type.getKey() +
                    ChatColor.GRAY + " - " + status +
                    ChatColor.GRAY + " (weight: " + ChatColor.WHITE + weight + ChatColor.GRAY + ")" +
                    (trait.isHostile() ? ChatColor.RED + " [Hostile]" : "") +
                    (trait.isPeriodic() ? ChatColor.BLUE + " [Periodic]" : ""));
        }

        sender.sendMessage(ChatColor.GRAY + "Active chaos chickens: " +
                ChatColor.WHITE + plugin.getActiveChickenCount());
        sender.sendMessage(ChatColor.GRAY + "Folia mode: " +
                ChatColor.WHITE + (plugin.isFolia() ? "Active" : "Not detected"));
        return true;
    }

    /**
     * /cc spawn [trait] - Spawn a chaos chicken near the player.
     * Bug #3 fix: Uses safe spawn location check.
     */
    private boolean handleSpawn(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players!");
            return true;
        }

        Player player = (Player) sender;

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Usage: /cc spawn <trait>");
            return true;
        }

        TraitType traitType = TraitType.fromKey(args[1]);
        if (traitType == TraitType.EMPTY && !args[1].equalsIgnoreCase("empty")) {
            player.sendMessage(ChatColor.RED + "Unknown trait: " + args[1]);
            return true;
        }

        // Bug #3 fix: Find a safe spawn location
        Location safeLoc = findSafeSpawnLocation(player);
        if (safeLoc == null) {
            player.sendMessage(ChatColor.RED + "[ChaosChickens] Could not find a safe location to spawn!");
            return true;
        }

        Chicken chicken = (Chicken) player.getWorld().spawnEntity(safeLoc, EntityType.CHICKEN);
        plugin.assignTrait(chicken, traitType);

        player.sendMessage(ChatColor.GREEN + "[ChaosChickens] Spawned " +
                ChatColor.YELLOW + traitType.getDisplayName() +
                ChatColor.GREEN + " near you!");
        return true;
    }

    /**
     * /cc toggle [trait] - Toggle a trait on or off.
     */
    private boolean handleToggle(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /cc toggle <trait>");
            return true;
        }

        TraitType traitType = TraitType.fromKey(args[1]);
        if (traitType == TraitType.EMPTY && !args[1].equalsIgnoreCase("empty")) {
            sender.sendMessage(ChatColor.RED + "Unknown trait: " + args[1]);
            return true;
        }

        boolean currentlyEnabled = com.chaoschickens.api.ChaosChickensAPI.isTraitEnabled(traitType);
        com.chaoschickens.api.ChaosChickensAPI.setTraitEnabled(traitType, !currentlyEnabled);

        String newState = !currentlyEnabled ? ChatColor.GREEN + "enabled" : ChatColor.RED + "disabled";
        sender.sendMessage(ChatColor.GREEN + "[ChaosChickens] Trait " +
                ChatColor.YELLOW + traitType.getDisplayName() +
                ChatColor.GREEN + " is now " + newState + ChatColor.GRAY +
                " (Note: this does not persist to config file. Use /cc reload to reset.)");
        return true;
    }

    /**
     * /cc update - Check for updates on Modrinth.
     */
    private boolean handleUpdate(CommandSender sender) {
        sender.sendMessage(ChatColor.YELLOW + "[ChaosChickens] Checking for updates...");
        com.chaoschickens.common.util.UpdateChecker.checkForUpdates().thenAccept(available -> {
            if (available) {
                sender.sendMessage(ChatColor.GOLD + "[ChaosChickens] " +
                        com.chaoschickens.common.util.UpdateChecker.getUpdateMessage());
            } else {
                sender.sendMessage(ChatColor.GREEN + "[ChaosChickens] You are running the latest version!");
            }
        });
        return true;
    }

    /**
     * Find a safe spawn location near the player.
     * Bug #3 fix: Checks for air blocks and avoids spawning inside blocks.
     *
     * @param player The player to spawn near
     * @return A safe Location, or null if none found
     */
    private Location findSafeSpawnLocation(Player player) {
        Location playerLoc = player.getLocation();
        Location candidate = playerLoc.clone().add(playerLoc.getDirection().multiply(2));

        // Try the candidate location and nearby adjustments
        for (int y = 0; y <= 3; y++) {
            Location testLoc = candidate.clone().add(0, y, 0);
            Block feet = testLoc.getBlock();
            Block head = testLoc.clone().add(0, 1, 0).getBlock();
            Block below = testLoc.clone().add(0, -1, 0).getBlock();

            // Check: feet and head are air, below is solid
            if (feet.getType().isAir() && head.getType().isAir() && below.getType().isSolid()) {
                testLoc.setX(testLoc.getBlockX() + 0.5);
                testLoc.setZ(testLoc.getBlockZ() + 0.5);
                return testLoc;
            }
        }

        // Fallback: spawn directly at player location (safe because player is standing there)
        return playerLoc.clone().add(0, 1, 0);
    }

    /**
     * Send help message to the sender.
     */
    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== ChaosChickens Commands ===");
        sender.sendMessage(ChatColor.YELLOW + "/cc reload" + ChatColor.GRAY + " - Reload configuration");
        sender.sendMessage(ChatColor.YELLOW + "/cc give <trait>" + ChatColor.GRAY + " - Give yourself a chaos chicken egg");
        sender.sendMessage(ChatColor.YELLOW + "/cc list" + ChatColor.GRAY + " - List all traits and their status");
        sender.sendMessage(ChatColor.YELLOW + "/cc spawn <trait>" + ChatColor.GRAY + " - Spawn a chaos chicken near you");
        sender.sendMessage(ChatColor.YELLOW + "/cc toggle <trait>" + ChatColor.GRAY + " - Toggle a trait on/off (runtime only)");
        sender.sendMessage(ChatColor.YELLOW + "/cc update" + ChatColor.GRAY + " - Check for updates on Modrinth");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            // Suggest subcommands
            List<String> subCommands = Arrays.asList("reload", "give", "list", "spawn", "toggle", "update");
            String input = args[0].toLowerCase();
            for (String sub : subCommands) {
                if (sub.startsWith(input)) {
                    completions.add(sub);
                }
            }
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            if (subCommand.equals("give") || subCommand.equals("spawn") || subCommand.equals("toggle")) {
                // Suggest trait types
                String input = args[1].toLowerCase();
                for (TraitType type : TraitType.values()) {
                    if (type != TraitType.EMPTY && type != TraitType.CUSTOM) {
                        if (type.getKey().startsWith(input)) {
                            completions.add(type.getKey());
                        }
                    }
                }
            }
        }

        return completions;
    }
}
