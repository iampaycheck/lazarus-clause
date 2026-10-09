package com.iampaycheck.ghostcore.ghost;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * Per-player Ghost state, stored as a data attachment. All timers are absolute game times
 * (shared by every dimension), so the client can count down without per-tick syncing.
 */
public class GhostData {
    public static final int POCKET_SIZE = 9;

    public static final Codec<GhostData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.BOOL.optionalFieldOf("bound", false).forGetter(d -> d.bound),
            Codec.BOOL.optionalFieldOf("starter_given", false).forGetter(d -> d.starterGiven),
            Codec.INT.optionalFieldOf("charges", 0).forGetter(d -> d.charges),
            Codec.LONG.optionalFieldOf("next_charge_at", 0L).forGetter(d -> d.nextChargeAt),
            Codec.LONG.optionalFieldOf("scan_ready_at", 0L).forGetter(d -> d.scanReadyAt),
            Codec.LONG.optionalFieldOf("transmat_ready_at", 0L).forGetter(d -> d.transmatReadyAt),
            Codec.LONG.optionalFieldOf("downed_until", 0L).forGetter(d -> d.downedUntil),
            GlobalPos.CODEC.optionalFieldOf("beacon").forGetter(d -> d.beacon),
            Codec.BOOL.optionalFieldOf("light", true).forGetter(d -> d.lightOn),
            ItemStack.OPTIONAL_CODEC.listOf().optionalFieldOf("pocket", List.of()).forGetter(d -> d.pocket)
    ).apply(i, GhostData::new));

    public boolean bound;
    public boolean starterGiven;
    public int charges;
    /** Game time the next resurrection charge comes back; 0 when full. */
    public long nextChargeAt;
    public long scanReadyAt;
    public long transmatReadyAt;
    /** Game time the downed player gets back up; 0 when not downed. */
    public long downedUntil;
    public Optional<GlobalPos> beacon = Optional.empty();
    public boolean lightOn = true;
    public final NonNullList<ItemStack> pocket = NonNullList.withSize(POCKET_SIZE, ItemStack.EMPTY);

    // Session-only state, deliberately not serialized.
    /** Game time a transmat channel completes; 0 when not channeling. */
    public long transmatAt;
    @Nullable public Vec3 transmatOrigin;
    public long scanAnimUntil;
    @Nullable public Vec3 lastSafePos;
    @Nullable public ResourceKey<Level> lastSafeDim;

    public GhostData() {}

    private GhostData(boolean bound, boolean starterGiven, int charges, long nextChargeAt, long scanReadyAt,
                      long transmatReadyAt, long downedUntil, Optional<GlobalPos> beacon, boolean lightOn, List<ItemStack> pocket) {
        this.bound = bound;
        this.starterGiven = starterGiven;
        this.charges = charges;
        this.nextChargeAt = nextChargeAt;
        this.scanReadyAt = scanReadyAt;
        this.transmatReadyAt = transmatReadyAt;
        this.downedUntil = downedUntil;
        this.beacon = beacon;
        this.lightOn = lightOn;
        for (int slot = 0; slot < Math.min(pocket.size(), POCKET_SIZE); slot++) {
            this.pocket.set(slot, pocket.get(slot));
        }
    }

    public boolean isDowned() {
        return downedUntil > 0;
    }

    public boolean isChanneling() {
        return transmatAt > 0;
    }
}
