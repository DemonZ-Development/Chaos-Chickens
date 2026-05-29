# 🔨 Building from Source

This guide provides step-by-step instructions on setting up your local environment and compiling **Chaos Chickens** from source code.

---

## 📋 Prerequisites

Before compiling, ensure you have the following installed on your machine:

1. **Java Development Kit (JDK)**:
   * **JDK 17** is required to compile Spigot/Paper compatibility versions.
   * **JDK 21** is required for modern Minecraft 1.20.5+ / 1.21.x mod modules.
2. **Build Tools**:
   * **Maven 3.8+** (for compiling the Bukkit module).
   * **Gradle 8.5+** (managed via gradle wrappers, for Fabric and Forge modules).
3. **Git** (to clone the repository).

---

## 📥 Cloning the Repository

Clone the project locally using git:

```bash
git clone https://github.com/DemonZDevelopment/Chaos-Chickens.git
cd Chaos-Chickens
```

---

## ⚙️ Building the Common & Bukkit Modules (Maven)

The core shared API and Spigot/Paper modules are built together using Maven:

```bash
# Build both common and bukkit modules, running all unit tests
mvn clean package
```

The output artifacts will be located under:
* **Common module**: `common/target/chaoschickens-common-X.Y.Z.jar`
* **Bukkit plugin**: `bukkit/target/chaoschickens-bukkit-X.Y.Z.jar` (This is the shaded, production-ready jar!)

---

## 🧵 Building the Fabric & Forge Modules (Gradle)

Fabric and Forge are built via Gradle. First, build the common dependency, then execute gradle:

```bash
# Build the Fabric mod
./gradlew :fabric:build

# Build the Forge mod
./gradlew :forge:build

# Build both mods simultaneously
./gradlew build
```

The output mod artifacts will be located under:
* **Fabric mod**: `fabric/build/libs/chaoschickens-fabric-X.Y.Z.jar`
* **Forge mod**: `forge/build/libs/chaoschickens-forge-X.Y.Z.jar`

---

## ⚙️ Local Maven Installation

If you are developing custom modules that depend on Chaos Chickens, you can install the common API to your local `.m2` repository:

```bash
mvn clean install
```
