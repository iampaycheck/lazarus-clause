package com.iampaycheck.ghostcore.command;

import com.iampaycheck.ghostcore.GhostConfig;
import com.iampaycheck.ghostcore.GhostCore;
import com.iampaycheck.ghostcore.ghost.GhostData;
import com.iampaycheck.ghostcore.ghost.GhostManager;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

/** /ghost bind|unbind|recharge [targets] — for testing and for quest/modpack scripting. */
@EventBusSubscriber(modid = GhostCore.MODID)
public final class GhostCommand {

    @SubscribeEvent
    static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("ghost")
                .requires(source -> source.hasPermission(2))
                .then(action("bind", player -> GhostManager.bind(player)))
                .then(action("unbind", GhostManager::unbind))
                .then(action("recharge", player -> {
                    GhostData data = GhostManager.data(player);
                    data.charges = GhostConfig.MAX_CHARGES.get();
                    data.nextChargeAt = 0;
                    data.scanReadyAt = 0;
                    data.transmatReadyAt = 0;
                })));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> action(String name, Consumer<ServerPlayer> effect) {
        return Commands.literal(name)
                .executes(ctx -> run(ctx, name, List.of(ctx.getSource().getPlayerOrException()), effect))
                .then(Commands.argument("targets", EntityArgument.players())
                        .executes(ctx -> run(ctx, name, EntityArgument.getPlayers(ctx, "targets"), effect)));
    }

    private static int run(CommandContext<CommandSourceStack> ctx, String name, Collection<ServerPlayer> targets,
                           Consumer<ServerPlayer> effect) throws CommandSyntaxException {
        targets.forEach(effect);
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.ghostcore." + name, targets.size()), true);
        return targets.isEmpty() ? 0 : Command.SINGLE_SUCCESS * targets.size();
    }

    private GhostCommand() {}
}
