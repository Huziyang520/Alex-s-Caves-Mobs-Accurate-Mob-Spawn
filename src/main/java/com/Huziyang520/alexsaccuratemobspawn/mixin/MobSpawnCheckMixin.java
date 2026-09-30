package com.Huziyang520.alexsaccuratemobspawn.mixin;

import com.Huziyang520.alexsaccuratemobspawn.spawn.NaturalSpawnHandler;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The decisive hook for the probability feature: {@code Mob#checkSpawnRules} is the last check a spawner
 * performs right before it adds the mob to the world, and it is called <b>with the spawn type</b>.
 *
 * <p>Judging here, instead of only when the entity joins the level, is what makes the feature work for
 * spawns that never reach a join event on the same thread. Alex's Caves' cave creature burst is the prime
 * example: it runs inside world generation, calls
 * {@code mob.checkSpawnRules(level, MobSpawnType.CHUNK_GENERATION)} itself and then adds the mob, so
 * returning {@code false} here stops the mob from ever being created in the world (the burst simply
 * discards it and tries another position).</p>
 *
 * <p>Mobs that pass this check are remembered by the handler, so the join event does not roll the same
 * spawn a second time.</p>
 */
@Mixin(Mob.class)
public abstract class MobSpawnCheckMixin {

    @Inject(
            method = "checkSpawnRules(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/world/entity/MobSpawnType;)Z",
            at = @At("RETURN"),
            cancellable = true)
    private void aams$judgeProbabilityBeforeAdding(LevelAccessor level, MobSpawnType spawnType,
                                                   CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) {
            return;
        }
        if (NaturalSpawnHandler.shouldVoidBeforeAdding((Mob) (Object) this, level, spawnType)) {
            cir.setReturnValue(false);
        }
    }
}
