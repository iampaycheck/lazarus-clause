package com.iampaycheck.ghostcore.client.render;

import com.iampaycheck.ghostcore.GhostCore;
import com.iampaycheck.ghostcore.ghost.GhostEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class GhostRenderer extends EntityRenderer<GhostEntity> {
    private static final ResourceLocation TEXTURE = GhostCore.id("textures/entity/ghost.png");
    private static final ResourceLocation GLOW = GhostCore.id("textures/entity/ghost_glow.png");
    private static final float SCALE = 0.8F;

    private final GhostModel model;

    public GhostRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new GhostModel(context.bakeLayer(GhostModel.LAYER));
        this.shadowRadius = 0F;
    }

    @Override
    public boolean shouldRender(GhostEntity ghost, Frustum frustum, double camX, double camY, double camZ) {
        if (ghost.isInvisible()) return false;
        Minecraft mc = Minecraft.getInstance();
        // In first person, don't let your own Ghost clip through the camera.
        if (mc.options.getCameraType().isFirstPerson() && ghost.getOwner() == mc.player
                && ghost.position().add(0, GhostEntity.CENTER, 0).distanceToSqr(camX, camY, camZ) < 0.6 * 0.6) {
            return false;
        }
        return super.shouldRender(ghost, frustum, camX, camY, camZ);
    }

    @Override
    public void render(GhostEntity ghost, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int packedLight) {
        poseStack.pushPose();
        poseStack.translate(0, GhostEntity.CENTER, 0);
        poseStack.mulPose(Axis.YP.rotationDegrees(180F - Mth.rotLerp(partialTick, ghost.yRotO, ghost.getYRot())));
        poseStack.mulPose(Axis.XP.rotationDegrees(-Mth.lerp(partialTick, ghost.xRotO, ghost.getXRot())));
        poseStack.scale(-SCALE, -SCALE, SCALE);

        model.animate(ghost.tickCount + partialTick, ghost.getShellOpen(partialTick));
        // The Ghost carries its own light, so never render it darker than a dim glow.
        int light = LightTexture.pack(Math.max(LightTexture.block(packedLight), 11), LightTexture.sky(packedLight));
        model.renderToBuffer(poseStack, buffers.getBuffer(model.renderType(TEXTURE)), light, OverlayTexture.NO_OVERLAY, -1);
        model.renderToBuffer(poseStack, buffers.getBuffer(RenderType.eyes(GLOW)), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, -1);
        poseStack.popPose();
        super.render(ghost, entityYaw, partialTick, poseStack, buffers, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(GhostEntity ghost) {
        return TEXTURE;
    }
}
