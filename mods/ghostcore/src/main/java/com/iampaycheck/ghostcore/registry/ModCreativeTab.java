package com.iampaycheck.ghostcore.registry;

import com.iampaycheck.ghostcore.GhostCore;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTab {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, GhostCore.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.ghostcore"))
            .icon(() -> ModItems.GHOST_SHELL.get().getDefaultInstance())
            .displayItems((params, output) -> {
                output.accept(ModItems.GHOST_SHELL.get());
                output.accept(ModItems.TRANSMAT_BEACON.get());
            })
            .build());

    private ModCreativeTab() {}
}
