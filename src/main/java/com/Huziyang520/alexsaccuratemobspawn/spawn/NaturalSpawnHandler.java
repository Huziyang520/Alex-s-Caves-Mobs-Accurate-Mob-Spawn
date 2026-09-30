package com.Huziyang520.alexsaccuratemobspawn.spawn;

import com.Huziyang520.alexsaccuratemobspawn.alexsaccuratemobspawn;
import com.Huziyang520.alexsaccuratemobspawn.config.MobRules;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * The single place where a mob's entry into the world is judged.
 *
 * <h2>1. Spawn probability (frequency)</h2>
 * Expectation of a natural spawn = {@code original occurrences × probability}, one rule for every value,
 * and <b>without touching any other mob</b>:
 * <ul>
 *   <li>{@code 1.0}: nothing.</li>
 *   <li>{@code 0 < p < 1}: an opportunity this mob already won is kept with probability {@code p}, the
 *       rest are voided. A voided opportunity is not handed to anybody else, so no other mob changes.</li>
 *   <li>{@code 0}: every natural opportunity of this mob is voided.</li>
 *   <li>{@code p > 1}: the vanilla occurrence is kept and the missing occurrences are created by our own
 *       spawn library {@link ExtraSpawnDriver} as additional, spread out attempts.</li>
 * </ul>
 * Only natural spawns count ({@code NATURAL}, {@code CHUNK_GENERATION}, {@code STRUCTURE}); spawn eggs,
 * commands, spawner blocks and event / transformation spawns are never affected. The origin is taken
 * from the {@code Mob#finalizeSpawn} mixin, which also covers the spawns other mods perform on their own
 * (Alex's Caves' cave burst uses {@code CHUNK_GENERATION}, Alex's Mobs' beached whale spawner uses
 * {@code SPAWNER}), with Forge's {@code MobSpawnEvent.PositionCheck} as a mixin free second source.
 * Individuals created by the library are marked so probability is not applied to them twice.
 *
 * <h2>2. Spawn multiplier (how many at once)</h2>
 * The original mod behaviour, untouched: {@code 0 < x < 1} keeps the entity with probability {@code x};
 * {@code x = 0} always cancels; {@code x > 1} adds {@code floor(x) - 1} copies plus a fractional one,
 * capped at {@value #MAX_EXTRA_COPIES}. The two features are independent and their effects multiply.
 */
@Mod.EventBusSubscriber(modid = alexsaccuratemobspawn.MOD_ID)
public final class NaturalSpawnHandler {

    /** Upper bound for the copies added by a single spawn; original mod behaviour. */
    private static final int MAX_EXTRA_COPIES = 50;

    /** Guards against the copies of a copy being multiplied again. */
    private static final ThreadLocal<Boolean> SPAWNING_EXTRA = ThreadLocal.withInitial(() -> false);

    /** Upper bound for queued extra spawn work, so a burst can never pile up without limit. */
    private static final int MAX_PENDING = 4096;

    /** How many queued extra spawn tasks run per server tick. */
    private static final int PENDING_PER_TICK = 8;

    /** How each mob was spawned; weak keys, removed when the mob joins. */
    private static final Map<Entity, MobSpawnType> ORIGINS =
            Collections.synchronizedMap(new WeakHashMap<>());

    /** Mobs currently created by our own spawn library; they must not be judged again. */
    private static final Set<Entity> MODULE_SPAWNS = Collections.synchronizedSet(new HashSet<>());

    /** Extra spawn work queued from world generation threads to run on the server thread. */
    private static final Queue<PendingExtra> PENDING = new ConcurrentLinkedQueue<>();

    /** Depth of "a mob is being created by known world generation code" on this thread. */
    private static final ThreadLocal<Integer> WORLDGEN_SPAWN_DEPTH = ThreadLocal.withInitial(() -> 0);

    /**
     * Mobs whose probability was already decided in {@code Mob#checkSpawnRules}, right before a spawner
     * added them. The join event must not roll the same spawn a second time.
     */
    private static final Set<Entity> PRE_JUDGED = Collections.synchronizedSet(
            Collections.newSetFromMap(new WeakHashMap<>()));

    /**
     * Entities created inside a known world generation window. Weak on purpose: entries disappear once
     * the entity itself is gone.
     */
    private static final Set<Entity> WORLDGEN_CREATED = Collections.synchronizedSet(
            Collections.newSetFromMap(new WeakHashMap<>()));

    private NaturalSpawnHandler() {
    }

    /** Natural sources: the runtime spawn cycle, world generation batches and structure spawns. */
    private static boolean isNatural(MobSpawnType type) {
        return type == MobSpawnType.NATURAL
                || type == MobSpawnType.CHUNK_GENERATION
                || type == MobSpawnType.STRUCTURE;
    }

    public static void beginModuleSpawn(Mob mob) {
        MODULE_SPAWNS.add(mob);
    }

    /**
     * Marks the current thread as "inside a spawn performed by world generation code that we know of"
     * (Alex's Caves' roost feature and its world generated dinosaur eggs create their mobs directly and
     * never call {@code Mob#finalizeSpawn}, so no spawn type can be read from there).
     */
    public static void beginWorldgenSpawn() {
        WORLDGEN_SPAWN_DEPTH.set(WORLDGEN_SPAWN_DEPTH.get() + 1);
    }

    public static void endWorldgenSpawn() {
        int depth = WORLDGEN_SPAWN_DEPTH.get() - 1;
        WORLDGEN_SPAWN_DEPTH.set(Math.max(0, depth));
    }

    private static boolean inKnownWorldgenSpawn() {
        return WORLDGEN_SPAWN_DEPTH.get() > 0;
    }

    public static void endModuleSpawn(Mob mob) {
        MODULE_SPAWNS.remove(mob);
    }

    /**
     * Records how a mob was spawned. Called from two places on purpose:
     *
     * <ol>
     *   <li>the {@code Mob#finalizeSpawn} mixin - that is the only hook that also sees the spawns
     *       performed by other mods themselves (Alex's Caves' cave burst, Alex's Mobs' beached whale
     *       spawner, conversions, spawn eggs, commands), because every spawn path calls
     *       {@code finalizeSpawn} with its {@link MobSpawnType};</li>
     *   <li>Forge's {@code MobSpawnEvent.PositionCheck}, which fires inside the vanilla spawn cycle for
     *       the runtime spawn pass, the world generation batch and structure spawns. It is kept as a
     *       second, mixin free source, so the feature still works for vanilla spawning even if the mixin
     *       could not be applied.</li>
     * </ol>
     */
    public static void recordOrigin(Mob mob, MobSpawnType type) {
        if (mob != null && type != null) {
            ORIGINS.put(mob, type);
        }
    }

    @SubscribeEvent
    public static void onPositionCheck(MobSpawnEvent.PositionCheck event) {
        // getEntity() already returns the Mob for this event.
        recordOrigin(event.getEntity(), event.getSpawnType());
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof Mob mob)) {
            return;
        }

        MobSpawnType origin = ORIGINS.remove(mob);
        if (origin == null && (inKnownWorldgenSpawn() || WORLDGEN_CREATED.remove(mob))) {
            // Alex's Caves' roost feature and its world generated dinosaur eggs create the entity and add
            // it directly, without ever calling Mob#finalizeSpawn. The thread flag is raised inside those
            // code paths and the stamp survives a join that happens later on another thread.
            origin = MobSpawnType.CHUNK_GENERATION;
        }
        if (origin == null && MobRules.probabilityEnabled() && MobRules.probability(mob.getType()) != 1.0D) {
            // Bounded diagnostic: tells us which mobs still arrive through a path with no spawn type.
            logOnce("origin", mob.getType(), "created without a spawn type; probability not applied");
        }
        if (PRE_JUDGED.remove(mob)) {
            // Already decided before the mob was added; only the multiplier is left to do.
            if (!MODULE_SPAWNS.contains(mob)) {
                applyMultiplier(event, mob);
            }
            return;
        }
        if (!MODULE_SPAWNS.contains(mob) && applyProbability(event, mob, origin)) {
            return;
        }
        applyMultiplier(event, mob);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        for (int i = 0; i < PENDING_PER_TICK; i++) {
            PendingExtra task = PENDING.poll();
            if (task == null) {
                return;
            }
            ExtraSpawnDriver.spawnExtra(task.level, task.type, task.anchor, task.count);
        }
    }

    /**
     * Judges a natural spawn in {@code Mob#checkSpawnRules}, the last check a spawner runs right before it
     * adds the mob to the world, and the only place that sees spawns which never produce a join event on
     * the same thread (Alex's Caves' cave creature burst is one of them).
     *
     * @return true when the mob must not be created at all
     */
    public static boolean shouldVoidBeforeAdding(Mob mob, LevelAccessor level, MobSpawnType spawnType) {
        if (!MobRules.probabilityEnabled() || !isNatural(spawnType)) {
            return false;
        }

        double probability = MobRules.probability(mob.getType());
        if (probability == 1.0D) {
            return false;
        }

        if (probability <= 0.0D) {
            logOnce("probability", mob.getType(), "voided every natural opportunity, probability " + probability);
            return true;
        }

        if (probability < 1.0D) {
            if (mob.getRandom().nextDouble() >= probability) {
                logOnce("probability", mob.getType(), "voided this opportunity, probability " + probability);
                return true;
            }
            PRE_JUDGED.add(mob);
            return false;
        }

        queueExtraSpawns(level, mob, probability);
        PRE_JUDGED.add(mob);
        return false;
    }

    /**
     * Creates the additional occurrences asked for by a probability above {@code 1.0}. The vanilla
     * occurrence itself is always kept, so the total expectation becomes {@code occurrences × probability}.
     */
    private static void queueExtraSpawns(LevelAccessor level, Mob mob, double probability) {
        int extra = Mth.floor(probability) - 1;
        if (mob.getRandom().nextDouble() < (probability - Mth.floor(probability))) {
            extra++;
        }
        extra = Math.min(extra, ExtraSpawnDriver.MAX_EXTRA_PER_SPAWN);
        if (extra <= 0) {
            return;
        }

        ServerLevel serverLevel = asServerLevel(level);
        if (serverLevel == null) {
            return;
        }
        if (PENDING.size() < MAX_PENDING) {
            PENDING.add(new PendingExtra(serverLevel, mob.getType(), mob.blockPosition(), extra));
        }
        logOnce("probability", mob.getType(),
                "queued " + extra + " extra spread out attempts, probability " + probability);
    }

    private static ServerLevel asServerLevel(LevelAccessor level) {
        if (level instanceof ServerLevel serverLevel) {
            return serverLevel;
        }
        if (level instanceof WorldGenRegion region) {
            return region.getLevel();
        }
        return null;
    }

    /** Stamps an entity created inside a known world generation window; the join event may come later. */
    public static void recordWorldgenCreated(Entity entity) {
        if (entity != null && inKnownWorldgenSpawn()) {
            WORLDGEN_CREATED.add(entity);
        }
    }

    /** @return true when the spawn was rejected, so the multiplier must not run either. */
    private static boolean applyProbability(EntityJoinLevelEvent event, Mob mob, MobSpawnType origin) {
        if (!MobRules.probabilityEnabled() || !isNatural(origin)) {
            return false;
        }

        double probability = MobRules.probability(mob.getType());
        if (probability == 1.0D) {
            return false;
        }

        if (probability <= 0.0D) {
            logOnce("probability", mob.getType(), "voided every natural opportunity, probability " + probability);
            event.setCanceled(true);
            return true;
        }

        if (probability < 1.0D) {
            if (mob.getRandom().nextDouble() >= probability) {
                logOnce("probability", mob.getType(), "voided this opportunity, probability " + probability);
                event.setCanceled(true);
                return true;
            }
            return false;
        }

        // probability > 1: keep this vanilla occurrence and create the missing ones ourselves.
        queueExtraSpawns(event.getLevel(), mob, probability);
        return false;
    }

    private static void applyMultiplier(EntityJoinLevelEvent event, Mob mob) {
        if (!MobRules.multiplierEnabled() || !MobRules.hasMultiplierRules()) {
            return;
        }

        double multiplier = MobRules.multiplier(mob.getType());
        if (multiplier == 1.0D) {
            return;
        }

        if (multiplier < 1.0D) {
            if (multiplier <= 0.0D) {
                logOnce("multiplier", mob.getType(), "blocked every spawn, multiplier " + multiplier);
                event.setCanceled(true);
            } else if (mob.getRandom().nextDouble() > multiplier) {
                logOnce("multiplier", mob.getType(), "dropped this spawn, multiplier " + multiplier);
                event.setCanceled(true);
            }
            return;
        }

        logOnce("multiplier", mob.getType(), "spawning extra copies, multiplier " + multiplier);

        if (SPAWNING_EXTRA.get()) {
            return;
        }
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            SPAWNING_EXTRA.set(true);
            try {
                int totalCopies = (int) multiplier - 1;
                if (mob.getRandom().nextDouble() < (multiplier - (int) multiplier)) {
                    totalCopies++;
                }
                totalCopies = Math.min(totalCopies, MAX_EXTRA_COPIES);
                spawnExtraCopies(serverLevel, mob, totalCopies);
            } finally {
                SPAWNING_EXTRA.set(false);
            }
        }
    }

    /**
     * Logs one line per entity type per config revision, so a player can see that (and what) the rules
     * did without flooding the log with one line per spawn event.
     */
    private static void logOnce(String feature, EntityType<?> type, String message) {
        if (MobRules.logOnce(feature + ":" + type)) {
            alexsaccuratemobspawn.LOGGER.info("Spawn {} for {}: {}", feature, type, message);
        }
    }

    private static void spawnExtraCopies(ServerLevel level, Mob originalMob, int count) {
        if (count <= 0) {
            return;
        }

        EntityType<?> type = originalMob.getType();
        BlockPos pos = originalMob.blockPosition();

        for (int i = 0; i < count; i++) {
            Entity newEntity = type.create(level);
            if (!(newEntity instanceof Mob newMob)) {
                if (newEntity != null) {
                    newEntity.discard();
                }
                continue;
            }
            double x = pos.getX() + 0.5D + (level.random.nextDouble() - 0.5D) * 2.0D;
            double y = pos.getY() + 0.5D;
            double z = pos.getZ() + 0.5D + (level.random.nextDouble() - 0.5D) * 2.0D;
            newMob.setPos(x, y, z);
            newMob.setYRot(level.random.nextFloat() * 360.0F);
            level.addFreshEntity(newMob);
        }
    }

    /** One unit of queued extra spawn work. */
    private record PendingExtra(ServerLevel level, EntityType<?> type, BlockPos anchor, int count) {
    }
}
