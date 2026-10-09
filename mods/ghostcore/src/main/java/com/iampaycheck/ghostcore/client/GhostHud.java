package com.iampaycheck.ghostcore.client;

import com.iampaycheck.ghostcore.GhostConfig;
import com.iampaycheck.ghostcore.GhostCore;
import com.iampaycheck.ghostcore.network.GhostSyncPayload;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

/** Ghost status panel (top-left), plus full-screen overlays while downed or channeling a transmat. */
public final class GhostHud {
    static final int ACCENT = 0xFF5CE1E6;
    private static final int DIM = 0xFF7F8C99;
    private static final int WARN = 0xFFFFB347;
    private static final int PANEL = 0x99000000;
    private static final String[] ARROWS = {"↑", "↗", "→", "↘", "↓", "↙", "←", "↖"};

    static void register(RegisterGuiLayersEvent event) {
        event.registerAboveAll(GhostCore.id("ghost_hud"), GhostHud::render);
    }

    private static void render(GuiGraphics g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.options.hideGui) return;
        GhostSyncPayload s = ClientPayloads.state();
        if (!s.bound()) return;
        long now = mc.level.getGameTime();
        float partial = delta.getGameTimeDeltaPartialTick(false);

        if (s.downedUntil() > 0) {
            g.fill(0, 0, g.guiWidth(), g.guiHeight(), 0x55300000);
            progressBar(g, mc.font, Component.translatable("ghostcore.hud.reviving"), g.guiHeight() / 2 - 30,
                    1F - (s.downedUntil() - now - partial) / s.reviveTotal());
        }
        if (s.transmatAt() > 0) {
            progressBar(g, mc.font, Component.translatable("ghostcore.hud.transmat"), g.guiHeight() / 2 + 24,
                    1F - (s.transmatAt() - now - partial) / s.transmatTotal());
        }
        if (GhostConfig.HUD_ENABLED.get() && !mc.getDebugOverlay().showDebugScreen()) {
            panel(g, mc.font, mc.player, s, now);
        }
    }

    private static void panel(GuiGraphics g, Font font, LocalPlayer player, GhostSyncPayload s, long now) {
        int x = 6, y = 6, width = 132, line = 10, rows = 5;
        g.fill(x - 4, y - 4, x + width, y + rows * line, PANEL);
        g.fill(x - 4, y - 4, x - 3, y + rows * line, ACCENT);

        g.drawString(font, Component.translatable("ghostcore.hud.title"), x, y, ACCENT, false);
        Component status = Component.translatable(s.downedUntil() > 0 ? "ghostcore.hud.status.reviving" : "ghostcore.hud.status.linked");
        g.drawString(font, status, x + width - 6 - font.width(status), y, s.downedUntil() > 0 ? WARN : DIM, false);

        y += line;
        label(g, font, "ghostcore.hud.rez", x, y);
        StringBuilder pips = new StringBuilder();
        for (int i = 0; i < s.maxCharges(); i++) pips.append(i < s.charges() ? '■' : '□');
        int pipX = x + 34;
        g.drawString(font, pips.toString(), pipX, y, s.charges() > 0 ? ACCENT : WARN, false);
        if (s.charges() < s.maxCharges() && s.nextChargeAt() > 0) {
            g.drawString(font, "+1 " + time(s.nextChargeAt() - now), pipX + font.width(pips.toString()) + 6, y, DIM, false);
        }

        y += line;
        label(g, font, "ghostcore.hud.scan", x, y);
        readiness(g, font, x + 34, y, s.scanReadyAt() - now);

        y += line;
        label(g, font, "ghostcore.hud.transmat_short", x, y);
        int afterReady = readiness(g, font, x + 34, y, s.transmatReadyAt() - now);
        g.drawString(font, beaconText(player, s), afterReady + 6, y, DIM, false);

        y += line;
        label(g, font, "ghostcore.hud.light", x, y);
        g.drawString(font, Component.translatable(s.lightOn() ? "ghostcore.hud.light.auto" : "ghostcore.hud.light.off"),
                x + 34, y, s.lightOn() ? ACCENT : DIM, false);
    }

    private static void label(GuiGraphics g, Font font, String key, int x, int y) {
        g.drawString(font, Component.translatable(key), x, y, DIM, false);
    }

    /** Draws READY or a countdown; returns the x just past the text. */
    private static int readiness(GuiGraphics g, Font font, int x, int y, long ticksLeft) {
        boolean ready = ticksLeft <= 0;
        Component text = ready ? Component.translatable("ghostcore.hud.ready") : Component.literal(time(ticksLeft));
        g.drawString(font, text, x, y, ready ? ACCENT : WARN, false);
        return x + font.width(text);
    }

    private static Component beaconText(LocalPlayer player, GhostSyncPayload s) {
        if (s.beacon().isEmpty()) return Component.translatable("ghostcore.hud.no_beacon");
        GlobalPos beacon = s.beacon().get();
        if (beacon.dimension() != player.level().dimension()) return Component.translatable("ghostcore.hud.off_world");
        double dx = beacon.pos().getX() + 0.5 - player.getX();
        double dz = beacon.pos().getZ() + 0.5 - player.getZ();
        float bearing = (float) (Mth.atan2(-dx, dz) * Mth.RAD_TO_DEG);
        int arrow = Math.round(Mth.wrapDegrees(bearing - player.getYRot()) / 45F) & 7;
        return Component.literal(Math.round(Math.sqrt(dx * dx + dz * dz)) + "m " + ARROWS[arrow]);
    }

    private static void progressBar(GuiGraphics g, Font font, Component title, int y, float progress) {
        int barWidth = 120;
        int x = g.guiWidth() / 2 - barWidth / 2;
        g.drawCenteredString(font, title, g.guiWidth() / 2, y, ACCENT);
        g.fill(x - 1, y + 11, x + barWidth + 1, y + 16, 0xCC000000);
        g.fill(x, y + 12, x + (int) (barWidth * Mth.clamp(progress, 0F, 1F)), y + 15, ACCENT);
    }

    private static String time(long ticks) {
        long seconds = Math.max(0, (ticks + 19) / 20);
        return seconds >= 60 ? String.format("%d:%02d", seconds / 60, seconds % 60) : seconds + "s";
    }

    private GhostHud() {}
}
