package com.iampaycheck.ghostcore.gametest.tacz.mixin;

import com.iampaycheck.ghostcore.gametest.tacz.SoundEvidence;

import com.tacz.guns.sound.SoundManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Observe the real server sound dispatch without changing the port's behavior. */
@Mixin(value = SoundManager.class, remap = false)
public abstract class SoundEvidenceMixin {
    @Inject(method = "sendSoundToNearby", at = @At("HEAD"))
    private static void observe(LivingEntity source, int distance, ResourceLocation gun,
                                ResourceLocation display, String sound, float volume, float pitch, CallbackInfo ci) {
        SoundEvidence.record(source, sound);
    }
}
