package net.ajay.betterInventories.bukkit.core;

import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public abstract class AbstractManager implements Listener
{
    protected final JavaPlugin plugin;
    
    protected AbstractManager(JavaPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
}