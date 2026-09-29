package com.Huziyang520.alexsaccuratemobspawn;

import com.Huziyang520.alexsaccuratemobspawn.config.ConfigStore;
import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Alex's Accurate Mob Spawn
 *
 * <p>Config driven spawn control for Minecraft 1.20.1 / Forge.
 * All configuration lives in {@code config/alexsaccuratemobspawn/} and is written by this mod
 * (no ForgeConfigSpec), so players may drop any additional {@code .toml} file into that folder
 * to control the spawn multiplier / probability of any mob, including vanilla and other mods.</p>
 */
@Mod(alexsaccuratemobspawn.MOD_ID)
public class alexsaccuratemobspawn {

    public static final String MOD_ID = "alexsaccuratemobspawn";
    public static final Logger LOGGER = LogUtils.getLogger();

    public alexsaccuratemobspawn() {
        // Loads config, creates missing files from templates, migrates the legacy config and
        // registers file watchers for hot reload. Must run before any spawn event can fire.
        ConfigStore.bootstrap();
    }
}
