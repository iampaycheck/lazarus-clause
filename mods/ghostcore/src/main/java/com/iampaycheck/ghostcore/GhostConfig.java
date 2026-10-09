package com.iampaycheck.ghostcore;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class GhostConfig {
    public enum StarterMode { ITEM, BIND, NONE }

    public static final ModConfigSpec SERVER_SPEC;
    public static final ModConfigSpec CLIENT_SPEC;

    // Server (per-world, synced to clients)
    public static final ModConfigSpec.EnumValue<StarterMode> STARTER_MODE;
    public static final ModConfigSpec.IntValue MAX_CHARGES;
    public static final ModConfigSpec.IntValue CHARGE_REGEN_SECONDS;
    public static final ModConfigSpec.IntValue REVIVE_DELAY_SECONDS;
    public static final ModConfigSpec.DoubleValue REVIVE_HEALTH_FRACTION;
    public static final ModConfigSpec.IntValue SCAN_COOLDOWN_SECONDS;
    public static final ModConfigSpec.IntValue SCAN_RADIUS;
    public static final ModConfigSpec.IntValue SCAN_ORE_RADIUS;
    public static final ModConfigSpec.IntValue SCAN_HIGHLIGHT_SECONDS;
    public static final ModConfigSpec.IntValue TRANSMAT_COOLDOWN_SECONDS;
    public static final ModConfigSpec.IntValue TRANSMAT_CHANNEL_SECONDS;
    public static final ModConfigSpec.BooleanValue TRANSMAT_CROSS_DIMENSION;
    public static final ModConfigSpec.BooleanValue LIGHT_ENABLED;

    // Client
    public static final ModConfigSpec.BooleanValue HUD_ENABLED;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();

        b.push("general");
        STARTER_MODE = b.comment("How new players get their Ghost.",
                        "ITEM: receive a Ghost Shell on first join. BIND: linked immediately. NONE: quests/commands handle it.")
                .defineEnum("starterMode", StarterMode.ITEM);
        b.pop();

        b.push("resurrection");
        MAX_CHARGES = b.comment("Resurrection charges a Ghost can hold. 0 disables resurrection.")
                .defineInRange("maxCharges", 3, 0, 20);
        CHARGE_REGEN_SECONDS = b.comment("Seconds to regenerate one spent charge.")
                .defineInRange("chargeRegenSeconds", 300, 1, 86400);
        REVIVE_DELAY_SECONDS = b.comment("Seconds spent downed while the Ghost restores you. You are immobile but cannot be hurt.")
                .defineInRange("reviveDelaySeconds", 3, 0, 60);
        REVIVE_HEALTH_FRACTION = b.comment("Fraction of max health restored on revive.")
                .defineInRange("reviveHealthFraction", 0.5, 0.05, 1.0);
        b.pop();

        b.push("scan");
        SCAN_COOLDOWN_SECONDS = b.defineInRange("cooldownSeconds", 15, 0, 3600);
        SCAN_RADIUS = b.comment("Radius for loot caches and hostiles.")
                .defineInRange("radius", 32, 4, 96);
        SCAN_ORE_RADIUS = b.comment("Radius for ore veins (block-by-block, keep it modest). 0 disables ore scanning.")
                .defineInRange("oreRadius", 12, 0, 32);
        SCAN_HIGHLIGHT_SECONDS = b.defineInRange("highlightSeconds", 10, 1, 120);
        b.pop();

        b.push("transmat");
        TRANSMAT_COOLDOWN_SECONDS = b.defineInRange("cooldownSeconds", 300, 0, 86400);
        TRANSMAT_CHANNEL_SECONDS = b.comment("Seconds you must hold still while the Ghost locks on to the beacon.")
                .defineInRange("channelSeconds", 3, 0, 60);
        TRANSMAT_CROSS_DIMENSION = b.comment("Allow transmat to a beacon in another dimension.")
                .define("crossDimension", true);
        b.pop();

        b.push("light");
        LIGHT_ENABLED = b.comment("Allow Ghosts to light dark areas (places a self-cleaning invisible light block).")
                .define("enabled", true);
        b.pop();

        SERVER_SPEC = b.build();

        ModConfigSpec.Builder c = new ModConfigSpec.Builder();
        HUD_ENABLED = c.comment("Show the Ghost status panel in the top-left corner.").define("hudEnabled", true);
        CLIENT_SPEC = c.build();
    }

    public static int ticks(ModConfigSpec.IntValue seconds) {
        return seconds.get() * 20;
    }

    private GhostConfig() {}
}
