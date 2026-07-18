package com.Huziyang520.alexscavessaccuratemobspawn;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(alexscavessaccuratemobspawn.MOD_ID)
public class alexscavessaccuratemobspawn
{
    public static final String MOD_ID = "alexscavessaccuratemobspawn";
    public static final Logger LOGGER = LogUtils.getLogger();

    public alexscavessaccuratemobspawn(FMLJavaModLoadingContext context)
    {
        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        LOGGER.info("Alex's Caves Accurate Mob Spawn loaded! Configure spawn rates in: config/{}", MOD_ID + "-common.toml");
    }
}