package com.Huziyang520.alexscavessaccuratemobspawn;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = alexscavessaccuratemobspawn.MOD_ID)
public class SpawnEventHandler {

    private static final String ALEXS_CAVES_NAMESPACE = "alexscaves";

    /**
     * Prevents infinite recursion when spawning extra copies for multiplier > 1.0.
     */
    private static final ThreadLocal<Boolean> SPAWNING_EXTRA = ThreadLocal.withInitial(() -> false);

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }

        if (!(event.getEntity() instanceof Mob mob)) {
            return;
        }

        ResourceLocation entityId = ForgeRegistries.ENTITY_TYPES.getKey(mob.getType());
        if (entityId == null || !entityId.getNamespace().equals(ALEXS_CAVES_NAMESPACE)) {
            return;
        }

        double multiplier = Config.getMultiplier(mob.getType());
        if (multiplier == 1.0D) {
            return;
        }

        // === DECREASE CASE (multiplier < 1.0) ===
        if (multiplier < 1.0D) {
            if (multiplier <= 0.0D) {
                event.setCanceled(true);
            } else if (mob.getRandom().nextDouble() > multiplier) {
                event.setCanceled(true);
            }
            return;
        }

        // === INCREASE CASE (multiplier > 1.0) ===
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
                totalCopies = Math.min(totalCopies, 50);
                spawnExtraCopies(serverLevel, mob, totalCopies);
            } finally {
                SPAWNING_EXTRA.set(false);
            }
        }
    }

    private static void spawnExtraCopies(ServerLevel level, Mob originalMob, int count) {
        if (count <= 0) return;

        EntityType<?> type = originalMob.getType();
        net.minecraft.core.BlockPos pos = originalMob.blockPosition();

        for (int i = 0; i < count; i++) {
            Entity newEntity = type.create(level);
            if (!(newEntity instanceof Mob newMob)) {
                newEntity.discard();
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