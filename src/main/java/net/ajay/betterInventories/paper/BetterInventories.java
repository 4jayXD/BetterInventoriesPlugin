package net.ajay.betterInventories.paper;

import net.ajay.betterInventories.paper.manager.echest.EChestManager;
import net.ajay.betterInventories.paper.manager.shulkerbox.ShulkerBoxManager;
import net.ajay.betterInventories.paper.util.OperatorUtils;
import org.bukkit.plugin.java.JavaPlugin;

public final class BetterInventories extends JavaPlugin 
{

    @Override
    public void onEnable() {
        OperatorUtils.init(this);
        
        EChestManager.create(this);
        ShulkerBoxManager.create(this);
    }
}