package com.Huziyang520.alexsaccuratemobspawn.config;

import com.Huziyang520.alexsaccuratemobspawn.alexsaccuratemobspawn;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;
import java.util.List;

/**
 * Every path and file name used by the config layer.
 *
 * <p>All runtime configuration lives in {@code config/<modid>/}. Built in file names are fixed and
 * must not be renamed: players may add arbitrary extra {@code .toml} files next to them.</p>
 */
public final class ConfigFiles {

    /** Runtime config folder: {@code config/alexsaccuratemobspawn/}. */
    public static final Path DIR = FMLPaths.CONFIGDIR.get().resolve(alexsaccuratemobspawn.MOD_ID);

    /** Global switches + chat notice toggle. */
    public static final String COMMON = "common.toml";

    public static final String AC_MULTIPLIER = "alexscavessaccuratemobspawnmultiplier.toml";
    public static final String AM_MULTIPLIER = "alexsmobssaccuratemobspawnmultiplier.toml";
    public static final String AC_PROBABILITY = "alexscavessaccuratemobspawnprobability.toml";
    public static final String AM_PROBABILITY = "alexsmobssaccuratemobspawnprobability.toml";

    /** Load order of the built in files; extra player files are loaded afterwards (they override). */
    public static final List<String> BUILTIN = List.of(AC_MULTIPLIER, AM_MULTIPLIER, AC_PROBABILITY, AM_PROBABILITY);

    /** Built in files that hold spawn multipliers. */
    public static final List<String> BUILTIN_MULTIPLIER = List.of(AC_MULTIPLIER, AM_MULTIPLIER);

    /** Legacy ForgeConfigSpec file of the previous mod id, migrated then deleted. */
    public static final String LEGACY_FILE = "alexscavessaccuratemobspawn-common.toml";

    /** Classpath root of the bundled default templates (inside the mod jar). */
    public static final String TEMPLATE_ROOT = "/assets/" + alexsaccuratemobspawn.MOD_ID + "/defaults/";

    private ConfigFiles() {
    }

    public static Path of(String fileName) {
        return DIR.resolve(fileName);
    }
}
