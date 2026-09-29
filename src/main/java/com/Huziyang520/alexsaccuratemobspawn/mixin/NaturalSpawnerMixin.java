package com.Huziyang520.alexsaccuratemobspawn.mixin;

import com.Huziyang520.alexsaccuratemobspawn.spawn.SpawnWeightScaler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

/**
 * Runtime half of the probability feature.
 *
 * <p>Vanilla 1.20.1 assembles the list of mobs a position may spawn with
 * {@code NaturalSpawner#mobsAt}: it returns either the nether fortress list, the biome list from
 * {@code ChunkGenerator#getMobsAt} or a structure's {@code spawn_override} list. Scaling the return
 * value therefore covers every runtime spawn, including structure overrides.</p>
 *
 * <p>Biome lists and world generation lists are already handled by
 * {@link MobSpawnSettingsMixin}; {@link SpawnWeightScaler#isProduced} makes sure such a list is not
 * scaled twice.</p>
 */
@Mixin(NaturalSpawner.class)
public abstract class NaturalSpawnerMixin {

    @Inject(method = "mobsAt", at = @At("RETURN"), cancellable = true, remap = true)
    private static void aams$scaleRuntimeSpawnList(ServerLevel level, StructureManager structureManager,
                                                    ChunkGenerator generator, MobCategory category, BlockPos pos,
                                                    @Nullable Holder<Biome> biome,
                                                    CallbackInfoReturnable<WeightedRandomList<MobSpawnSettings.SpawnerData>> cir) {
        WeightedRandomList<MobSpawnSettings.SpawnerData> original = cir.getReturnValue();
        if (original != null && !SpawnWeightScaler.isProduced(original)) {
            cir.setReturnValue(SpawnWeightScaler.scale(original));
        }
    }
}
