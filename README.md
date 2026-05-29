<div align="center">

![Chaos Chickens Banner](https://github.com/DemonZ-Development/Chaos-Chickens/raw/main/assets/banner.png)

<br/>

<img src="https://github.com/DemonZ-Development/Chaos-Chickens/raw/main/assets/icon.png" width="128" height="128" alt="Chaos Chickens Logo" />

# 🐔 Chaos Chickens

[![License](https://img.shields.io/badge/license-GPL--3.0-blue?style=for-the-badge)](LICENSE)
[![GitHub release](https://img.shields.io/github/v/release/DemonZ-Development/Chaos-Chickens?style=for-the-badge&color=orange&include_prereleases)](https://github.com/DemonZ-Development/Chaos-Chickens/releases)
[![Build Status](https://img.shields.io/github/actions/workflow/status/DemonZ-Development/Chaos-Chickens/build.yml?style=for-the-badge&color=green&branch=main)](https://github.com/DemonZ-Development/Chaos-Chickens/actions)
[![Minecraft Support](https://img.shields.io/badge/Minecraft-1.20%20--%2026.x-brightgreen?style=for-the-badge)](https://www.minecraft.net/)
[![bStats Metrics](https://img.shields.io/badge/bStats-30945-blueviolet?style=for-the-badge)](https://bstats.org/plugin/bukkit/Chaos%20Chickens/30945)

**Unpredictable. Hilarious. Deadly.**  
Inject chaos into your Minecraft server or client with chickens that possess randomized magic traits, combat mutations, and rare boss behaviors!

[📖 Read the Full GitHub Wiki](wiki/Home) | [💬 Join the Discord](https://discord.com/invite/GYsTt96ypf) | [📥 Download on Modrinth](https://modrinth.com/project/chaos-chickens)

</div>

---

## 🌟 Introduction

Every time a chicken spawns in your Minecraft world, there is a **35% chance** (fully configurable) that it will gain a completely random **Chaos Trait**! Players will never know what a chicken will do until they approach it, attack it, or try to harvest its eggs.

From simple speed boosts to massive detonations, item-stealing magnets, and rare multi-trait **Boss Chickens**, this mod/plugin adds action-packed gameplay to survival, faction, and adventure servers.

---

## 🔮 Featured Chaos Traits

| Icon | Trait Name | Hostility | Proximity Range | Description |
|:---:|---|---|---|---|
| 💥 | **Explosive** | ⚡ High (on death) | N/A | Detonates on death. Power is safe (visual & damage, **no block damage**). |
| 🏃 | **Speed** | ⚪ Passive | N/A | Runs at a blistering **3x default speed** (`0.75`). |
| 🔥 | **Fire** | 🟡 Medium | N/A | Sets block underneath on fire. Completely immune to fire/lava damage. |
| 🧲 | **Magnet** | ⚪ Passive | 6.0 blocks | Pulls nearby dropped items toward itself (steals player loot!). |
| 💎 | **Golden** | ⚪ Passive | N/A | Glows gold. Drops nuggets, gold/iron ingots, emeralds, or diamonds on death! |
| 🎵 | **Disco** | ⚪ Passive | N/A | Spawns rainbow rings & plays note block chimes continuously. |
| 🧟 | **Zombie** | 🔴 Hostile | 8.0 blocks | Chases players, pecks for damage, and summons custom zombie reinforcements. |
| 🔄 | **Teleport** | ⚪ Passive | 5.0 blocks | Teleports away randomly when approached or taking damage. |
| 🧊 | **Ice** | ⚪ Passive | N/A | Freezes water below it into Frosted Ice, places snow layers on solid blocks. |
| 💀 | **Cursed** | 🔴 Hostile | 6.0 blocks | Inflicts Blindness, Slowness, or Poison to players who get too close. |
| 👑 | **Boss** | ⚡ Extreme | 16.0 blocks | **Apex Chicken!** Combines 3+ random active traits, 3x max health, and weakness aura. |

> [!TIP]
> Need more details on trait values and custom mechanics? Head over to the **[Traits Encyclopedia Wiki Page](wiki/Traits)**!

---

## 🔌 Supported Platforms

* **Bukkit / Spigot / Paper / Purpur**: ✅ Full native support (`1.20 - 26.x`).
* **Folia**: ✅ Full native support (utilizes Folia's `GlobalRegionScheduler` asynchronously, guaranteeing zero tick lag).
* **Fabric**: ✅ Full native support (`1.21 - 26.x`, configures via `config/chaoschickens.json`).
* **Forge**: ✅ Full native support (`1.21 - 26.x`, bundled with core common systems directly inside the mod jar).

---

## 🚀 Fast Installation

Want to set up Chaos Chickens quickly? Check out our quick guides:

### Bukkit / Paper / Spigot / Folia
1. Download `chaoschickens-bukkit-X.Y.Z.jar` from [Releases](https://github.com/DemonZ-Development/Chaos-Chickens/releases).
2. Move the JAR into your server's `plugins/` directory.
3. Restart the server.
4. Edit configuration details in `plugins/ChaosChickens/config.yml`.
5. Run `/cc reload` to apply updates!

### Fabric
1. Download `chaoschickens-fabric-X.Y.Z.jar` and make sure you have Fabric API installed.
2. Put the JAR in your `mods/` directory.
3. Boot client or server. Config is at `config/chaoschickens.json`.

### Forge
1. Download `chaoschickens-forge-X.Y.Z.jar` and make sure you have Forge loaded.
2. Put the JAR in your `mods/` directory.
3. Boot client or server. Config is at `config/chaoschickens.json`.

> [!NOTE]
> For detailed troubleshooting, version support guides, or advanced steps, check out the **[Installation Wiki Page](wiki/Installation)**.

---

## 🛠️ Building from Source

To compile and package Chaos Chickens yourself, ensure you have **Java 17/21**, **Maven 3.8+**, and **Gradle 8.5+** installed.

```bash
# Build core and Bukkit plugin (Maven)
mvn clean package

# Build Fabric mod (Gradle)
./gradlew :fabric:build

# Build Forge mod (Gradle)
./gradlew :forge:build
```

Detailed compilation settings and local Maven installation instructions are available on the **[Building from Source Wiki Page](wiki/Building-from-Source)**.

---

## 📊 bStats Analytics

This plugin uses [bStats](https://bstats.org/plugin/bukkit/Chaos%20Chickens/30945) to collect anonymous usage data to help us track active servers, player counts, and trait statistics.

> [!NOTE]
> **Plugin-Only Metrics:** These metrics are collected for the Bukkit/Spigot/Paper/Folia plugin version only. Fabric and Forge mod versions do not collect bStats data.
> You can opt-out of analytics at any time by setting `bstats-enabled: false` in `config.yml`.

<div align="center">

[![bStats Server Signature](https://bstats.org/signatures/bukkit/Chaos%20Chickens.svg)](https://bstats.org/plugin/bukkit/Chaos%20Chickens/30945)

*Click the signature image above to view the live, interactive charts on the official bStats page.*

</div>

### Collected data includes:
* Active chaos chicken count in the world
* Configured chaos chance percentage
* Folia region safety detection status
* Active trait distribution percentages

---

## 🛡️ License & Community

**Chaos Chickens** is maintained by the **DemonZ Development** community, with lead development by Cyrus.

This project is licensed under the **GNU General Public License v3.0** — see the [LICENSE](LICENSE) file for details.
Contributions are highly welcomed! Feel free to open issues or submit pull requests.
