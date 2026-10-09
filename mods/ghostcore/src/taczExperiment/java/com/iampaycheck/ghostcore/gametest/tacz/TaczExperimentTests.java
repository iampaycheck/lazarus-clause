package com.iampaycheck.ghostcore.gametest.tacz;

import com.iampaycheck.ghostcore.GhostCore;
import com.iampaycheck.ghostcore.ghost.GhostData;
import com.iampaycheck.ghostcore.ghost.GhostEntity;
import com.iampaycheck.ghostcore.ghost.GhostManager;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.entity.ShootResult;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.api.item.builder.GunItemBuilder;
import com.tacz.guns.api.item.gun.FireMode;
import com.tacz.guns.entity.EntityKineticBullet;
import com.tacz.guns.init.ModDamageTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.network.CommonListenerCookie;
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
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/** Actual pinned port API, with no production compatibility changes or fire bypasses. */
@PrefixGameTestTemplate(false)
public final class TaczExperimentTests {
    private static final ResourceLocation GUN = ResourceLocation.parse("tacz:ak47");
    private static final ResourceLocation SCOPE = ResourceLocation.parse("tacz:sight_552");

    private TaczExperimentTests() {}

    private static ServerPlayer operator(GameTestHelper helper) {
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
        helper.assertTrue(TimelessAPI.getCommonGunIndex(GUN).isPresent(), "BLOCKED: bundled AK47 index missing");
        GunItemBuilder builder = GunItemBuilder.create().setId(GUN).setAmmoCount(10)
                .setAmmoInBarrel(true).setFireMode(FireMode.SEMI);
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

    @GameTest(template = "platform", templateNamespace = GhostCore.MODID, timeoutTicks = 200)
    public static void e4DownedPlayerCannotFire(GameTestHelper helper) {
        ServerPlayer player = operator(helper);
        ItemStack gun = gun(helper, false);
        IGunOperator operator = draw(helper, player, gun);
        helper.runAfterDelay(75, () -> {
            ready(helper, operator);
            GhostData data = down(helper, player);
            int before = ammo(gun);
            AtomicInteger bullets = new AtomicInteger();
            List<String> sounds = new ArrayList<>();
            Consumer<EntityJoinLevelEvent> observer = event -> {
                if (event.getEntity() instanceof EntityKineticBullet bullet && bullet.getOwner() == player) bullets.incrementAndGet();
            };
            NeoForge.EVENT_BUS.addListener(observer);
            SoundEvidence.observer = (source, sound) -> {
                if (source == player) sounds.add(sound);
            };
            ShootResult result;
            try {
                result = fire(player, operator);
            } finally {
                NeoForge.EVENT_BUS.unregister(observer);
                SoundEvidence.observer = null;
            }
            helper.assertTrue(data.isDowned(), "BLOCKED: player revived before measurement");
            int after = ammo(gun);
            String evidence = "result=" + result + ", spawnedBullets=" + bullets.get() + ", totalAmmo=" + before + "->" + after
                    + ", serverSoundDispatch=" + sounds + "; client playback not measured in headless GameTest";
            String outcome = bullets.get() > 0 || before != after ? "FAIL"
                    : result == ShootResult.FORGE_EVENT_CANCEL ? "PASS" : "BLOCKED";
            GhostCore.LOGGER.info("ISSUE10 E4 {}: {}", outcome, evidence);
            helper.assertTrue(bullets.get() == 0 && before == after, "E4 FAIL: " + evidence);
            // A normal precondition rejection cannot prove downed-state blocking.
            helper.assertTrue(result == ShootResult.FORGE_EVENT_CANCEL, "BLOCKED: unrelated fire rejection: " + result);
            helper.succeed();
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
