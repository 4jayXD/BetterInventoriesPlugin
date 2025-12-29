package net.ajay.betterInventories.paper.util;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.CompletableFuture;

public final class OperatorUtils 
{
    public static final LiteralArgumentBuilder<CommandSourceStack> PeekRoot;
    
    public static CompletableFuture<Suggestions> buildPlayerNames(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        
        if (ctx.getSource().getSender().isOp()) {
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                builder.suggest(onlinePlayer.getName());
            }
        }
        
        return builder.buildFuture();
    }
    
    public static boolean IsOporator(CommandSourceStack commandSourceStack) {
        CommandSender sender = commandSourceStack.getSender();
        return sender instanceof Player && sender.isOp();
    }
    
    private static int PeekExecutor(CommandContext<CommandSourceStack> ctx) {
        if (IsOporator(ctx.getSource())) {
            CommandSender sender = ctx.getSource().getSender();
            Player player = Bukkit.getPlayer(ctx.getArgument("player", String.class));
            if (player != null && sender instanceof Player playerSender) {
                playerSender.openInventory(player.getInventory());
            }
            
            return Command.SINGLE_SUCCESS;
        }
        
        return 0;
    }
    
    public static void init(JavaPlugin plugin) {
        plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            commands.registrar().register(PeekRoot.build());
        });
    }
    
    static {
        PeekRoot = Commands.literal("peek").requires(OperatorUtils::IsOporator);
        PeekRoot.then(Commands.argument("player", StringArgumentType.word()).requires(OperatorUtils::IsOporator).suggests(OperatorUtils::buildPlayerNames).executes(OperatorUtils::PeekExecutor));
    }
}