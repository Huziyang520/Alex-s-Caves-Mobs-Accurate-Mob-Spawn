package com.Huziyang520.alexsaccuratemobspawn.spawn;

import com.Huziyang520.alexsaccuratemobspawn.alexsaccuratemobspawn;
import com.Huziyang520.alexsaccuratemobspawn.config.MobRules;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * The single place where a mob's entry into the world is judged.
 *
 * <h2>1. Spawn probability (frequency)</h2>
 * Natural spawns only. Below {@code 1.0} the attempt is accepted with exactly that probability, so
 * {@code 0.5} really is about half as many spawns and {@code 0.0} removes the mob from natural spawning
 * entirely - while spawn eggs, commands, dispensers, mob spawners and transformations stay untouched.
 * This is a per attempt decision at a point that is consumed exactly once, so no vanilla spawn list is
 * ever rewritten for it. Values above {@code 1.0} are handled by boosting the biome's weights
 * ({@link SpawnWeightScaler}), not here.
 *
 * <h2>2. Spawn multiplier (how many at once)</h2>
 * The original mod behaviour, unchanged:
 * <ul>
 *   <li>{@code 0 < x < 1}: the entity is kept with probability {@code x}, otherwise the spawn is cancelled.</li>
 *   <li>{@code x = 0}: the spawn is always cancelled.</li>
 *   <li>{@code x > 1}: {@code floor(x) - 1} extra copies are added around the original, plus one more with
 *       the probability of the fractional part, capped at {@value #MAX_EXTRA_COPIES} per entity.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = alexsaccuratemobspawn.MOD_ID)
public final class NaturalSpawnHandler {

    /** Upper bound for the copies added by a single spawn; original mod behaviour. */
    private static final int MAX_EXTRA_COPIES = 50;

    /** Guards against the copies of a copy being multiplied again. */
    private static final ThreadLocal<Boolean> SPAWNING_EXTRA = ThreadLocal.withInitial(() -> false);

    /**
     * How each mob was spawned, taken from Forge's {@code MobSpawnEvent.FinalizeSpawn}. Weak keys, and
     * entries are removed when the mob joins, so nothing is retained after the spawn completed.
     */
    private static final Map<Entity, MobSpawnType> ORIGINS =
            Collections.synchronizedMap(new WeakHashMap<>());

    private NaturalSpawnHandler() {
    }

    /** Natural sources: the runtime spawn cycle, world generation batches and structure spawns. */
    private static boolean isNatural(MobSpawnType type) {
        return type == MobSpawnType.NATURAL
                || type == MobSpawnType.CHUNK_GENERATION
                || type == MobSpawnType.STRUCTURE;
    }

    @SubscribeEvent
    public static void onFinalizeSpawn(MobSpawnEvent.FinalizeSpawn event) {
        Entity entity = event.getEntity();
        if (entity != null) {
            ORIGINS.put(entity, event.getSpawnType());
        }
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof Mob mob)) {
            return;
        }

        // Read and clear: this entity is judged exactly once, when it first enters the world.
        MobSpawnType origin = ORIGINS.remove(mob);
        if (applyProbability(event, mob, origin)) {
            return;
        }

        applyMultiplier(event, mob);
    }

    /** @return true when the spawn was rejected, so the multiplier must not run either. */
    private static boolean applyProbability(EntityJoinLevelEvent event, Mob mob, MobSpawnType origin) {
        if (!MobRules.probabilityEnabled() || !isNatural(origin)) {
            return false;
        }

        double probability = MobRules.probability(mob.getType());
        if (probability >= 1.0D) {
            return false;
        }

        if (probability <= 0.0D) {
            logOnce("probability", mob.getType(), "blocked natural spawn, probability " + probability);
            event.setCanceled(true);
            return true;
        }
        if (mob.getRandom().nextDouble() >= probability) {
            logOnce("probability", mob.getType(), "dropped natural spawn, probability " + probability);
            event.setCanceled(true);
            return true;
        }
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
}
