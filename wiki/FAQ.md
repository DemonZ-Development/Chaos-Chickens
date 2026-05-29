# ❓ Frequently Asked Questions

Find quick answers to common questions and troubleshooting inquiries about **Chaos Chickens**.

---

### Q: Is this mod safe to run on production servers?
**A**: Absolutely! Chaos Chickens is designed with extreme performance in mind. Periodic ticking is throttled to run every 10 ticks, and regional multithreading is fully supported on Folia. Furthermore, the **Explosive** trait does not destroy any blocks, ensuring your player bases remain perfectly safe.

---

### Q: Can I breed chaos chickens to pass down their traits?
**A**: By default, no. Spawning a new chicken via breeding is treated as a normal chicken spawn and has a standard 35% chance to roll a random trait from the weighted trait pool. They do not automatically inherit parent traits unless you install a custom breeding listener plugin that uses our Developer API.

---

### Q: How do I disable a specific trait that I don't want?
**A**: You can easily disable any trait inside the config file under the `traits:` section by setting `enabled: false`. For example, to disable explosive chickens:

```yaml
traits:
  explosive:
    enabled: false
```

---

### Q: Can I run this plugin on Spigot, Paper, and Purpur?
**A**: Yes! The Bukkit module is 100% compatible with Spigot, Paper, Purpur, Folia, and any other downstream forks.

---

### Q: Does this mod work on single-player clients?
**A**: Yes! The Fabric and Forge versions are fully operational on single-player worlds, local LAN servers, and dedicated multiplayer servers.

---

### Q: How do I obtain a chaos egg to spawn a specific chicken?
**A**: If you are an administrator, you can run `/chaoschickens egg <trait>` to give yourself a customized spawn egg that is permanently bound to that specific trait.

---

### Q: Is this mod required on the client side for multiplayer servers?
**A**: No! Chaos Chickens is designed as a **server-side mod/plugin**. If you run the Fabric, Forge, or Bukkit version on your server, players can join using a completely vanilla Minecraft client. They do not need to install the mod locally because all traits, sound effects, particle animations, and behaviors are handled server-side and transmitted via standard Minecraft network packets. (Note: For singleplayer/local LAN worlds, the mod must be installed in the client's `mods` folder.)
