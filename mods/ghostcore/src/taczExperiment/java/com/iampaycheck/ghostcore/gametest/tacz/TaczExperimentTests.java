package com.iampaycheck.ghostcore.gametest.tacz;

import com.iampaycheck.ghostcore.GhostCore;
import com.iampaycheck.ghostcore.ghost.GhostData;
import com.iampaycheck.ghostcore.ghost.GhostEntity;
import com.iampaycheck.ghostcore.ghost.GhostManager;
import com.iampaycheck.ghostcore.network.GhostSyncPayload;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.entity.ShootResult;
import com.tacz.guns.api.event.common.GunFireEvent;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.api.item.builder.GunItemBuilder;
import com.tacz.guns.api.item.gun.FireMode;
import com.tacz.guns.entity.EntityKineticBullet;
import com.tacz.guns.init.ModDamageTypes;
import com.tacz.guns.network.message.ClientMessagePlayerShoot;
import com.tacz.guns.sound.SoundManager;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import com.mojang.authlib.GameProfile;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ConfigurationTask;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.LogicalSide;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.extensions.ICommonPacketListener;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Actual pinned port API against production Ghost Core, including its #11 TaCZ fire guard.
 * No test bypasses shooting, damage or collision checks.
 */
@PrefixGameTestTemplate(false)
public final class TaczExperimentTests {
    private static final ResourceLocation GUN = ResourceLocation.parse("tacz:ak47");
    private static final ResourceLocation SCOPE = ResourceLocation.parse("tacz:sight_552");

    private TaczExperimentTests() {}

    private static ServerPlayer operator(GameTestHelper helper) {
        return operator(helper, null);
    }

