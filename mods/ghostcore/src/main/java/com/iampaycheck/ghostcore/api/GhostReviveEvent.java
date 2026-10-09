package com.iampaycheck.ghostcore.api;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Fired on the NeoForge event bus when a Ghost is about to resurrect its player, before a charge is spent.
 * Cancel it to let the player die normally — e.g. inside a raid, a "dark zone", or a hardcore dimension.
 * KubeJS can listen via {@code NativeEvents.onEvent}.
 */
public class GhostReviveEvent extends PlayerEvent implements ICancellableEvent {
    private final DamageSource source;

    public GhostReviveEvent(Player player, DamageSource source) {
        super(player);
        this.source = source;
    }

    public DamageSource getSource() {
        return source;
    }
}
