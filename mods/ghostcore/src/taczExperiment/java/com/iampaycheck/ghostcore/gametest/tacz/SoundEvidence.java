package com.iampaycheck.ghostcore.gametest.tacz;

import net.minecraft.world.entity.LivingEntity;

import java.util.function.BiConsumer;

/** Server-thread-only observation hook; installed briefly by E4 and never cancels sound. */
public final class SoundEvidence {
    static BiConsumer<LivingEntity, String> observer;

    private SoundEvidence() {}

    public static void record(LivingEntity source, String sound) {
        if (observer != null) observer.accept(source, sound);
    }
}
