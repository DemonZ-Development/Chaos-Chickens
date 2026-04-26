# Chaos Chickens

[![GitHub license](https://img.shields.io/github/license/DemonZ-Dev/ChaosChickens?style=flat-square)](LICENSE)
[![GitHub release](https://img.shields.io/github/v/release/DemonZ-Dev/ChaosChickens?style=flat-square&include_prereleases)](https://github.com/DemonZ-Dev/ChaosChickens/releases)
[![Build](https://img.shields.io/github/actions/workflow/status/DemonZ-Dev/ChaosChickens/build.yml?style=flat-square&branch=main)](https://github.com/DemonZ-Dev/ChaosChickens/actions)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.20.4%20--%201.21.1%2B-green?style=flat-square)](https://www.minecraft.net/)
[![Modrinth](https://img.shields.io/modrinth/dt/chaos-chickens?style=flat-square&logo=modrinth)](https://modrinth.com/project/chaos-chickens)
[![bStats](https://img.shields.io/badge/bStats-30945-blue?style=flat-square)](https://bstats.org/plugin/bukkit/Chaos%20Chickens/30945)
[![Folia Supported](https://img.shields.io/badge/Folia-Supported-blue?style=flat-square)](https://papermc.io/software/folia)

![Chaos Chickens Banner](assets/banner.png)

Chickens with random chaos traits! Every chicken that spawns has a random chaos trait — making them unpredictable, hilarious, and sometimes deadly. Players never know what a chicken will do until it's too late.

## Chaos Traits

| Trait | Effect |
|---|---|
| 💥 **Explosive** | Explodes when killed (small TNT blast, no block damage) |
| 🏃 **Speed** | Runs 3x faster, drops 2x feathers |
| 🔥 **Fire** | Sets you on fire if you get close |
| 🧲 **Magnet** | Pulls nearby items toward it (steals your loot!) |
| 💎 **Golden** | Drops random ore instead of eggs |
| 🎵 **Disco** | Plays note block sounds & dyes nearby sheep |
| 🧟 **Zombie** | Hostile — chases players and pecks (1 damage) |
| 🔄 **Teleport** | Randomly teleports every 10 seconds |
| 🧊 **Ice** | Freezes water it walks on, drops ice instead of eggs |
| 💀 **Cursed** | Gives you a random bad effect when near |
| 👑 **Boss** | Combines multiple traits — 3x health, weakness aura, dragon breath particles |

## Supported Platforms

| Platform | Module | Status |
|---|---|---|
| Spigot / Paper / Purpur | `bukkit/` | ✅ Supported |
| Folia | `bukkit/` | ✅ Supported (auto-detected, RegionScheduler) |
| Fabric | `fabric/` | ✅ Supported (with Cloth Config GUI) |
| Forge | `forge/` | ✅ Supported |

**Minecraft Version:** 1.20.4 — 1.21.1+ (all platforms)
**Singleplayer & Multiplayer:** Yes, both!

## Installation

### Bukkit (Spigot / Paper / Purpur / Folia)

1. Download `ChaosChickens-Bukkit.jar` from [Modrinth](https://modrinth.com/project/chaos-chickens) or [Releases](https://github.com/DemonZ-Dev/ChaosChickens/releases)
2. Place the JAR in your server's `plugins/` folder
3. Restart or reload your server
4. Edit `plugins/ChaosChickens/config.yml` to customize traits and chances
5. Run `/cc reload` to apply config changes

### Fabric

1. Download `ChaosChickens-Fabric.jar` from [Modrinth](https://modrinth.com/project/chaos-chickens) or [Releases](https://github.com/DemonZ-Dev/ChaosChickens/releases)
2. Make sure [Fabric Loader](https://fabricmc.net/use/) 0.15+ and [Fabric API](https://modrinth.com/mod/fabric-api) are installed
3. Place the JAR in your `mods/` folder
4. Launch the game — config is at `config/chaoschickens.json`

### Forge

1. Download `ChaosChickens-Forge.jar` from [Modrinth](https://modrinth.com/project/chaos-chickens) or [Releases](https://github.com/DemonZ-Dev/ChaosChickens/releases)
2. Make sure [Minecraft Forge](https://files.minecraftforge.net/) 47.0+ is installed
3. Place the JAR in your `mods/` folder
4. Launch the game — config is at `config/chaoschickens.json`

## Building from Source

### Prerequisites
- Java 17+
- Maven 3.8+ (for Bukkit & Common)
- Gradle 8.x (for Fabric & Forge)

### Build Common Module (required first)

```bash
cd ChaosChickens/common
mvn clean install
```

### Build Bukkit (Spigot/Paper/Purpur/Folia)

```bash
cd ../bukkit
mvn clean package
# Output: bukkit/target/chaoschickens-bukkit-1.0.0.jar
```

### Build Fabric

```bash
cd ../fabric
./gradlew build
# Output: fabric/build/libs/chaoschickens-fabric-1.0.0.jar
```

### Build Forge

```bash
cd ../forge
./gradlew build
# Output: forge/build/libs/chaoschickens-forge-1.0.0.jar
```

## Configuration

### Bukkit (config.yml)
Located at `plugins/ChaosChickens/config.yml`

```yaml
config-version: 3              # Auto-managed, do not change manually
chaos-chance: 0.35             # 35% chance a chicken gets a trait
enable-boss-chickens: true
boss-chance: 0.02              # 2% chance for a boss chicken
boss-trait-count: 3
enable-particles: true
enable-messages: true
announce-trait-on-spawn: false
only-natural-spawns: false
max-chickens-per-player: -1    # -1 = unlimited
check-for-updates: true        # Check Modrinth for updates
enable-folia-support: true     # Auto-detect Folia and use compatible scheduling
bstats-enabled: true           # Enable bStats anonymous analytics

traits:
  explosive:
    enabled: true
    weight: 1.0
  speed:
    enabled: true
    weight: 1.2
  boss:
    enabled: true
    weight: 0.02
  # ... all traits listed
```

### Fabric / Forge (chaoschickens.json)
Located at `config/chaoschickens.json`

```json
{
  "config-version": 3,
  "chaosChance": 0.35,
  "enableBossChickens": true,
  "bossChance": 0.02,
  "bossTraitCount": 3,
  "checkForUpdates": true,
  "enableFoliaSupport": true,
  "bstatsEnabled": true,
  "traits": {
    "explosive": { "enabled": true, "weight": 1.0 },
    "speed": { "enabled": true, "weight": 1.2 }
  }
}
```

### Config Migration
When updating from an older config version, the plugin automatically detects the outdated format and migrates it, adding any new fields with sensible defaults while preserving your existing settings.

## Commands (Bukkit only)

| Command | Description | Permission |
|---|---|---|
| `/cc reload` | Reload config | `chaoschickens.admin` |
| `/cc list` | List all traits + Folia status | `chaoschickens.admin` |
| `/cc spawn [trait]` | Spawn a chaos chicken (safe location) | `chaoschickens.admin` |
| `/cc give [trait]` | Give a chaos chicken egg | `chaoschickens.admin` |
| `/cc toggle [trait]` | Toggle a trait on/off (runtime only) | `chaoschickens.admin` |
| `/cc update` | Check for updates on Modrinth | `chaoschickens.admin` |
| Right-click chicken | Show trait info | `chaoschickens.see_traits` |

## API for Modders

The common module provides a public API that other plugins/mods can use:

```java
// Register a custom trait
ChaosChickensAPI.registerTrait(myTrait, 1.0);

// Pick a random trait
TraitType type = ChaosChickensAPI.pickRandomTrait(random);

// Enable/disable traits
ChaosChickensAPI.setTraitEnabled(TraitType.EXPLOSIVE, false);

// Get trait info
Optional<ChaosTrait> trait = ChaosChickensAPI.getTrait(TraitType.GOLDEN);

// Check for updates
UpdateChecker.setCurrentVersion("1.0.0");
UpdateChecker.checkForUpdates().thenAccept(available -> {
    if (available) System.out.println(UpdateChecker.getUpdateMessage());
});
```

### Creating a Custom Trait

1. Create a class extending the platform's base trait (`BukkitTrait` / `FabricTrait` / `ForgeTrait`)
2. Implement the lifecycle methods: `onApply()`, `onTick()`, `onDeath()`, `onPlayerNear()`
3. Register it via `ChaosChickensAPI.registerTrait()`

```java
// Bukkit example
public class GravityTrait extends BukkitTrait {
    public GravityTrait() {
        super(TraitType.CUSTOM, "Floats upward!", 0.5, false, true, 20);
    }

    @Override
    public void onApply(Chicken chicken) {
        chicken.setCustomName(ChatColor.LIGHT_PURPLE + "Gravity Chicken");
    }

    @Override
    public void onTick(Chicken chicken) {
        chicken.setVelocity(chicken.getVelocity().add(new Vector(0, 0.1, 0)));
    }
}
```

## bStats Analytics

This plugin uses [bStats](https://bstats.org/plugin/bukkit/Chaos%20Chickens/30945) to collect anonymous usage data. You can disable it in the config with `bstats-enabled: false`.

## Credits

**Chaos Chickens** is created and maintained by the **DemonZ Development** community.

| Role | Member |
|---|---|
| Lead Developer | Cyrus |
| Community | [DemonZ Development](https://github.com/DemonZ-Dev) |

Contributions are welcome! Feel free to open issues, submit pull requests, or suggest new chaos traits.

## License

This project is licensed under the **GNU General Public License v3.0** — see the [LICENSE](LICENSE) file for details.
