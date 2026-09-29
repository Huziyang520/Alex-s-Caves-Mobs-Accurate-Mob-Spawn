package com.Huziyang520.alexsaccuratemobspawn.mixin;

import com.Huziyang520.alexsaccuratemobspawn.spawn.SpawnWeightScaler;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.MobSpawnSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Spawn list half of the probability feature.
 *
 * <p>{@code MobSpawnSettings#getMobs} is the single source of every biome spawn list. Hooking the
 * method itself (instead of one of its call sites) covers the runtime path
 * ({@code ChunkGenerator#getMobsAt}), the world generation path
 * ({@code NaturalSpawner#spawnMobsForChunkGeneration}) and the extra burst Alex's Caves performs
 * during chunk generation (its own mixin calls {@code getMobs(CAVE_CREATURE)} directly), without
 * depending on the order in which other mods inject code.</p>
 */
@Mixin(MobSpawnSettings.class)
public abstract class MobSpawnSettingsMixin {

    @Inject(method = "getMobs", at = @At("RETURN"), cancellable = true, remap = true)
    private void aams$scaleBiomeSpawnList(MobCategory category,
                                          CallbackInfoReturnable<WeightedRandomList<MobSpawnSettings.SpawnerData>> cir) {
        WeightedRandomList<MobSpawnSettings.SpawnerData> original = cir.getReturnValue();
        if (original != null) {
            cir.setReturnValue(SpawnWeightScaler.scale(original));
        }
    }
}
