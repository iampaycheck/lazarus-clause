package com.iampaycheck.ghostcore.compat;

import com.iampaycheck.ghostcore.GhostCore;
import com.iampaycheck.ghostcore.ghost.GhostManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.function.Predicate;

/**
 * Optional TaCZ support (the tacz-1.21.1 port): a downed operator can't fire. The port's cancellable
 * {@code GunShootEvent} comes once per trigger pull, before the shot is broadcast or any gun logic runs;
 * {@code GunFireEvent} comes on every burst cycle, before ammo is spent, bullets spawn or sound plays.
 * Both are looked up by name only when the port is loaded, so Ghost Core never needs TaCZ to build or run.
 */
public final class TaczCompat {
    public static final String MODID = "tacz";
    private static final String[] FIRE_EVENTS = {
            "com.tacz.guns.api.event.common.GunShootEvent",
            "com.tacz.guns.api.event.common.GunFireEvent"
    };

    /** The authoritative check: the server refuses every shot from a downed player, whatever the client sends. */
    public static void register() {
        guard("server", shooter -> shooter instanceof ServerPlayer player && GhostManager.data(player).isDowned());
    }

    /** Cancels both fire events whenever {@code downed} matches the shooter. Does nothing without the port. */
    public static void guard(String side, Predicate<LivingEntity> downed) {
        if (!ModList.get().isLoaded(MODID)) return;
        for (String name : FIRE_EVENTS) {
            try {
                Class<? extends Event> type = Class.forName(name).asSubclass(Event.class);
                if (!ICancellableEvent.class.isAssignableFrom(type)) throw new ClassCastException(name + " is not cancellable");
                MethodHandle shooter = MethodHandles.publicLookup()
                        .findVirtual(type, "getShooter", MethodType.methodType(LivingEntity.class))
                        .asType(MethodType.methodType(LivingEntity.class, Event.class));
                NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, false, type, event -> {
                    if (downed.test(shooterOf(shooter, event))) ((ICancellableEvent) event).setCanceled(true);
                });
            } catch (ReflectiveOperationException | ClassCastException e) {
                GhostCore.LOGGER.error("TaCZ is installed but {} is missing or changed; downed players may be able to fire", name, e);
                return;
            }
        }
        GhostCore.LOGGER.info("TaCZ found: downed operators can't fire ({} guard)", side);
    }

    private static LivingEntity shooterOf(MethodHandle getter, Event event) {
        try {
            return (LivingEntity) getter.invokeExact(event);
        } catch (Throwable e) {
            throw new IllegalStateException("TaCZ getShooter failed", e);
        }
    }

    private TaczCompat() {}
}
