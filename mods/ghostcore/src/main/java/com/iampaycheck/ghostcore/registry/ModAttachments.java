package com.iampaycheck.ghostcore.registry;

import com.iampaycheck.ghostcore.GhostCore;
import com.iampaycheck.ghostcore.ghost.GhostData;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, GhostCore.MODID);

    /** The player's bond with their Ghost. Survives death so the Ghost (and its cache) stay with you. */
    public static final Supplier<AttachmentType<GhostData>> GHOST = ATTACHMENTS.register("ghost",
            () -> AttachmentType.builder(() -> new GhostData()).serialize(GhostData.CODEC).copyOnDeath().build());

    private ModAttachments() {}
}
