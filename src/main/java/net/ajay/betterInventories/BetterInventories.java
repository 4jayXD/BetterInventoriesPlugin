package net.ajay.betterInventories;

import net.ajay.betterInventories.manager.EChestManager;
import net.ajay.betterInventories.manager.ShulkerBoxManager;
import net.ajay.betterInventories.util.OperatorUtils;
import org.bukkit.plugin.java.JavaPlugin;

public final class BetterInventories extends JavaPlugin {

    @Override
    public void onEnable() {
        OperatorUtils.init(this);
        
        EChestManager.create(this);
        ShulkerBoxManager.create(this);
    }
}
