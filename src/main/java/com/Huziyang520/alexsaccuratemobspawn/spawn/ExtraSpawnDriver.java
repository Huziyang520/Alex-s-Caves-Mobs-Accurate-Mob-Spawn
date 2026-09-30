package com.Huziyang520.alexsaccuratemobspawn.spawn;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * The self written spawn library that backs the probability feature.
 *
 * <p>It never touches a vanilla spawn list. A probability above {@code 1} is realised by running
 * <b>additional, independent vanilla style spawn attempts</b> for that one mob type - the only way to
 * make a mob spawn more often while every other mob keeps its vanilla behaviour untouched, because the
 * vanilla spawn cycle produces one mob per attempt and only we can create the extra attempts.</p>
 *
 * <h2>Kept spread out on purpose</h2>
 * An attempt is not a copy next to the original spawn. Each one picks a random direction and a random
 * distance of {@value #MIN_RADIUS} to {@value #MAX_RADIUS} blocks around the anchor, then the position is
 * validated the way vanilla validates a natural spawn: the biome must really list this mob, the
 * placement rules must pass, the spot must be free. Several candidates are tried per individual and a
 * failed attempt simply spawns nothing, exactly like a vanilla attempt that fails.
 *
 * <h2>Budget</h2>
 * The individuals created here do <b>not</b> count towards the vanilla mob cap, so the vanilla spawn
 * cycle keeps giving every other mob its full, unchanged share. To keep that from running away this
 * library enforces its own hard ceiling of {@value #MAX_EXTRA_ALIVE} extra individuals per mob category
 * around the anchor, and never adds more than {@value #MAX_EXTRA_PER_SPAWN} individuals for one natural
 * spawn (the probability value is capped at 100, so at most 99 are ever requested).
 */
public final class ExtraSpawnDriver {

    /** Hard ceiling for a single natural spawn. */
    public static final int MAX_EXTRA_PER_SPAWN = 100;

    /** Hard ceiling for extra individuals this library keeps alive around one anchor. */
    public static final int MAX_EXTRA_ALIVE = 100;

    /** Radius the extra attempts look in; large enough to stay spread out. */
    private static final int MIN_RADIUS = 8;
    private static final int MAX_RADIUS = 48;

    /** Position candidates per extra individual. */
    private static final int CANDIDATES = 12;

    /** How far around the anchor our own extra individuals are counted for the ceiling. */
    private static final double BUDGET_RADIUS = 128.0D;

    /** Individuals this library created; weak keys so nothing is retained once they are gone. */
    private static final Set<Mob> OUR_EXTRAS =
            Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

    private ExtraSpawnDriver() {
    }

    /**
     * Runs the extra spawn attempts, spread around {@code anchor}.
     *
     * @return how many individuals were actually created.
     */
    public static int spawnExtra(ServerLevel level, EntityType<?> type, BlockPos anchor, int desired) {
        if (desired <= 0) {
            return 0;
        }
        MobCategory category = type.getCategory();
        int attempts = Math.min(Math.min(desired, MAX_EXTRA_PER_SPAWN), aliveBudget(level, category, anchor));
        if (attempts <= 0) {
            return 0;
        }

        int spawned = 0;
        RandomSource random = level.random;
        for (int i = 0; i < attempts; i++) {
            if (tryPlaceOne(level, type, category, anchor, random)) {
                spawned++;
            }
        }
        return spawned;
    }

    /** @return how many more individuals this library may add around {@code anchor} for that category. */
    private static int aliveBudget(ServerLevel level, MobCategory category, BlockPos anchor) {
        AABB area = new AABB(anchor).inflate(BUDGET_RADIUS);
        int alive = 0;
        synchronized (OUR_EXTRAS) {
            for (Mob extra : OUR_EXTRAS) {
                if (!extra.isRemoved() && extra.level() == level && area.contains(extra.position())) {
                    alive++;
                }
            }
        }
        return Math.max(0, MAX_EXTRA_ALIVE - alive);
    }

    private static boolean tryPlaceOne(ServerLevel level, EntityType<?> type, MobCategory category,
                                       BlockPos anchor, RandomSource random) {
        for (int candidate = 0; candidate < CANDIDATES; candidate++) {
            BlockPos pos = candidatePos(level, anchor, random);
            if (pos == null || !isValidSpot(level, type, category, pos, random)) {
                continue;
            }

            Entity created = asMobType(type).create(level);
            if (!(created instanceof Mob mob)) {
                if (created != null) {
                    created.discard();
                }
                return false;
            }

            mob.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, random.nextFloat() * 360.0F, 0.0F);
            NaturalSpawnHandler.beginModuleSpawn(mob);
            try {
                mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.NATURAL, null, null);
                level.addFreshEntityWithPassengers(mob);
            } finally {
                NaturalSpawnHandler.endModuleSpawn(mob);
            }
            OUR_EXTRAS.add(mob);
            return true;
        }
        return false;
    }

    /** A random, loaded, spread out position around the anchor, or {@code null} when none is usable. */
    private static BlockPos candidatePos(ServerLevel level, BlockPos anchor, RandomSource random) {
        double angle = random.nextDouble() * Math.PI * 2.0D;
        double distance = MIN_RADIUS + random.nextDouble() * (MAX_RADIUS - MIN_RADIUS);
        int x = Mth.floor(anchor.getX() + Math.cos(angle) * distance);
        int z = Mth.floor(anchor.getZ() + Math.sin(angle) * distance);
        if (!level.hasChunkAt(new BlockPos(x, anchor.getY(), z))) {
            return null;
        }
        int surface = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) + 1;
        if (surface <= level.getMinBuildHeight()) {
            return null;
        }
        int y = Mth.randomBetweenInclusive(random, level.getMinBuildHeight(), surface);
        return new BlockPos(x, y, z);
    }

    /** The same checks vanilla performs before it accepts a natural spawn. */
    private static boolean isValidSpot(ServerLevel level, EntityType<?> type, MobCategory category,
                                       BlockPos pos, RandomSource random) {
        boolean listedHere = level.getBiome(pos).value().getMobSettings().getMobs(category)
                .unwrap().stream().anyMatch(data -> data.type == type);
        if (!listedHere) {
            // Only spawn where the biome itself lists this mob, so nothing is conjured into a biome it
            // does not belong to.
            return false;
        }
        EntityType<Mob> mobType = asMobType(type);
        if (!NaturalSpawner.isSpawnPositionOk(SpawnPlacements.getPlacementType(mobType), level, pos, mobType)) {
            return false;
        }
        if (!SpawnPlacements.checkSpawnRules(mobType, level, MobSpawnType.NATURAL, pos, random)) {
            return false;
        }

        Entity probe = mobType.create(level);
        if (!(probe instanceof Mob mob)) {
            if (probe != null) {
                probe.discard();
            }
            return false;
        }
        boolean ok;
        try {
            mob.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 0.0F, 0.0F);
            ok = level.noCollision(mob)
                    && mob.checkSpawnObstruction(level)
                    && mob.checkSpawnRules(level, MobSpawnType.NATURAL);
        } finally {
            mob.discard();
        }
        return ok;
    }

    /** The entity types handled here are always mob types; the wildcard only comes from the registry. */
    @SuppressWarnings("unchecked")
    private static EntityType<Mob> asMobType(EntityType<?> type) {
        return (EntityType<Mob>) type;
    }
}
