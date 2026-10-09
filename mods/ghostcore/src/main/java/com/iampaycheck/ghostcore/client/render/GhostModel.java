package com.iampaycheck.ghostcore.client.render;

import com.iampaycheck.ghostcore.GhostCore;
import com.iampaycheck.ghostcore.ghost.GhostEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * Placeholder Ghost: a gunmetal core with a glowing lens, wrapped in four shell shards that
 * spread apart and orbit while it works. Swap for a Blockbench model when real art lands.
 * Model space: 1 unit = 1 pixel, front faces -Z.
 */
public class GhostModel extends EntityModel<GhostEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(GhostCore.id("ghost"), "main");

    private static final float SHARD_DISTANCE = 2.5F * Mth.SQRT_OF_TWO;
    private static final float TILT = 0.22F;

    private final ModelPart root;
    private final ModelPart core;
    private final ModelPart[] shards = new ModelPart[4];

    public GhostModel(ModelPart root) {
        this.root = root;
        this.core = root.getChild("core");
        for (int i = 0; i < 4; i++) shards[i] = root.getChild("shard_" + i);
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("core", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-2, -2, -2, 4, 4, 4)
                        .texOffs(16, 0).addBox(-1, -1, -3, 2, 2, 1)   // lens
                        .texOffs(16, 8).addBox(-0.5F, -1, 2, 1, 2, 3), // rear antenna fin
                PartPose.ZERO);
        for (int i = 0; i < 4; i++) {
            root.addOrReplaceChild("shard_" + i, CubeListBuilder.create()
                    .texOffs(0, 8).addBox(-1.5F, -1.5F, -2.5F, 3, 3, 5), PartPose.ZERO);
        }
        return LayerDefinition.create(mesh, 32, 32);
    }

    /** @param open 0 = shell closed around the core, 1 = expanded and orbiting (scanning, reviving, transmat). */
    public void animate(float age, float open) {
        float breathe = Mth.sin(age * 0.15F) * 0.15F;
        float distance = SHARD_DISTANCE + breathe + open * 2.2F;
        float roll = age * 0.12F * open;
        core.zRot = age * 0.05F * open;
        for (int i = 0; i < 4; i++) {
            float angle = (float) (Math.PI / 4 + i * Math.PI / 2) + roll;
            int sx = i == 0 || i == 3 ? 1 : -1;
            int sy = i < 2 ? 1 : -1;
            ModelPart shard = shards[i];
            shard.x = Mth.cos(angle) * distance;
            shard.y = Mth.sin(angle) * distance;
            shard.z = 0.5F - open * 0.5F;
            // Tilt each shard so its front tips lean in toward the lens; flatten as the shell opens.
            shard.yRot = sx * TILT * (1 - open * 0.6F);
            shard.xRot = -sy * TILT * (1 - open * 0.6F);
            shard.zRot = roll;
        }
    }

    @Override
    public void setupAnim(GhostEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        // Driven by animate(), which needs the partial-ticked shell state.
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        root.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
