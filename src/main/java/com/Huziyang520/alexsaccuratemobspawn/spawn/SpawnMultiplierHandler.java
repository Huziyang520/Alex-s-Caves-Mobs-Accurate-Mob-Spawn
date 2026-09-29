package com.Huziyang520.alexsaccuratemobspawn.spawn;

import com.Huziyang520.alexsaccuratemobspawn.alexsaccuratemobspawn;
import com.Huziyang520.alexsaccuratemobspawn.config.MobRules;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Spawn multiplier.
 *
 * <p>Keeps the original mod behaviour unchanged: the multiplier is applied when an entity joins the
 * level, it is NOT a spawn weight.</p>
 * <ul>
 *   <li>{@code 0 < x < 1}: the entity is kept with probability {@code x}, otherwise the spawn is cancelled.</li>
 *   <li>{@code x = 0}: the spawn is always cancelled.</li>
 *   <li>{@code x > 1}: {@code floor(x) - 1} extra copies are added around the original, plus one more
 *       with the probability of the fractional part, capped at {@value #MAX_EXTRA_COPIES} per entity.</li>
 * </ul>
 *
 * <p>Unlike the previous version this handler is not limited to the {@code alexscaves} namespace, so
 * the configuration can control any mob, including vanilla and other mods.</p>
 */
@Mod.EventBusSubscriber(modid = alexsaccuratemobspawn.MOD_ID)
public final class SpawnMultiplierHandler {

    /** Upper bound for the copies added by a single spawn; original mod behaviour. */
    private static final int MAX_EXTRA_COPIES = 50;

    /** Guards against the copies of a copy being multiplied again. */
    private static final ThreadLocal<Boolean> SPAWNING_EXTRA = ThreadLocal.withInitial(() -> false);

    private SpawnMultiplierHandler() {
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof Mob mob)) {
            return;
        }
        if (!MobRules.multiplierEnabled() || !MobRules.hasMultiplierRules()) {
            return;
        }

        double multiplier = MobRules.multiplier(mob.getType());
        if (multiplier == 1.0D) {
            return;
        }

        if (multiplier < 1.0D) {
            if (multiplier <= 0.0D) {
                logOnce(mob.getType(), "blocked every spawn, multiplier " + multiplier);
                event.setCanceled(true);
            } else if (mob.getRandom().nextDouble() > multiplier) {
                logOnce(mob.getType(), "dropped this spawn, multiplier " + multiplier);
                event.setCanceled(true);
            }
            return;
        }

        logOnce(mob.getType(), "spawning extra copies, multiplier " + multiplier);

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
     * Logs one line per entity type per config revision, so a player can see that (and what) the
     * rules did without flooding the log with one line per spawn event.
     */
    private static void logOnce(EntityType<?> type, String message) {
        if (MobRules.logOnce("multiplier:" + type)) {
            alexsaccuratemobspawn.LOGGER.info("Spawn multiplier for {}: {}", type, message);
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
