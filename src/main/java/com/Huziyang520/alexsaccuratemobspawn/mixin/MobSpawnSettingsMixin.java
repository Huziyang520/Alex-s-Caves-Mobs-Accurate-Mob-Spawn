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
 * Spawn frequency boosts (probabilities above {@code 1.0}) for every biome spawn list.
 *
 * <p>{@code getMobs} is the single source of every list vanilla and other mods use, so hooking it
 * covers the runtime spawn cycle, the world generation batch and Alex's Caves' extra burst without
 * depending on the order in which other mods inject code.</p>
 *
 * <p>The important part is {@link SpawnWeightScaler}: a boost is only handed out when the very same
 * object can be returned for every later call of the same attempt, because vanilla revalidates the
 * chosen {@code SpawnerData} by reference. When that cannot be guaranteed the original list is kept, so
 * this hook can never stop mobs from spawning.</p>
 */
@Mixin(MobSpawnSettings.class)
public abstract class MobSpawnSettingsMixin {

    @Inject(method = "getMobs", at = @At("RETURN"), cancellable = true, remap = true)
    private void aams$applyFrequencyBoosts(MobCategory category,
                                           CallbackInfoReturnable<WeightedRandomList<MobSpawnSettings.SpawnerData>> cir) {
        WeightedRandomList<MobSpawnSettings.SpawnerData> original = cir.getReturnValue();
        if (original != null) {
            cir.setReturnValue(SpawnWeightScaler.apply(original));
        }
    }
}
