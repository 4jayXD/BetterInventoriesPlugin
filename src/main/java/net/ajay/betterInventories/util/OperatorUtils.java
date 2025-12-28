package net.ajay.betterInventories.util;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;

public final class OperatorUtils 
{
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
    
}