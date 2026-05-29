# 🔌 Developer API Guide

**Chaos Chickens** features a multi-platform development API, allowing you to easily register custom traits and hook into spawn/death cycles from your own plugins or mods.

---

## 📦 Setting Up the Dependency

To use the API, add the **Common module** as a compile dependency to your build file:

### Maven (`pom.xml`)
```xml
<dependency>
    <groupId>com.chaoschickens</groupId>
    <artifactId>chaoschickens-common</artifactId>
    <version>1.0.1</version>
    <scope>provided</scope>
</dependency>
```

### Gradle (`build.gradle`)
```groovy
dependencies {
    compileOnly 'com.chaoschickens:chaoschickens-common:1.0.1'
}
```

---

## 🎨 Creating a Custom Trait (Bukkit Example)

Extend `BukkitTrait` (or `FabricTrait` / `ForgeTrait`) to build a custom trait:

```java
package com.myplugin.trait;

import com.chaoschickens.bukkit.trait.BukkitTrait;
import com.chaoschickens.common.trait.TraitType;
import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.entity.Chicken;

public class LevitatingTrait extends BukkitTrait {

    public LevitatingTrait() {
        // TraitType key, description, default weight, periodic, proximity, interval
        super(TraitType.custom("levitating", "Floating Chicken"), 
              "Floats into the sky periodically!", 0.8, true, false, 40);
    }

    @Override
    public void onApply(Chicken chicken) {
        chicken.setCustomName(ChatColor.AQUA + "Levitating Chicken");
        chicken.setCustomNameVisible(true);
    }

    @Override
    public void onTick(Chicken chicken) {
        if (chicken == null || chicken.isDead()) return;
        // Make the chicken float up slightly
        chicken.setVelocity(chicken.getVelocity().add(new org.bukkit.util.Vector(0, 0.2, 0)));
        chicken.getWorld().spawnParticle(Particle.CLOUD, chicken.getLocation(), 3, 0.1, 0.1, 0.1, 0.01);
    }
}
```

---

## 📝 Registering Your Trait

To register your custom trait with the **Chaos Chickens API**, do so inside your plugin's `onEnable` or mod's init:

```java
import com.chaoschickens.api.ChaosChickensAPI;

public class MyPlugin extends org.bukkit.plugin.java.JavaPlugin {

    @Override
    public void onEnable() {
        // Register custom trait instance with a base weight of 1.0
        LevitatingTrait myTrait = new LevitatingTrait();
        ChaosChickensAPI.registerTrait(myTrait, 1.0);
        
        getLogger().info("Successfully registered custom Levitating trait with Chaos Chickens!");
    }
}
```

---

## 🔍 Interacting with Chaos Chickens

Use the shared API to check active chicken statuses:

```java
import com.chaoschickens.api.ChaosChickensAPI;
import com.chaoschickens.common.trait.TraitType;
import org.bukkit.entity.Chicken;

public class MyListener implements org.bukkit.event.Listener {

    public void checkChicken(Chicken chicken) {
        // Check if this chicken has any active chaos trait
        boolean isChaos = ChaosChickensAPI.isChaosChicken(chicken.getUniqueId());
        
        if (isChaos) {
            TraitType activeTrait = ChaosChickensAPI.getActiveTrait(chicken.getUniqueId());
            System.out.println("This chicken has active trait: " + activeTrait.getDisplayName());
        }
    }
}
```
