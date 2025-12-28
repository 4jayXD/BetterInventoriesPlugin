package net.ajay.betterInventories;

import net.ajay.betterInventories.manager.EChestManager;
import net.ajay.betterInventories.manager.ShulkerBoxManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class BetterInventories extends JavaPlugin {

    @Override
    public void onEnable() {
        EChestManager.create(this);
        ShulkerBoxManager.create(this); // shulker functionality disabled. As I dont know how shulkers work :(.. me smol brain.
    }

    @Override
    public void onDisable() {
    }
}
