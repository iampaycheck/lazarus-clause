package com.iampaycheck.ghostcore.gametest.tacz.client;

import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.event.common.GunFireEvent;
import com.tacz.guns.api.event.common.GunShootEvent;
import com.tacz.guns.client.gameplay.LocalPlayerDataHolder;
import com.tacz.guns.client.input.ShootKey;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.ICancellableEvent;

/**
 * Compile-time contract: the pinned port publicly exposes, with these exact types, every member that Ghost Core's
 * TaczCompat and client TaczClientGuard look up by name. Compiled by -PtaczExperiment=true, never loaded or run
 * (the GameTest server is a dedicated dist); the guards' runtime behavior on a real client is the owner's playtest.
 */
final class GuardContract {
    private GuardContract() {}

    static LivingEntity shotEvents(GunShootEvent shoot, GunFireEvent fire) {
        ICancellableEvent cancellableShoot = shoot;
        ICancellableEvent cancellableFire = fire;
        LivingEntity shooter = shoot.getShooter();
        return fire.getShooter();
    }

    static void releaseFireInput(LocalPlayer player) {
        KeyMapping key = ShootKey.SHOOT_KEY;
        key.setDown(false);
        boolean unused = ShootKey.shootControllerTick(false);
        LocalPlayerDataHolder data = ((IClientPlayerGunOperator) player).getDataHolder();
        float charge = data.chargeProgress;
        boolean charging = data.isCharging;
        data.chargeProgress = 0F;
        data.isCharging = false;
    }
}
