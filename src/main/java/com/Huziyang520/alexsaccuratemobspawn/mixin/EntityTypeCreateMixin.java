package com.Huziyang520.alexsaccuratemobspawn.mixin;

import com.Huziyang520.alexsaccuratemobspawn.spawn.NaturalSpawnHandler;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Stamps entities that are created inside a known world generation window (Alex's Caves' roost feature
 * and its world generated dinosaur eggs, see {@code NaturalSpawnHandler#beginWorldgenSpawn}).
 *
 * <p>Those spawns never call {@code Mob#finalizeSpawn} and the entity can even join the level much later,
 * on another thread, when the chunk is promoted. Stamping the entity object at creation time keeps the
 * knowledge "this one came from world generation" without depending on the thread or the timing of the
 * join event.</p>
 */
@Mixin(EntityType.class)
public abstract class EntityTypeCreateMixin {

    @Inject(
            method = "create(Lnet/minecraft/world/level/Level;)Lnet/minecraft/world/entity/Entity;",
            at = @At("RETURN"))
    private void aams$stampWorldGenCreation(Level level, CallbackInfoReturnable<Entity> cir) {
        NaturalSpawnHandler.recordWorldgenCreated(cir.getReturnValue());
    }
}
