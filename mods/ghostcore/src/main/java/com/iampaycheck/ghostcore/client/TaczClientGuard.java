package com.iampaycheck.ghostcore.client;

import com.iampaycheck.ghostcore.GhostCore;
import com.iampaycheck.ghostcore.compat.TaczCompat;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * Client half of the TaCZ guard. While the server says you're downed, TaCZ's fire input is held released:
 * its input loop ({@code ShootKey.autoShoot}, on {@code ClientTickEvent.Post}) then never charges, shoots,
 * dry-fires or bolts, so no recoil, sound or fire packet. A trigger held through the downed state must be
 * pressed again after revive. The shot events are cancelled too, as a backstop for any other fire path.
 */
final class TaczClientGuard {
    @Nullable private static FireInput input;
    private static boolean resolved;

    static void register() {
        if (!ModList.get().isLoaded(TaczCompat.MODID)) return;
        TaczCompat.guard("client", TaczClientGuard::isLocalPlayerDowned);
        NeoForge.EVENT_BUS.addListener(TaczClientGuard::onClientTick);
    }

    private static boolean isLocalPlayerDowned(LivingEntity shooter) {
        return shooter.level().isClientSide() && shooter == Minecraft.getInstance().player && ClientPayloads.state().isDowned();
    }

    // Pre runs before TaCZ reads its input in Post, and no input callbacks land in between.
    private static void onClientTick(ClientTickEvent.Pre event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !ClientPayloads.state().isDowned()) return;
        if (!resolved) {
            resolved = true; // looked up on the first downed tick, once TaCZ has registered its keys
            input = FireInput.resolve();
        }
        if (input != null) input.release(player);
    }

    /** TaCZ's client fire input, all public in the port: the fire key, the controller trigger flag and the charge. */
    private record FireInput(KeyMapping key, MethodHandle controllerTrigger, MethodHandle dataHolder,
                             MethodHandle setCharge, MethodHandle setCharging) {
        @Nullable
        static FireInput resolve() {
            try {
                MethodHandles.Lookup lookup = MethodHandles.publicLookup();
                Class<?> shootKey = Class.forName("com.tacz.guns.client.input.ShootKey");
                Class<?> operator = Class.forName("com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator");
                Class<?> data = Class.forName("com.tacz.guns.client.gameplay.LocalPlayerDataHolder");
                return new FireInput(
                        (KeyMapping) lookup.findStaticGetter(shootKey, "SHOOT_KEY", KeyMapping.class).invoke(),
                        lookup.findStatic(shootKey, "shootControllerTick", MethodType.methodType(boolean.class, boolean.class)),
                        lookup.findVirtual(operator, "getDataHolder", MethodType.methodType(data))
                                .asType(MethodType.methodType(Object.class, LocalPlayer.class)),
                        lookup.findSetter(data, "chargeProgress", float.class)
                                .asType(MethodType.methodType(void.class, Object.class, float.class)),
                        lookup.findSetter(data, "isCharging", boolean.class)
                                .asType(MethodType.methodType(void.class, Object.class, boolean.class)));
            } catch (Throwable e) {
                GhostCore.LOGGER.error("TaCZ fire input not found; a downed player's trigger may still click, bolt or charge", e);
                return null;
            }
        }

        /** Trigger up (key and controller) and charge emptied, so no queued hold/delay charge fires on revive. */
        void release(LocalPlayer player) {
            key.setDown(false);
            try {
                boolean unused = (boolean) controllerTrigger.invokeExact(false);
                Object holder = dataHolder.invokeExact(player);
                setCharge.invokeExact(holder, 0F);
                setCharging.invokeExact(holder, false);
            } catch (Throwable e) {
                throw new IllegalStateException("TaCZ fire input reset failed", e);
            }
        }
    }

    private TaczClientGuard() {}
}
