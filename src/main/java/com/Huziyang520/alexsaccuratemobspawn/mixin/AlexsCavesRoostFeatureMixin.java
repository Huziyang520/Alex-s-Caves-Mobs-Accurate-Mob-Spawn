package com.Huziyang520.alexsaccuratemobspawn.mixin;

import com.Huziyang520.alexsaccuratemobspawn.spawn.NaturalSpawnHandler;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Alex's Caves compatibility: the subterranodon roost feature.
 *
 * <p>{@code SubterranodonRoostFeature#place} creates an adult subterranodon with
 * {@code create(...)} + {@code addFreshEntity(...)} and, next to it, places a world generated egg with
 * {@code needs_player = true}. No {@code Mob#finalizeSpawn} is called anywhere, so the spawn carries no
 * spawn type and the probability feature would ignore it.</p>
 *
 * <p>The whole feature runs during chunk generation, so everything it spawns is reported as a natural
 * world generation spawn for the duration of that call. The flag is thread local, so nothing else is
 * affected.</p>
 */
@Mixin(targets = "com.github.alexmodguy.alexscaves.server.level.feature.SubterranodonRoostFeature")
public abstract class AlexsCavesRoostFeatureMixin {

    @Inject(method = "place", at = @At("HEAD"), remap = false)
    private void aams$beginRoost(FeaturePlaceContext<?> context, CallbackInfoReturnable<Boolean> cir) {
        NaturalSpawnHandler.beginWorldgenSpawn();
    }

    @Inject(method = "place", at = @At("RETURN"), remap = false)
    private void aams$endRoost(FeaturePlaceContext<?> context, CallbackInfoReturnable<Boolean> cir) {
        NaturalSpawnHandler.endWorldgenSpawn();
    }
}
