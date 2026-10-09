package com.iampaycheck.ghostcore.registry;

import com.iampaycheck.ghostcore.GhostCore;
import com.iampaycheck.ghostcore.ghost.GhostEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, GhostCore.MODID);

    // Never saved: the server respawns each Ghost from its owner's attachment data.
    // Clients simulate the follow motion themselves, so position updates can be infrequent.
    public static final DeferredHolder<EntityType<?>, EntityType<GhostEntity>> GHOST = ENTITIES.register("ghost",
            () -> EntityType.Builder.<GhostEntity>of(GhostEntity::new, MobCategory.MISC)
                    .sized(0.4F, 0.4F)
                    .clientTrackingRange(10)
                    .updateInterval(10)
                    .noSave()
                    .noSummon()
                    .fireImmune()
                    .build("ghost"));

    private ModEntities() {}
}