    /** With {@code syncs}, the sink also claims Ghost Core's channel and records every Ghost sync it is sent. */
    private static ServerPlayer operator(GameTestHelper helper, @Nullable List<GhostSyncPayload> syncs) {
        // The legacy helper hardcodes isCreative=true and places an unnegotiated
        // connection through login (TaCZ's gun-pack sync throws there). Create
        // a real survival ServerPlayer in the level without claiming a login test.
        CommonListenerCookie cookie = CommonListenerCookie.createInitial(
                new GameProfile(UUID.randomUUID(), "issue10-survival"), false);
        ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                cookie.gameProfile(), cookie.clientInformation());
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        player.connection = new ServerGamePacketListenerImpl(helper.getLevel().getServer(), connection, player, cookie) {
            @Override
            public void send(Packet<?> packet, PacketSendListener listener) {
                // Headless packet sink only: TaCZ also sends a base timestamp on level join.
                // This does not measure negotiation/client delivery. E1 keeps the original
                // eight tests unadapted so their raw networking incompatibility remains visible.
                if (syncs != null && packet instanceof ClientboundCustomPayloadPacket custom
                        && custom.payload() instanceof GhostSyncPayload sync) syncs.add(sync);
            }

            @Override
            public boolean hasChannel(ResourceLocation id) {
                // Only Ghost Core's channel, so its payloads still go through GhostNetwork.send.
                return syncs != null && id.getNamespace().equals(GhostCore.MODID);
            }
        };
        player.setGameMode(GameType.SURVIVAL);
        Vec3 spot = helper.absoluteVec(new Vec3(3.5, 1, 1.5));
        player.teleportTo(helper.getLevel(), spot.x, spot.y, spot.z, 0, 0);
        helper.getLevel().addNewPlayer(player);
        helper.assertTrue(!player.isCreative() && IGunOperator.fromLivingEntity(player).consumesAmmoOrNot(),
                "BLOCKED: fixture must consume ammo as a survival player");
        helper.onEachTick(player::doTick);
        return player;
    }

    private static ItemStack gun(GameTestHelper helper, boolean attachment) {
        return gun(helper, attachment, true);
    }

    private static ItemStack gun(GameTestHelper helper, boolean attachment, boolean chambered) {
        helper.assertTrue(TimelessAPI.getCommonGunIndex(GUN).isPresent(), "BLOCKED: bundled AK47 index missing");
        GunItemBuilder builder = GunItemBuilder.create().setId(GUN).setAmmoCount(10)
                .setAmmoInBarrel(chambered).setFireMode(FireMode.SEMI);
        if (attachment) {
            helper.assertTrue(TimelessAPI.getCommonAttachmentIndex(SCOPE).isPresent(), "BLOCKED: scope index missing");
            builder.putAttachment(AttachmentType.SCOPE, SCOPE);
        }
        ItemStack stack = builder.build(helper.getLevel().registryAccess());
        helper.assertTrue(!stack.isEmpty(), "BLOCKED: gun builder returned empty stack");
        return stack;
    }

    private static int ammo(ItemStack stack) {
        IGun gun = IGun.getIGunOrNull(stack);
        return gun.getCurrentAmmoCount(stack) + (gun.hasBulletInBarrel(stack) ? 1 : 0);
    }

    private static GhostData down(GameTestHelper helper, ServerPlayer player) {
        GhostManager.bind(player);
        GhostData data = GhostManager.data(player);
        int charges = data.charges;
        // E2/E3 explicitly exercise the port's own registered bullet damage type.
        boolean damaged = player.hurt(ModDamageTypes.Sources.bullet(helper.getLevel().registryAccess(), null, null, false), 1000F);
        helper.assertTrue(player.isAlive() && data.isDowned() && data.charges == charges - 1,
                "bullet damage must leave player alive/downed and spend exactly one charge: hurt=" + damaged
                        + ", health=" + player.getHealth() + ", downed=" + data.isDowned() + ", charges=" + charges + "->" + data.charges);
        return data;
    }

    private static IGunOperator draw(GameTestHelper helper, ServerPlayer player, ItemStack gun) {
        player.setItemSlot(EquipmentSlot.MAINHAND, gun);
        IGunOperator operator = IGunOperator.fromLivingEntity(player);
        operator.draw(player::getMainHandItem);
        return operator;
    }

    private static void ready(GameTestHelper helper, IGunOperator operator) {
        helper.assertTrue(operator.getSynDrawCoolDown() == 0, "BLOCKED: gun draw cooldown not elapsed");
        helper.assertTrue(operator.getCacheProperty() != null, "BLOCKED: gun attachment cache uninitialized");
    }

    private static ShootResult fire(ServerPlayer player, IGunOperator operator) {
        // Same four-argument entrypoint used by ClientMessagePlayerShoot.handle.
        // Keep the normal timestamp/network/ammo checks enabled.
        return operator.shoot(player::getXRot, player::getYRot,
                System.currentTimeMillis() - operator.getDataHolder().baseTimestamp, 0F);
    }

    /** What a client that ignores its fire guard sends: wire-encoded, decoded, then the port's own handler. */
    private static ShootResult firePacket(GameTestHelper helper, ServerPlayer player, IGunOperator operator) {
        ClientMessagePlayerShoot sent = new ClientMessagePlayerShoot(
                System.currentTimeMillis() - operator.getDataHolder().baseTimestamp, 0F);
        RegistryFriendlyByteBuf wire = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        ClientMessagePlayerShoot.STREAM_CODEC.encode(wire, sent);
        ClientMessagePlayerShoot.handle(ClientMessagePlayerShoot.STREAM_CODEC.decode(wire), new ServerThreadContext(player));
        return null; // the handler discards the result; bullets, ammo and sound are measured instead
    }

    /** Runs enqueued handler work at once: GameTests already run on the server thread. */
    private record ServerThreadContext(ServerPlayer player) implements IPayloadContext {
        @Override
        public ICommonPacketListener listener() {
            return player.connection;
        }

        @Override
        public CompletableFuture<Void> enqueueWork(Runnable task) {
            task.run();
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public <T> CompletableFuture<T> enqueueWork(Supplier<T> task) {
            return CompletableFuture.completedFuture(task.get());
        }

        @Override
        public PacketFlow flow() {
            return PacketFlow.SERVERBOUND;
        }

        @Override
        public void handle(CustomPacketPayload payload) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void finishCurrentTask(ConfigurationTask.Type type) {
            throw new UnsupportedOperationException();
        }
    }

    private record Shot(@Nullable ShootResult result, int bullets, int ammoBefore, int ammoAfter, List<String> sounds) {
        boolean nothingHappened() {
            return bullets == 0 && ammoBefore == ammoAfter && sounds.isEmpty();
        }

        boolean realShot() {
            return result == ShootResult.SUCCESS && bullets == 1 && ammoAfter == ammoBefore - 1
                    && sounds.contains(SoundManager.SHOOT_3P_SOUND);
        }

        @Override
        public String toString() {
            return "result=" + (result == null ? "n/a (packet handler)" : result) + ", spawnedBullets=" + bullets
                    + ", totalAmmo=" + ammoBefore + "->" + ammoAfter + ", serverSoundDispatch=" + sounds;
        }
    }

    /** Counts the shooter's real bullets, total loaded ammo and server sound dispatch around one fire attempt. */
    private static Shot measure(ServerPlayer player, ItemStack gun, Supplier<ShootResult> attempt) {
        int before = ammo(gun);
        AtomicInteger bullets = new AtomicInteger();
        List<String> sounds = new ArrayList<>();
        Consumer<EntityJoinLevelEvent> observer = event -> {
            if (event.getEntity() instanceof EntityKineticBullet bullet && bullet.getOwner() == player) bullets.incrementAndGet();
        };
        NeoForge.EVENT_BUS.addListener(EntityJoinLevelEvent.class, observer);
        SoundEvidence.observer = (source, sound) -> {
            if (source == player) sounds.add(sound);
        };
        ShootResult result;
        try {
            result = attempt.get();
        } finally {
            NeoForge.EVENT_BUS.unregister(observer);
            SoundEvidence.observer = null;
        }
        return new Shot(result, bullets.get(), before, ammo(gun), sounds);
    }

    /** Waits for Ghost Core's own tick to revive the player, then runs {@code then}. */
    private static void afterRevive(GameTestHelper helper, GhostData data, Runnable then) {
        long wait = data.downedUntil - helper.getLevel().getGameTime() + 2;
        helper.runAfterDelay(Math.max(1, wait), () -> {
            helper.assertTrue(!data.isDowned(), "BLOCKED: player was not revived on schedule");
            then.run();
        });
    }

    /** The latest Ghost sync as the client decodes it: the only input to the client's fire guard. */
    @Nullable
    private static GhostSyncPayload lastReceived(List<GhostSyncPayload> syncs) {
        if (syncs.isEmpty()) return null;
        FriendlyByteBuf wire = new FriendlyByteBuf(Unpooled.buffer());
        GhostSyncPayload.STREAM_CODEC.encode(wire, syncs.get(syncs.size() - 1));
        return GhostSyncPayload.STREAM_CODEC.decode(wire);
    }

    private static void pass(GameTestHelper helper, String id, String evidence) {
        GhostCore.LOGGER.info("ISSUE10 {} PASS: {}", id, evidence);
        helper.succeed();
    }

    @GameTest(template = "platform", templateNamespace = GhostCore.MODID)
    public static void e1PortBootsAndSupportsLegacyMockLogin(GameTestHelper helper) {
        helper.assertTrue(ModList.get().getModContainerById("tacz").orElseThrow().getModInfo().getVersion()
                .toString().equals("1.1.8-hotfix-r7"), "BLOCKED: wrong prototype version");
        helper.assertTrue(helper.getLevel().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE)
                .getHolder(ModDamageTypes.BULLET).isPresent(), "bullet type must register on dedicated server");
        GhostCore.LOGGER.info("ISSUE10 E1 BOOT: port loaded on dedicated server; checking the unadapted mock login used by existing tests");
        try {
            helper.makeMockServerPlayerInLevel();
        } catch (RuntimeException failure) {
            GhostCore.LOGGER.info("ISSUE10 E1 FAIL: unadapted mock login: {}", failure.toString());
            throw failure;
        }
        pass(helper, "E1", "dedicated boot and unadapted mock login; aggregate E1 also requires the existing eight tests to pass");
    }

    @GameTest(template = "platform", templateNamespace = GhostCore.MODID)
    public static void e2BulletDamageDownsPlayer(GameTestHelper helper) {
        ServerPlayer player = operator(helper);
        // ServerPlayer starts with 60 ticks of vanilla spawn invulnerability.
        helper.runAfterDelay(75, () -> {
            down(helper, player);
            pass(helper, "E2", "alive/downed, exactly one charge spent using tacz:bullet (packet-sink survival fixture)");
        });
    }

    @GameTest(template = "platform", templateNamespace = GhostCore.MODID)
    public static void e3DownedPlayerIgnoresBulletDamage(GameTestHelper helper) {
        ServerPlayer player = operator(helper);
        helper.runAfterDelay(75, () -> {
            GhostData data = down(helper, player);
            float health = player.getHealth();
            int charges = data.charges;
            // Avoid mistaking vanilla hurt cooldown for Ghost Core invulnerability.
            player.invulnerableTime = 0;
            player.hurt(ModDamageTypes.Sources.bullet(helper.getLevel().registryAccess(), null, null, true), 1000F);
            helper.assertTrue(player.getHealth() == health && data.charges == charges && data.isDowned(),
                    "additional armor-piercing bullet must not change downed health/charges");
            pass(helper, "E3", "health and charges unchanged after tacz:bullet_ignore_armor with hurt cooldown cleared (packet-sink survival fixture)");
        });
    }

    // #11 regression for #10's E4 FAIL (SUCCESS, 1 bullet, 11->10, shoot_3p while downed).
    @GameTest(template = "platform", templateNamespace = GhostCore.MODID, timeoutTicks = 300)
    public static void e4DownedPlayerCannotFireUntilRevived(GameTestHelper helper) {
        List<GhostSyncPayload> syncs = new ArrayList<>();
        ServerPlayer player = operator(helper, syncs);
        ItemStack gun = gun(helper, false);
        IGunOperator operator = draw(helper, player, gun);
        helper.runAfterDelay(75, () -> {
            ready(helper, operator);
            GhostData data = down(helper, player);
            Shot entrypoint = measure(player, gun, () -> fire(player, operator));
            Shot packet = measure(player, gun, () -> firePacket(helper, player, operator));
            // Burst cycles post GunFireEvent before ammo, bullets and sound; the guard must cancel it too.
            boolean cycleCancelled = NeoForge.EVENT_BUS.post(new GunFireEvent(player, gun, LogicalSide.SERVER)).isCanceled();
            helper.assertTrue(data.isDowned(), "BLOCKED: player revived before measurement");
            String downed = "entrypoint[" + entrypoint + "], decodedPacket[" + packet + "], serverGunFireEventCancelled=" + cycleCancelled;
            GhostCore.LOGGER.info("ISSUE11 E4 downed: {}", downed);
            helper.assertTrue(entrypoint.nothingHappened() && packet.nothingHappened(), "E4 FAIL: downed player fired: " + downed);
            // A normal precondition rejection cannot prove downed-state blocking.
            helper.assertTrue(entrypoint.result() == ShootResult.FORGE_EVENT_CANCEL, "BLOCKED: unrelated fire rejection: " + downed);
            helper.assertTrue(cycleCancelled, "E4 FAIL: server GunFireEvent not cancelled while downed");

            helper.runAfterDelay(2, () -> {
                // The client guard's only input: the Ghost sync the server sends through GhostNetwork.send.
                GhostSyncPayload sync = lastReceived(syncs);
                helper.assertTrue(data.isDowned() && sync != null && sync.isDowned(),
                        "E4 FAIL: synced client state does not show downed: " + sync);
                afterRevive(helper, data, () -> {
                    GhostSyncPayload revivedSync = lastReceived(syncs);
                    helper.assertTrue(revivedSync != null && !revivedSync.isDowned(),
                            "E4 FAIL: synced client state still downed after revive: " + revivedSync);
                    Shot revived = measure(player, gun, () -> fire(player, operator));
                    String evidence = downed + "; syncDowned=true->false (" + syncs.size() + " syncs); revived[" + revived
                            + "]; client playback not measured in headless GameTest";
                    GhostCore.LOGGER.info("ISSUE11 E4 {}: {}", revived.realShot() ? "PASS" : "FAIL", evidence);
                    helper.assertTrue(revived.realShot(), "E4 FAIL: firing did not resume after revive: " + evidence);
                    helper.succeed();
                });
            });
        });
    }

    // TaCZ chambers a closed-bolt round before posting GunShootEvent: the round moves, nothing is spent or fired.
    @GameTest(template = "platform", templateNamespace = GhostCore.MODID, timeoutTicks = 300)
    public static void e7DownedClosedBoltEmptyChamberSpendsNothing(GameTestHelper helper) {
        ServerPlayer player = operator(helper);
        ItemStack gun = gun(helper, false, false);
        IGun api = IGun.getIGunOrNull(gun);
        IGunOperator operator = draw(helper, player, gun);
        helper.runAfterDelay(75, () -> {
            ready(helper, operator);
            helper.assertTrue(!api.hasBulletInBarrel(gun) && api.getCurrentAmmoCount(gun) == 10,
                    "BLOCKED: fixture must start with an empty chamber and ten magazine rounds");
            GhostData data = down(helper, player);
            Shot downed = measure(player, gun, () -> fire(player, operator));
            String split = "magazine=" + api.getCurrentAmmoCount(gun) + ", chambered=" + api.hasBulletInBarrel(gun);
            GhostCore.LOGGER.info("ISSUE11 E7 downed: {}, {}", downed, split);
            helper.assertTrue(downed.nothingHappened(), "E7 FAIL: downed closed-bolt attempt spent or fired: " + downed + ", " + split);
            helper.assertTrue(downed.result() == ShootResult.FORGE_EVENT_CANCEL, "BLOCKED: unrelated fire rejection: " + downed);
            afterRevive(helper, data, () -> {
                Shot revived = measure(player, gun, () -> fire(player, operator));
                String evidence = "downed[" + downed + ", " + split + "]; revived[" + revived + "]";
                GhostCore.LOGGER.info("ISSUE11 E7 {}: {}", revived.realShot() ? "PASS" : "FAIL", evidence);
                helper.assertTrue(revived.realShot(), "E7 FAIL: firing did not resume after revive: " + evidence);
                helper.succeed();
            });
        });
    }

    @GameTest(template = "platform", templateNamespace = GhostCore.MODID)
    public static void e5LoadedAttachedGunSurvivesCodec(GameTestHelper helper) {
        GhostData data = new GhostData();
        ItemStack gun = gun(helper, true);
        IGun api = IGun.getIGunOrNull(gun);
        helper.assertTrue(ammo(gun) == 11 && api.getAttachmentId(gun, AttachmentType.SCOPE).equals(SCOPE),
                "BLOCKED: fixture must really contain ammo, chambered round and installed scope");
        data.pocket.set(4, gun);
        RegistryOps<Tag> ops = helper.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE);
        GhostData copy = GhostData.CODEC.parse(ops, GhostData.CODEC.encodeStart(ops, data).getOrThrow()).getOrThrow();
        helper.assertTrue(ItemStack.matches(gun, copy.pocket.get(4)), "loaded attached stack must round-trip exactly");
        pass(helper, "E5", "AK47, ten magazine rounds, chambered round and sight_552 match after GhostData.CODEC round-trip");
    }

    @GameTest(template = "platform", templateNamespace = GhostCore.MODID, timeoutTicks = 200)
    public static void e6OwnGhostDoesNotInterceptShot(GameTestHelper helper) {
        ServerPlayer player = operator(helper);
        GhostManager.bind(player);
        Cow target = helper.spawn(EntityType.COW, new BlockPos(3, 3, 5));
        target.setNoAi(true);
        target.setNoGravity(true);
        ItemStack gun = gun(helper, false);
        IGunOperator operator = draw(helper, player, gun);
        helper.runAfterDelay(75, () -> {
            ready(helper, operator);
            GhostEntity ghost = GhostManager.ghostOf(player);
            helper.assertTrue(ghost != null && ghost.getOwner() == player, "BLOCKED: owner's Ghost missing");
            // Legacy GameTest ticking can settle the mock one block into the arena
            // floor. Use an unobstructed standing firing position, then synchronize
            // old/current coordinates as the normal server entity tick does.
            Vec3 firingSpot = helper.absoluteVec(new Vec3(3.5, 3, 1.5));
            player.teleportTo(helper.getLevel(), firingSpot.x, firingSpot.y, firingSpot.z, 0, 0);
            player.setPose(net.minecraft.world.entity.Pose.STANDING);
            player.refreshDimensions();
            player.setOldPosAndRot();
            helper.assertTrue(!helper.getLevel().getBlockCollisions(player, player.getBoundingBox()).iterator().hasNext(),
                    "BLOCKED: shooter firing position intersects arena blocks: " + player.getBoundingBox());
            Vec3 aim = target.position().add(0, target.getBbHeight() / 2, 0).subtract(player.getEyePosition());
            player.setYRot((float) Math.toDegrees(Math.atan2(-aim.x, aim.z)));
            player.setXRot((float) -Math.toDegrees(Math.atan2(aim.y, Math.sqrt(aim.x * aim.x + aim.z * aim.z))));
            float before = target.getHealth();
            AtomicInteger crossed = new AtomicInteger();
            AtomicInteger bullets = new AtomicInteger();
            AtomicInteger targetSegments = new AtomicInteger();
            AtomicInteger clearSegments = new AtomicInteger();
            Consumer<EntityJoinLevelEvent> observer = event -> {
                if (event.getEntity() instanceof EntityKineticBullet bullet && bullet.getOwner() == player) {
                    bullets.incrementAndGet();
                }
            };
            Consumer<EntityTickEvent.Pre> beforeBullet = event -> {
                if (event.getEntity() instanceof EntityKineticBullet bullet && bullet.getOwner() == player && crossed.get() == 0) {
                    Vec3 start = bullet.position();
                    Vec3 end = start.add(bullet.getDeltaMovement());
                    Vec3 midpoint = start.add(end.subtract(start).normalize());
                    // Place the actual owner's Ghost immediately before the real projectile ticks,
                    // accounting for port inaccuracy without altering bullet velocity or collision logic.
                    ghost.setPos(midpoint.x, midpoint.y - ghost.getBbHeight() / 2, midpoint.z);
                    if (ghost.getBoundingBox().clip(start, end).isPresent()) crossed.incrementAndGet();
                    if (target.getBoundingBox().clip(start, end).isPresent()) targetSegments.incrementAndGet();
                    GhostCore.LOGGER.info("ISSUE10 E6 trajectory: start={}, end={}, targetBox={}, targetIntersection={}, ghostPickable={}",
                            start, end, target.getBoundingBox(), targetSegments.get(), ghost.isPickable());
                    var candidate = com.tacz.guns.util.EntityUtil.findEntityOnPath(bullet, start, end);
                    var block = com.tacz.guns.util.block.BlockRayTrace.rayTraceBlocks(helper.getLevel(),
                            new net.minecraft.world.level.ClipContext(start, end,
                                    net.minecraft.world.level.ClipContext.Block.COLLIDER,
                                    net.minecraft.world.level.ClipContext.Fluid.NONE, bullet));
                    var targetHit = target.getBoundingBox().clip(start, end);
                    if (targetHit.isPresent() && (block.getType() == net.minecraft.world.phys.HitResult.Type.MISS
                            || start.distanceToSqr(block.getLocation()) > start.distanceToSqr(targetHit.get()))) clearSegments.incrementAndGet();
                    GhostCore.LOGGER.info("ISSUE10 E6 collision: player={}, oldY={}, eye={}, pitch={}, candidate={}, block={}",
                            player.position(), player.yOld, player.getEyePosition(), player.getXRot(),
                            candidate == null ? "none" : candidate.getEntity().getType(), block.getType() + "@" + block.getLocation());
                }
            };
            NeoForge.EVENT_BUS.addListener(observer);
            NeoForge.EVENT_BUS.addListener(beforeBullet);
            ShootResult result;
            try {
                result = fire(player, operator);
            } catch (RuntimeException | Error failure) {
                NeoForge.EVENT_BUS.unregister(beforeBullet);
                throw failure;
            } finally {
                NeoForge.EVENT_BUS.unregister(observer);
            }
            if (result != ShootResult.SUCCESS || bullets.get() == 0) {
                NeoForge.EVENT_BUS.unregister(beforeBullet);
                helper.fail("BLOCKED: real fire path did not spawn bullet: " + result);
            }
            helper.runAfterDelay(5, () -> {
                NeoForge.EVENT_BUS.unregister(beforeBullet);
                helper.assertTrue(crossed.get() > 0, "BLOCKED: Ghost bounding box did not intersect actual bullet segment at collision tick");
                helper.assertTrue(targetSegments.get() > 0, "BLOCKED: real bullet segment missed target bounding box; cannot attribute no hit to Ghost");
                helper.assertTrue(clearSegments.get() > 0, "BLOCKED: arena block obstructed bullet before target; cannot attribute no hit to Ghost");
                GhostCore.LOGGER.info("ISSUE10 E6 {}: result={}, bullets={}, intersectingGhostSegments={}, targetHealth={}->{}",
                        target.getHealth() < before ? "PASS" : "FAIL", result, bullets.get(), crossed.get(), before, target.getHealth());
                helper.assertTrue(target.getHealth() < before, "E6 FAIL: target behind own Ghost was not hit by real bullet");
                helper.succeed();
            });
        });
    }
}
