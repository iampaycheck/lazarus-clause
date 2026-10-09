package com.iampaycheck.ghostcore.item;

import com.iampaycheck.ghostcore.ghost.GhostManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** A dormant Ghost. Use it to bring the Ghost online and bind it to you. */
public class GhostShellItem extends Item {
    public GhostShellItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer) {
            if (GhostManager.bind(serverPlayer)) {
                stack.consume(1, player);
            } else {
                GhostManager.notify(player, "ghostcore.message.already_bound");
                return InteractionResultHolder.fail(stack);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.ghostcore.ghost_shell.desc").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.ghostcore.ghost_shell.lore").withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.ITALIC));
    }
}
