# ⚙️ Configuration Guide

**Chaos Chickens** offers a deeply customizable configuration system. The Bukkit module uses `config.yml`, whereas the Fabric and Forge modules use a JSON-based config system managed via `cloth-config` / config folders.

---

## 📄 Config Options & Details

Here is a full breakdown of every configuration option available:

| Key (YAML) | Key (JSON) | Type | Default | Description |
|---|---|---|---|---|
| `config-version` | `configVersion` | `integer` | `3` | Internal config version. Do not change this. |
| `chaos-chance` | `chaosChance` | `double` | `0.35` | Probability (`0.0 - 1.0`) that a fresh chicken spawn receives a chaos trait. `0.35` = 35% chance. |
| `enable-boss-chickens` | `enableBossChickens` | `boolean` | `true` | If true, extremely rare **Boss Chickens** can spawn. |
| `boss-chance` | `bossChance` | `double` | `0.02` | Independent spawn chance (`0.0 - 1.0`) of any chicken spawn becoming a Boss. `0.02` = 2% chance. |
| `boss-trait-count` | `bossTraitCount` | `integer` | `3` | Number of simultaneous random traits applied to a Boss Chicken. |
| `enable-particles` | `enableParticles` | `boolean` | `true` | Enables active visual particle traits (e.g. fire/magnet aura, disco rings). |
| `enable-messages` | `enableMessages` | `boolean` | `true` | Enables system logging and player chat notices. |
| `announce-trait-on-spawn`| `announceTraitOnSpawn`| `boolean` | `false` | If true, broadcasts a server-wide chat notice when a chaos chicken spawns. |
| `only-natural-spawns` | `onlyNaturalSpawns` | `boolean` | `false` | If true, only natural world spawns (and spawners) receive traits. Breeding, eggs, and commands are ignored. |
| `max-chickens-per-player` | `maxChickensPerPlayer` | `integer` | `-1` | Cap on the number of active chaos chickens near a player. `-1` = unlimited. |
| `check-for-updates` | `checkForUpdates` | `boolean` | `true` | Enables update checking against Modrinth. |
| `enable-folia-support` | `enableFoliaSupport` | `boolean` | `true` | Enables regionized ticking for Folia servers. |
| `bstats-enabled` | `bstatsEnabled` | `boolean` | `true` | Enables anonymous bStats metric reporting. |

---

## 🎨 Trait Specific Configuration

Under the `traits:` section, you can configure individual traits. For example, in Bukkit's `config.yml`:

```yaml
traits:
  explosive:
    enabled: true
    weight: 1.0
  speed:
    enabled: true
    weight: 1.2
  disco:
    enabled: true
    weight: 0.8
```

* **`enabled`** (`boolean`): If false, this trait will never be picked during spawning.
* **`weight`** (`double`): Modifies the relative probability of this trait being selected. A weight of `2.0` makes it twice as likely to spawn compared to a weight of `1.0`.

---

## 📝 Example config.yml (Bukkit)

```yaml
# Chaos Chickens Configuration File
# Managed by DemonZ Development community
config-version: 3

# General Settings
chaos-chance: 0.35
enable-boss-chickens: true
boss-chance: 0.02
boss-trait-count: 3
enable-particles: true
enable-messages: true
announce-trait-on-spawn: false
only-natural-spawns: false
max-chickens-per-player: -1
check-for-updates: true
enable-folia-support: true
bstats-enabled: true

# Traits Weights & Settings
traits:
  explosive:
    enabled: true
    weight: 1.0
  speed:
    enabled: true
    weight: 1.2
  fire:
    enabled: true
    weight: 1.0
  magnet:
    enabled: true
    weight: 0.8
  golden:
    enabled: true
    weight: 0.5
  disco:
    enabled: true
    weight: 0.8
  zombie:
    enabled: true
    weight: 0.7
  teleport:
    enabled: true
    weight: 0.6
  ice:
    enabled: true
    weight: 0.9
  cursed:
    enabled: true
    weight: 0.7
```
