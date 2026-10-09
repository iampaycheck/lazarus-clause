package com.iampaycheck.ghostcore.ghost;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

/** A small cache the Ghost keeps in transmat storage. Follows you everywhere, survives death. */
public final class GhostPocket {

    public static void open(ServerPlayer player, GhostData data) {
        if (data.isDowned()) return;
        ItemStack[] contents = data.pocket.stream().map(ItemStack::copy).toArray(ItemStack[]::new);
        Container container = new SimpleContainer(contents) {
            @Override
            public void setChanged() {
                super.setChanged();
                save(this, data);
            }

            @Override
            public void stopOpen(Player p) {
                super.stopOpen(p);
                save(this, data);
            }
        };
        player.openMenu(new SimpleMenuProvider(
                (id, inventory, p) -> new ChestMenu(MenuType.GENERIC_9x1, id, inventory, container, 1),
                Component.translatable("container.ghostcore.pocket")));
    }

    private static void save(Container container, GhostData data) {
        for (int slot = 0; slot < GhostData.POCKET_SIZE; slot++) {
            data.pocket.set(slot, container.getItem(slot).copy());
        }
    }

    private GhostPocket() {}
}
