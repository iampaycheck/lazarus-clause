package com.iampaycheck.ghostcore.client;

import com.iampaycheck.ghostcore.network.ScanResultPayload;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4fStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders scan results as an x-ray: a sonar ring sweeps outward from where you scanned, revealing
 * boxes around unlooted caches (gold), ore (cyan) and hostiles (red) as it passes them, then fades.
 */
public final class ScanHighlights {
    private static final int LOOT = 0xFFC94A;
    private static final int ORE = 0x4AE3FF;
    private static final int HOSTILE = 0xFF4A4A;
    private static final double WAVE_BLOCKS_PER_TICK = 2.0;
    private static final int FADE_TICKS = 30;

    private record Mark(AABB box, int color, double distance) {}

    private static final List<Mark> marks = new ArrayList<>();
    private static final List<Integer> hostiles = new ArrayList<>();
    private static Vec3 origin;
    private static long startTime;
    private static int duration;

    static void start(ScanResultPayload payload) {
        clear();
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        origin = Vec3.atCenterOf(payload.origin());
        startTime = mc.level.getGameTime();
        duration = payload.durationTicks();
        payload.loot().forEach(pos -> mark(pos, LOOT));
        payload.ores().forEach(pos -> mark(pos, ORE));
        hostiles.addAll(payload.hostiles());
    }

    private static void mark(BlockPos pos, int color) {
        marks.add(new Mark(new AABB(pos).inflate(0.002), color, Math.sqrt(pos.distToCenterSqr(origin))));
    }

    static void clear() {
        marks.clear();
        hostiles.clear();
        origin = null;
    }

    static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL || origin == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        double elapsed = mc.level.getGameTime() - startTime + partial;
        if (elapsed > duration) {
            clear();
            return;
        }
        double wave = elapsed * WAVE_BLOCKS_PER_TICK;
        float alpha = (float) Mth.clamp((duration - elapsed) / FADE_TICKS, 0, 1);

        Vec3 cam = event.getCamera().getPosition();
        PoseStack poseStack = new PoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer lines = buffers.getBuffer(GhostRenderTypes.SCAN_LINES);

        for (Mark mark : marks) {
            if (mark.distance() <= wave) box(poseStack, lines, mark.box().move(cam.reverse()), mark.color(), alpha);
        }
        for (int id : hostiles) {
            Entity entity = mc.level.getEntity(id);
            if (entity == null || !entity.isAlive() || entity.distanceToSqr(origin) > wave * wave) continue;
            Vec3 offset = entity.getPosition(partial).subtract(entity.position());
            box(poseStack, lines, entity.getBoundingBox().move(offset.subtract(cam)).inflate(0.05), HOSTILE, alpha);
        }
        if (elapsed < 25) {
            ring(poseStack, lines, origin.subtract(cam).subtract(0, 0.45, 0), wave, (float) (1 - elapsed / 25.0));
        }

        // Draw with this frame's camera rotation regardless of what the stage left on the matrix stack.
        Matrix4fStack modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        modelView.set(event.getModelViewMatrix());
        RenderSystem.applyModelViewMatrix();
        buffers.endBatch(GhostRenderTypes.SCAN_LINES);
        modelView.popMatrix();
        RenderSystem.applyModelViewMatrix();
    }

    private static void box(PoseStack poseStack, VertexConsumer lines, AABB box, int rgb, float alpha) {
        LevelRenderer.renderLineBox(poseStack, lines, box,
                (rgb >> 16 & 255) / 255F, (rgb >> 8 & 255) / 255F, (rgb & 255) / 255F, alpha * 0.9F);
    }

    private static void ring(PoseStack poseStack, VertexConsumer lines, Vec3 center, double radius, float alpha) {
        PoseStack.Pose pose = poseStack.last();
        int segments = 72;
        for (int i = 0; i < segments; i++) {
            double a0 = Math.PI * 2 * i / segments;
            double a1 = Math.PI * 2 * (i + 1) / segments;
            float x0 = (float) (center.x + Math.cos(a0) * radius), z0 = (float) (center.z + Math.sin(a0) * radius);
            float x1 = (float) (center.x + Math.cos(a1) * radius), z1 = (float) (center.z + Math.sin(a1) * radius);
            float nx = x1 - x0, nz = z1 - z0;
            float len = Mth.sqrt(nx * nx + nz * nz);
            nx /= len;
            nz /= len;
            float y = (float) center.y;
            lines.addVertex(pose, x0, y, z0).setColor(0.36F, 0.88F, 0.9F, alpha).setNormal(pose, nx, 0, nz);
            lines.addVertex(pose, x1, y, z1).setColor(0.36F, 0.88F, 0.9F, alpha).setNormal(pose, nx, 0, nz);
        }
    }

    private ScanHighlights() {}
}
