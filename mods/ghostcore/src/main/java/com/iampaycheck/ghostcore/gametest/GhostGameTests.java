package com.iampaycheck.ghostcore.gametest;

import com.iampaycheck.ghostcore.GhostConfig;
import com.iampaycheck.ghostcore.GhostCore;
import com.iampaycheck.ghostcore.ghost.GhostData;
import com.iampaycheck.ghostcore.ghost.GhostEntity;
import com.iampaycheck.ghostcore.ghost.GhostManager;
import com.iampaycheck.ghostcore.ghost.Scanner;
import com.iampaycheck.ghostcore.ghost.Transmat;
import com.iampaycheck.ghostcore.network.ScanResultPayload;
import com.iampaycheck.ghostcore.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Optional;

/** Run with {@code ./gradlew runGameTestServer}. Arena: 7x6x7, smooth stone floor at y=0. */
@GameTestHolder(GhostCore.MODID)
@PrefixGameTestTemplate(false)
public class GhostGameTests {
    private static final String ARENA = "platform";

    /** A mock player standing in the middle of the arena. Mock players have no connection, so we tick them ourselves. */
    private static ServerPlayer operator(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        Vec3 spot = helper.absoluteVec(new Vec3(3.5, 1, 3.5));
        player.teleportTo(helper.getLevel(), spot.x, spot.y, spot.z, 0, 0);
        helper.onEachTick(player::doTick);
        return player;
    }

