package com.Huziyang520.alexsaccuratemobspawn.mixin;

import com.Huziyang520.alexsaccuratemobspawn.spawn.NaturalSpawnHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Alex's Caves compatibility: dinosaur eggs.
 *
 * <p>{@code DinosaurEggBlock#spawnDinosaurs} hatches its dinosaurs with
 * {@code births.get().create(level)} followed by {@code level.addFreshEntity(...)} and <b>never calls
 * {@code Mob#finalizeSpawn}</b>, so such a spawn carries no spawn type at all and the probability
 * feature would silently ignore it (that is exactly why the primordial cave dinosaurs kept appearing
 * while, for example, trilocaris - which has no egg block - was blocked correctly).</p>
 *
 * <p>Eggs placed by world generation are marked {@code needs_player = true} by Alex's Caves'
 * {@code SubterranodonRoostFeature}, while eggs a player places or gets from breeding keep the default
 * {@code false}. Only the world generated ones are reported as natural, so a mob set to {@code 0.0}
 * really stops appearing in the caves while player made egg farms keep working.</p>
 *
 * <p>The {@code needs_player} property is read by name on purpose: this mixin never references an
 * Alex's Caves class, so it loads and applies cleanly whether or not that mod is installed (the mixin
 * config is not required, so a missing target is only logged).</p>
 */
@Mixin(targets = "com.github.alexmodguy.alexscaves.server.block.DinosaurEggBlock")
public abstract class AlexsCavesEggBlockMixin {

    @Inject(method = "spawnDinosaurs", at = @At("HEAD"), remap = false)
    private void aams$beginWorldGenHatch(Level level, BlockPos pos, BlockState state, CallbackInfo ci) {
        if (isWorldGenEgg(state)) {
            NaturalSpawnHandler.beginWorldgenSpawn();
        }
    }

    @Inject(method = "spawnDinosaurs", at = @At("RETURN"), remap = false)
    private void aams$endWorldGenHatch(Level level, BlockPos pos, BlockState state, CallbackInfo ci) {
        if (isWorldGenEgg(state)) {
            NaturalSpawnHandler.endWorldgenSpawn();
        }
    }

    private static boolean isWorldGenEgg(BlockState state) {
        for (Property<?> property : state.getProperties()) {
            if ("needs_player".equals(property.getName())) {
                return state.getValue(property) instanceof Boolean flag && flag;
            }
        }
        return false;
    }
}
