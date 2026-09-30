package com.Huziyang520.alexsaccuratemobspawn.mixin;

import com.Huziyang520.alexsaccuratemobspawn.spawn.NaturalSpawnHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Records how a mob was spawned, which is what lets the probability feature apply to natural spawns
 * only.
 *
 * <p>Forge 1.20.1 does <b>not</b> fire {@code MobSpawnEvent.FinalizeSpawn} for regular spawns: that
 * event is only posted by {@code BaseSpawner} (and always with {@code MobSpawnType.SPAWNER}), so it can
 * never tell a natural spawn apart. Every spawn path, however - the vanilla spawn cycle, world
 * generation, spawn eggs, commands, mob spawners, conversions and the custom spawns of other mods -
 * calls {@code Mob#finalizeSpawn} and passes the {@link MobSpawnType}. Injecting here is therefore the
 * one place that sees all of them, including:</p>
 *
 * <ul>
 *   <li>Alex's Caves' cave creature burst ({@code CHUNK_GENERATION}) - it runs its own loop instead of
 *       the vanilla spawn cycle, so no vanilla hook would ever see it;</li>
 *   <li>Alex's Caves' amber monolith and summoning paths;</li>
 *   <li>Alex's Mobs' beached whale spawner ({@code SPAWNER}) and lightning conversion
 *       ({@code CONVERSION}).</li>
 * </ul>
 *
 * <p>The mixin config is marked {@code required: false} with {@code defaultRequire: 0} on purpose: if
 * this injection ever fails to apply, Mixin only logs it and the mod keeps working through Forge's
 * {@code MobSpawnEvent.PositionCheck} source instead of crashing the game.</p>
 */
@Mixin(Mob.class)
public abstract class MobSpawnOriginMixin {

    @Inject(method = "finalizeSpawn", at = @At("HEAD"), remap = true)
    private void aams$recordSpawnOrigin(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType spawnType, SpawnGroupData spawnGroupData, CompoundTag dataTag,
                                        CallbackInfoReturnable<SpawnGroupData> cir) {
        NaturalSpawnHandler.recordOrigin((Mob) (Object) this, spawnType);
    }
}