    @GameTest(template = ARENA)
    public static void bindingSpawnsAGhost(GameTestHelper helper) {
        ServerPlayer player = operator(helper);
        helper.assertTrue(GhostManager.bind(player), "first bind should succeed");
        helper.assertTrue(!GhostManager.bind(player), "second bind should be refused");
        helper.succeedWhen(() -> {
            GhostEntity ghost = GhostManager.ghostOf(player);
            helper.assertTrue(ghost != null && ghost.isAlive(), "Ghost entity should spawn");
            helper.assertTrue(ghost.distanceTo(player) < 3, "Ghost should hover near its owner");
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void lethalDamageDownsThenRevives(GameTestHelper helper) {
        ServerPlayer player = operator(helper);
        GhostManager.bind(player);
        GhostData data = GhostManager.data(player);
        int max = GhostConfig.MAX_CHARGES.get();
        data.lastSafePos = player.position();
        data.lastSafeDim = player.level().dimension();

        player.hurt(player.damageSources().fellOutOfWorld(), 1000F);

        helper.assertTrue(player.isAlive(), "Ghost should prevent the death");
        helper.assertTrue(data.isDowned(), "player should be downed");
        helper.assertTrue(data.charges == max - 1, "a resurrection charge should be spent");
        helper.assertTrue(player.getAttributeValue(Attributes.MOVEMENT_SPEED) == 0, "downed player should be rooted");
        helper.succeedWhen(() -> {
            helper.assertTrue(!data.isDowned(), "player should get back up");
            helper.assertTrue(player.getHealth() >= player.getMaxHealth() * 0.5F - 0.01F, "revive should restore health");
            helper.assertTrue(player.getAttributeValue(Attributes.MOVEMENT_SPEED) > 0, "movement should be restored");
        });
    }

    @GameTest(template = ARENA)
    public static void noChargesMeansRealDeath(GameTestHelper helper) {
        ServerPlayer player = operator(helper);
        GhostManager.bind(player);
        GhostData data = GhostManager.data(player);
        data.charges = 0;
        data.nextChargeAt = Long.MAX_VALUE;

        player.hurt(player.damageSources().fellOutOfWorld(), 1000F);

        helper.assertTrue(player.isDeadOrDying(), "with no charges left the player should die");
        helper.assertTrue(!data.isDowned(), "a dead player is not downed");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void scanFindsCachesOreAndHostiles(GameTestHelper helper) {
        BlockPos cache = new BlockPos(1, 1, 1);
        helper.setBlock(cache, Blocks.CHEST);
        ((RandomizableContainer) helper.getBlockEntity(cache)).setLootTable(BuiltInLootTables.SIMPLE_DUNGEON);
        BlockPos plainChest = new BlockPos(5, 1, 1);
        helper.setBlock(plainChest, Blocks.CHEST);
        BlockPos ore = new BlockPos(1, 1, 5);
        helper.setBlock(ore, Blocks.DIAMOND_ORE);
        Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(5, 1, 5));
        Cow cow = helper.spawn(EntityType.COW, new BlockPos(3, 1, 5));

        ScanResultPayload result = Scanner.collect(helper.getLevel(), helper.absolutePos(new BlockPos(3, 1, 3)), 8, 6, 100);

        helper.assertTrue(result.loot().contains(helper.absolutePos(cache)), "unlooted chest should be flagged");
        helper.assertTrue(!result.loot().contains(helper.absolutePos(plainChest)), "a chest with no loot table is not a cache");
        helper.assertTrue(result.ores().contains(helper.absolutePos(ore)), "diamond ore should be flagged");
        helper.assertTrue(result.hostiles().contains(zombie.getId()), "zombie should be flagged");
        helper.assertTrue(!result.hostiles().contains(cow.getId()), "cows are not hostile");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void transmatLandsOnBeacon(GameTestHelper helper) {
        ServerPlayer player = operator(helper);
        GhostManager.bind(player);
        GhostData data = GhostManager.data(player);
        BlockPos beacon = new BlockPos(5, 1, 5);
        helper.setBlock(beacon, ModBlocks.TRANSMAT_BEACON.get());

        Transmat.linkBeacon(player, GlobalPos.of(helper.getLevel().dimension(), helper.absolutePos(beacon)));
        // Let the mock player settle onto the floor first: any movement cancels the channel.
        helper.runAfterDelay(20, () -> {
            Transmat.request(player, data);
            helper.assertTrue(data.isChanneling(), "transmat should start channeling");
        });

        Vec3 pad = Vec3.atBottomCenterOf(helper.absolutePos(beacon.above()));
        helper.succeedWhen(() -> {
            helper.assertTrue(player.position().distanceTo(pad) < 0.5, "player should land on the beacon, is at " + player.position());
            helper.assertTrue(data.transmatReadyAt > helper.getLevel().getGameTime(), "cooldown should start");
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void ghostLightsTheDark(GameTestHelper helper) {
        // Seal the arena so no sky light reaches inside.
        for (int x = 0; x < 7; x++) for (int y = 1; y < 6; y++) for (int z = 0; z < 7; z++) {
            if (x == 0 || x == 6 || z == 0 || z == 6 || y == 5) helper.setBlock(x, y, z, Blocks.STONE);
        }
        ServerPlayer player = operator(helper);
        GhostManager.bind(player);
        helper.succeedWhen(() -> {
            GhostEntity ghost = GhostManager.ghostOf(player);
            helper.assertTrue(ghost != null, "Ghost should spawn");
            BlockPos lit = ghost.blockPosition();
            boolean found = BlockPos.betweenClosedStream(lit.offset(-1, -1, -1), lit.offset(1, 1, 1))
                    .anyMatch(p -> helper.getLevel().getBlockState(p).is(ModBlocks.GHOST_LIGHT.get()));
            helper.assertTrue(found, "Ghost should carry a light in the dark");
        });
    }

    @GameTest(template = ARENA)
    public static void orphanedLightCleansItselfUp(GameTestHelper helper) {
        BlockPos pos = new BlockPos(3, 2, 3);
        helper.setBlock(pos, ModBlocks.GHOST_LIGHT.get());
        helper.assertBlockPresent(ModBlocks.GHOST_LIGHT.get(), pos);
        helper.succeedWhen(() -> helper.assertBlockNotPresent(ModBlocks.GHOST_LIGHT.get(), pos));
    }

    @GameTest(template = ARENA)
    public static void ghostDataSurvivesSaveAndLoad(GameTestHelper helper) {
        GhostData data = new GhostData();
        data.bound = true;
        data.charges = 2;
        data.scanReadyAt = 1234;
        data.lightOn = false;
        data.beacon = Optional.of(GlobalPos.of(Level.NETHER, new BlockPos(10, 64, -5)));
        data.pocket.set(4, new ItemStack(Items.DIAMOND, 7));

        RegistryOps<Tag> ops = helper.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE);
        GhostData copy = GhostData.CODEC.parse(ops, GhostData.CODEC.encodeStart(ops, data).getOrThrow()).getOrThrow();

        helper.assertTrue(copy.bound && copy.charges == 2 && copy.scanReadyAt == 1234 && !copy.lightOn, "scalar fields should round-trip");
        helper.assertTrue(copy.beacon.equals(data.beacon), "beacon should round-trip");
        helper.assertTrue(ItemStack.matches(copy.pocket.get(4), data.pocket.get(4)), "pocket contents should round-trip");
        helper.succeed();
    }
}
