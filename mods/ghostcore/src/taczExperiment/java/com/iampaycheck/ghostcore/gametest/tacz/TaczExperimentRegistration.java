package com.iampaycheck.ghostcore.gametest.tacz;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

/** Compiled and loaded only by -PtaczExperiment=true; never included in the mod jar. */
@Mod("ghostcore_tacz_experiment")
@EventBusSubscriber(modid = "ghostcore_tacz_experiment", bus = EventBusSubscriber.Bus.MOD)
public final class TaczExperimentRegistration {
    public TaczExperimentRegistration() {}

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        if (ModList.get().isLoaded("tacz")) event.register(TaczExperimentTests.class);
    }
}
