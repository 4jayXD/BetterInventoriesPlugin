package net.ajay.betterInventories.manager;

import com.destroystokyo.paper.event.player.PlayerConnectionCloseEvent;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.ajay.betterInventories.core.AbstractManager;
import net.ajay.betterInventories.util.OperatorUtils;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class EChestManager extends AbstractManager
{
    private static final LiteralArgumentBuilder<CommandSourceStack> CommandRoot;
    
    private static EChestManager instance;
    
    private EChestManager(JavaPlugin plugin) {
        super(plugin);
        plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            commands.registrar().register(CommandRoot.build());
        });
    }
    
    private static HashSet<UUID> playersInEChest;
    
    private static void OpenEnderChest(Player player) {
        if (playersInEChest.contains(player.getUniqueId()))
            return;
        
        player.sendMessage(Component.text("Opening Ender Chest...", NamedTextColor.LIGHT_PURPLE));
        player.openInventory(player.getEnderChest());
        playersInEChest.add(player.getUniqueId());

        player.playSound(Sound.sound(Key.key("block.chest.open"), Sound.Source.PLAYER, 1, (float)Math.random()));
        
        // play chest open sound here. 

    }
    private static void CloseEnderChest(Player player) {
        if (!playersInEChest.contains(player.getUniqueId()))
            return;

        player.playSound(Sound.sound(Key.key("block.chest.close"), Sound.Source.PLAYER, 1, (float)Math.random()));
        
        playersInEChest.remove(player.getUniqueId());
    }
    
    public static int CommandExecutor(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        if (sender instanceof Player player) {
            
            OpenEnderChest(player);
            return Command.SINGLE_SUCCESS;
        }
        
        return 0;
    }
    public static int OpenPlayerEChest(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        Player victim = Bukkit.getPlayer(ctx.getArgument("player", String.class));
        
        if (sender.isOp() && victim != null && sender instanceof Player playerSender) {
            playerSender.openInventory(victim.getEnderChest());
            return Command.SINGLE_SUCCESS;
        }
        
        return 0;
    }
    public static boolean CanExecuteCommand(CommandSourceStack commandSourceStack) {
        return commandSourceStack.getSender() instanceof Player;
    }
    
    @EventHandler
    public void playerInteraction(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        
        if (playersInEChest.contains(player.getUniqueId()))
            return;
        
        if (event.getAction() == Action.RIGHT_CLICK_AIR && event.getItem().getType() == Material.ENDER_CHEST)
            OpenEnderChest(player);
    }
    
    @EventHandler 
    public void inventoryClosed(InventoryCloseEvent event) {
        HumanEntity entity = event.getPlayer();
        
        if (entity instanceof Player player && playersInEChest.contains(entity.getUniqueId()))
            CloseEnderChest(player);
    }
    
    @EventHandler
    public void inventoryClick(InventoryClickEvent event) {
        ItemStack currentItem = event.getCurrentItem();
        
        if (currentItem == null || currentItem.isEmpty())
            return;
        
        if (event.isRightClick() && event.getCurrentItem().getType() == Material.ENDER_CHEST && event.getWhoClicked() instanceof Player player)
            OpenEnderChest(player);
    }
    
    @EventHandler
    public void playerQuit(PlayerQuitEvent event) {
        if (playersInEChest.contains(event.getPlayer().getUniqueId()))
            playersInEChest.remove(event.getPlayer().getUniqueId());
    }
    
    public static EChestManager create(JavaPlugin plugin) {
        if (instance == null)
            instance = new EChestManager(plugin);
        
        return instance;
    }
    public static EChestManager get() {
        return instance;
    }
    
    static {
        playersInEChest = new HashSet<>();
        
        CommandRoot = Commands.literal("echest").requires(EChestManager::CanExecuteCommand).executes(EChestManager::CommandExecutor);
        CommandRoot.then(Commands.argument("player", StringArgumentType.word()).suggests(OperatorUtils::buildPlayerNames).requires(OperatorUtils::IsOporator).executes(EChestManager::OpenPlayerEChest));
    }
}