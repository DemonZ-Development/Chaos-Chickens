# 🖥️ Commands & Permissions Reference

This page contains a complete reference sheet for all command arguments, usage instructions, and permission nodes within **Chaos Chickens**.

---

## 💻 Commands

All actions are structured under the main command `/chaoschickens` (aliases: `/chaoschicken`, `/cc`).

| Command Syntax | Description | Permission Node |
|---|---|---|
| `/chaoschickens` | Opens the interactive help menu. | `chaoschickens.use` (default) |
| `/chaoschickens spawn <trait>` | Spawns a chaos chicken with the specified trait at your feet. | `chaoschickens.spawn` (admin) |
| `/chaoschickens spawn boss` | Spawns a fully powered **Boss Chicken** at your feet. | `chaoschickens.spawn` (admin) |
| `/chaoschickens egg <trait>` | Gives the player a custom chaos chicken spawn egg bound to a trait. | `chaoschickens.egg` (admin) |
| `/chaoschickens reload` | Reloads the configuration file and migrates settings. | `chaoschickens.admin` (admin) |
| `/chaoschickens status` | Displays the current mod status, version, and tracked counts. | `chaoschickens.use` (default) |

---

## 🔑 Permission Nodes

| Permission Node | Description | Default |
|---|---|---|
| `chaoschickens.use` | Grants access to `/cc` and `/cc status`. | `true` (all players) |
| `chaoschickens.spawn` | Grants access to `/cc spawn` and spawning traits. | `op` (operators only) |
| `chaoschickens.egg` | Grants access to `/cc egg` and obtaining custom eggs. | `op` (operators only) |
| `chaoschickens.admin` | Grants full access, including reloading the plugin config. | `op` (operators only) |

---

## 💡 Command Examples

* **Spawn a Speed Chicken**:
  `/chaoschickens spawn speed`
* **Spawn a Cursed Chicken**:
  `/chaoschickens spawn cursed`
* **Get an Explosive Egg**:
  `/chaoschickens egg explosive`
* **Reload Config**:
  `/chaoschickens reload`
