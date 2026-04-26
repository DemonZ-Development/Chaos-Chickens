---
title: Chaos Chickens
summary: Chickens with random chaos traits! Explosive, speedy, golden, teleporting, and more.
authors:
  - DemonZ Development
  - Cyrus
categories:
  - gameplay
  - adventure
  - utility
license: GPL-3.0
project-url: https://github.com/DemonZ-Dev/ChaosChickens
source-url: https://github.com/DemonZ-Dev/ChaosChickens
issues-url: https://github.com/DemonZ-Dev/ChaosChickens/issues
---

![Chaos Chickens Banner](https://raw.githubusercontent.com/DemonZ-Dev/ChaosChickens/main/assets/banner.png)

# Chaos Chickens

Chickens with random chaos traits! Every chicken that spawns has a random chaos trait — making them unpredictable, hilarious, and sometimes deadly. Players never know what a chicken will do until it's too late.

## Chaos Traits

| Trait | Effect | Hostile? |
|---|---|---|
| 💥 **Explosive** | Explodes when killed (small TNT blast, no block damage) | On death |
| 🏃 **Speed** | Runs 3x faster, drops 2x feathers | No |
| 🔥 **Fire** | Sets you on fire if you get close | Yes |
| 🧲 **Magnet** | Pulls nearby items toward it (steals your loot!) | No |
| 💎 **Golden** | Drops random ore instead of eggs | No |
| 🎵 **Disco** | Plays note block sounds & dyes nearby sheep | No |
| 🧟 **Zombie** | Chases players and pecks (1 damage) | Yes |
| 🔄 **Teleport** | Randomly teleports around every 10 seconds | No |
| 🧊 **Ice** | Freezes water it walks on, drops ice instead of eggs | No |
| 💀 **Cursed** | Gives you a random bad effect when near | Yes |
| 👑 **Boss** | Combines multiple traits — 3x health, weakness aura, dragon breath particles | Yes |

## Boss Chickens

Boss Chickens are rare (2% chance by default) and combine the powers of multiple chaos traits into one formidable chicken. They have 3x health, emit dragon breath particles, and apply Weakness to nearby players. When killed, they produce a massive explosion effect.

## Supported Platforms

| Platform | Versions | Status |
|---|---|---|
| Spigot / Paper / Purpur | 1.20.4 — 1.21.1+ | ✅ Full support |
| Folia | 1.20.4 — 1.21.1+ | ✅ Auto-detected |
| Fabric | 1.20.4 — 1.21.1+ | ✅ Full support |
| Forge | 1.20.4 — 1.21.1+ | ✅ Full support |

Both **singleplayer** and **multiplayer** are fully supported!

## Installation

### Bukkit (Spigot / Paper / Purpur / Folia)

1. Download `ChaosChickens-Bukkit.jar`
2. Place the JAR in your server's `plugins/` folder
3. Restart your server
4. Edit `plugins/ChaosChickens/config.yml` to customize traits and chances
5. Run `/cc reload` to apply config changes

### Fabric

1. Make sure [Fabric Loader](https://fabricmc.net/use/) 0.15+ and [Fabric API](https://modrinth.com/mod/fabric-api) are installed
2. Download `ChaosChickens-Fabric.jar`
3. Place the JAR in your `mods/` folder
4. Launch the game — config is at `config/chaoschickens.json`

### Forge

1. Make sure [Minecraft Forge](https://files.minecraftforge.net/) 47.0+ is installed
2. Download `ChaosChickens-Forge.jar`
3. Place the JAR in your `mods/` folder
4. Launch the game — config is at `config/chaoschickens.json`

## Configuration

### Bukkit (config.yml)
Located at `plugins/ChaosChickens/config.yml`

```yaml
config-version: 3              # Auto-managed, do not change
chaos-chance: 0.35             # 35% chance a chicken gets a trait
enable-boss-chickens: true
boss-chance: 0.02              # 2% chance for boss chicken
boss-trait-count: 3
enable-particles: true
enable-messages: true
announce-trait-on-spawn: false
only-natural-spawns: false
max-chickens-per-player: -1    # -1 = unlimited
check-for-updates: true        # Modrinth update checker
enable-folia-support: true     # Auto-detect Folia
bstats-enabled: true           # Anonymous analytics (bStats)

traits:
  explosive: { enabled: true, weight: 1.0 }
  speed: { enabled: true, weight: 1.2 }
  # ... all traits listed individually
```

### Fabric / Forge (chaoschickens.json)
Located at `config/chaoschickens.json`

```json
{
  "config-version": 3,
  "chaosChance": 0.35,
  "enableBossChickens": true,
  "bossChance": 0.02,
  "checkForUpdates": true,
  "bstatsEnabled": true
}
```

**Config Migration**: When updating from an older config, the plugin automatically detects the outdated format and migrates it, adding new fields with sensible defaults while preserving your existing settings.

## Commands (Bukkit only)

| Command | Description | Permission |
|---|---|---|
| `/cc reload` | Reload configuration | `chaoschickens.admin` |
| `/cc list` | List all traits + Folia status | `chaoschickens.admin` |
| `/cc spawn <trait>` | Spawn a chaos chicken (safe location) | `chaoschickens.admin` |
| `/cc give <trait>` | Give a chaos chicken egg | `chaoschickens.admin` |
| `/cc toggle <trait>` | Toggle a trait on/off (runtime only) | `chaoschickens.admin` |
| `/cc update` | Check for updates on Modrinth | `chaoschickens.admin` |
| Right-click chicken | Show trait info | `chaoschickens.see_traits` |

## API for Modders

The common module provides a public API for other plugins/mods:

```java
// Register a custom trait
ChaosChickensAPI.registerTrait(myTrait, 1.0);

// Pick a random trait
TraitType type = ChaosChickensAPI.pickRandomTrait(random);

// Enable/disable traits
ChaosChickensAPI.setTraitEnabled(TraitType.EXPLOSIVE, false);

// Check for updates
UpdateChecker.setCurrentVersion("1.0.0");
UpdateChecker.checkForUpdates().thenAccept(available -> {
    if (available) System.out.println(UpdateChecker.getUpdateMessage());
});
```

## bStats Analytics

This plugin uses [bStats](https://bstats.org/plugin/bukkit/Chaos%20Chickens/30945) to collect anonymous usage data. This helps us understand how the plugin is used and prioritize features. You can disable it in the config with `bstats-enabled: false`.

Collected data includes:
- Active chaos chicken count
- Chaos chance percentage
- Folia detection status
- Trait distribution

## Credits

**Chaos Chickens** is created and maintained by the **DemonZ Development** community.

| Role | Member |
|---|---|
| Lead Developer | Cyrus |
| Community | [DemonZ Development](https://github.com/DemonZ-Dev) |

## License

This project is licensed under the **GNU General Public License v3.0**.
