# 🧬 Traits Encyclopedia

This is a comprehensive encyclopedia of all **11 active chaos traits** in Chaos Chickens. Learn what makes each chicken unique, what dangers they present, and what rewards they drop.

---

## 📊 Summary Table

| Icon | Trait Name | Custom Name Color | Hostility | Proximity Range | Tick Interval | Description |
|:---:|---|---|---|---|---|---|
| 💥 | **[Explosive](Traits#explosive)** | 🔴 Red | ⚡ High | N/A | Death | Triggers a custom explosion (visual/damage, block safe) on death. |
| ⚡ | **[Speed](Traits#speed)** | 🟡 Yellow | ⚪ Passive | N/A | N/A | Moves at **3x default speed** (`0.75`). |
| 🔥 | **[Fire](Traits#fire)** | 🔴 Orange | 🟡 Medium | N/A | 10 ticks | Sets blocks below it on fire, immune to fire/lava damage. |
| 🧲 | **[Magnet](Traits#magnet)** | 🟣 Purple | ⚪ Passive | 6.0 blocks | 10 ticks | Attracts dropped items nearby toward the chicken's position. |
| 🪙 | **[Golden](Traits#golden)** | 🟡 Gold | ⚪ Passive | N/A | Death | Drops rich gold nuggets, gold/iron ingots, emeralds, or diamonds on death. Glows. |
| 🪩 | **[Disco](Traits#disco)** | 💖 Light Purple | ⚪ Passive | N/A | 10 ticks | Spawns rainbow particle rings and plays music notes continuously. |
| 🧟 | **[Zombie](Traits#zombie)** | 🟢 Dark Green | 🔴 Hostile | 8.0 blocks | 20 ticks | Spawns zombie reinforcement reinforcements when attacked or nearby. |
| 🌀 | **[Teleport](Traits#teleport)** | 🔵 Dark Blue | ⚪ Passive | 5.0 blocks | 20 ticks | Teleports away randomly when approached or damaged. |
| ❄️ | **[Ice](Traits#ice)** | 🔵 Aqua | ⚪ Passive | N/A | 10 ticks | Freezes water into frosted ice and places snow blocks beneath it. |
| 💀 | **[Cursed](Traits#cursed)** | 🟣 Dark Purple | 🔴 Hostile | 6.0 blocks | 20 ticks | Applies Blindness, Slowness, or Poison to players who wander too close. |
| 👑 | **[Boss](Traits#boss)** | 🔴 Bold Dark Red | ⚡ High | 16.0 blocks | 10 ticks | **A combined power chicken** with multiple traits active at once, 3x max health, and a weakness aura! |

---

## 💥 Explosive

* **Key**: `explosive`
* **Default Weight**: `1.0`
* **Details**: When killed, this chicken immediately detonates. The explosion power is standard (`2.0f`). It will damage nearby entities (including players) but **will not destroy blocks**, making it perfectly safe for home builds while keeping fights exciting.

---

## ⚡ Speed

* **Key**: `speed`
* **Default Weight**: `1.2`
* **Details**: Zoom! This chicken moves at a blistering **3x movement speed multiplier** (`0.75` base speed attribute). It can easily outrun standard mobs and is incredibly hard to capture!

---

## 🔥 Fire

* **Key**: `fire`
* **Default Weight**: `1.0`
* **Details**: Imbued with nether flames, this chicken is completely immune to fire and lava. As it walks, it has a 10% chance per tick to ignite the block underneath it (if air/passable), leaving a trail of fire. Spawns flame particles.

---

## 🧲 Magnet

* **Key**: `magnet`
* **Default Weight**: `0.8`
* **Details**: Acts as a walking item hopper! Within a **6-block radius**, any dropped items on the ground will be slowly pulled toward the chicken. Excellent for vacuuming up stray drops in mob farms!

---

## 🪙 Golden

* **Key**: `golden`
* **Default Weight**: `0.5`
* **Details**: Highly sought after! Golden chickens glow brightly (`setGlowing(true)`) and have a 100% chance to drop precious items on death instead of feathers and raw chicken:
  * **40%**: Gold Nuggets (2-5)
  * **25%**: Gold Ingot (1)
  * **20%**: Iron Ingot (1)
  * **10%**: Emerald (1)
  * **5%**: Diamond (1)

---

## 🪩 Disco

* **Key**: `disco`
* **Default Weight**: `0.8`
* **Details**: A party animal! The Disco Chicken plays colorful note particles and rainbow dust rings around its feet. Plays gentle chimes (`NOTE_BLOCK_BELL`) periodically as it dances around.

---

## 🧟 Zombie

* **Key**: `zombie`
* **Default Weight**: `0.7`
* **Details**: Cursed with undead traits, this chicken is highly defensive. When attacked by a player, or when a player gets within **8 blocks**, there is a chance it will summon custom zombie reinforcements to protect it.

---

## 🌀 Teleport

* **Key**: `teleport`
* **Default Weight**: `0.6`
* **Details**: Possesses ender traits. If a player approaches within **5 blocks**, or when the chicken takes any form of damage, it will instantly teleport to a safe location within a 10-block radius, leaving portal particles in its wake.

---

## ❄️ Ice

* **Key**: `ice`
* **Default Weight**: `0.9`
* **Details**: A walking frost walker! Converts water blocks below it into Frosted Ice (which melts after a short duration) and places snow layers on solid blocks. Immune to freezing.

---

## 💀 Cursed

* **Key**: `cursed`
* **Default Weight**: `0.7`
* **Details**: Possesses a dark, malicious aura. When a player steps within **6 blocks**, the chicken will inflict random negative potion effects:
  * **Blindness** (3 seconds)
  * **Slowness** (4 seconds)
  * **Poison I** (5 seconds)

---

## 👑 Boss Chicken

* **Key**: `boss`
* **Default Weight**: `0.02` (independent roll)
* **Details**: The ultimate challenge! The **Boss Chicken** boasts:
  * **3x Maximum Health** (12 HP instead of 4 HP)
  * **Weakness Aura**: Automatically inflicts Weakness on any player within **16 blocks**.
  * **Dragon Breath Particles**: Emits dynamic purple magic particles.
  * **Super Explosion**: Triggers a huge explosion on death and dragon breath shockwaves.
  * **Multi-Traits**: Combines **multiple active traits** from the registry!
