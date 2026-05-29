# 💾 Installation Guide

Installing **Chaos Chickens** is simple and fast. This page guides you through the setup process for all supported platforms.

---

## 📋 Compatibility Matrix

Ensure your server/client matches the requirements:

| Platform | Supported Minecraft Versions | Requirements | Shading / Dependencies |
|---|---|---|---|
| **Bukkit / Spigot / Paper / Folia** | `1.20 - 1.21.x` | Java 17+ (Java 21+ for 1.20.5+) | None (bStats shaded inside) |
| **Fabric** | `1.20.4 - 1.21.x` | Fabric Loader `0.15.x+`, Java 17/21 | [Cloth Config API](https://modrinth.com/mod/cloth-config) (bundled) |
| **Forge** | `1.20.4 - 1.21.x` | Forge Loader `47.x+` or `52.x+`, Java 17/21 | None (common core shaded inside) |

---

## 🔌 Bukkit / Spigot / Paper / Folia Setup

1. **Download the plugin JAR** (`chaoschickens-bukkit-X.Y.Z.jar`).
2. **Move the JAR** to your server's `plugins/` directory.
3. **Restart the server**. Do NOT hot-reload using `/reload` as this can lead to memory leaks in some environments; a fresh boot is recommended.
4. **Verify the installation** by running `/chaoschickens status` in the console.

---

## 🧵 Fabric Setup

1. **Ensure you have Fabric Loader installed** for your target Minecraft version.
2. **Download the mod JAR** (`chaoschickens-fabric-X.Y.Z.jar`).
3. **Place the JAR** inside your local `mods/` directory.
4. **Launch the game/server**. The required Cloth Config library is already bundled inside, so no separate installation is needed!
5. **Verify** by opening the Cloth Config mod options menu or typing `/chaoschickens` in single-player or server chat.

---

## 🛠️ Forge Setup

1. **Ensure you have Minecraft Forge installed** for your target Minecraft version.
2. **Download the mod JAR** (`chaoschickens-forge-X.Y.Z.jar`).
3. **Place the JAR** inside your local `mods/` directory.
4. **Boot the client/server**. The common trait module is already bundled inside, so no extra steps are required.
5. **Verify** by ensuring the mod shows up in your Minecraft Mods list.

---

## 🚨 Troubleshooting

### "Unsupported class version error"
Your server is running an older Java version. Java 17 is required for 1.20 - 1.20.4. If you are running Minecraft 1.20.5 or 1.21+, you **must** use Java 21.

### "Folia global region scheduler failed"
This is a standard warning indicating the server is NOT running Folia and has gracefully fallen back to Spigot's standard scheduler. It is safe to ignore.

### "Cloth Config was not found" (Fabric)
Chaos Chickens Fabric bundles Cloth Config inside. If you encounter issues, ensure you are not running conflicting mods that modify the loader's jar-in-jar classpaths.
