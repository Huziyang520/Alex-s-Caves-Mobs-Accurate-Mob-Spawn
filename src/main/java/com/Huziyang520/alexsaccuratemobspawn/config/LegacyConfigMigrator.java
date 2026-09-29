package com.Huziyang520.alexsaccuratemobspawn.config;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.Huziyang520.alexsaccuratemobspawn.alexsaccuratemobspawn;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Migrates the configuration of the previous mod id.
 *
 * <p>The old version stored 43 spawn multipliers with ForgeConfigSpec in
 * {@code config/alexscavessaccuratemobspawn-common.toml} using the layout
 * {@code [spawn_multipliers.<cave group>] <name> = <value>}. The old
 * {@code Config#MULTIPLIER_MAP} mapped each of those names to {@code alexscaves:<same name>}, so the
 * migration is an identity mapping of the key names.</p>
 *
 * <p>The legacy file is deleted only after the values were written and saved successfully; a file
 * that cannot be parsed is left untouched so nothing can be lost.</p>
 */
public final class LegacyConfigMigrator {

    private LegacyConfigMigrator() {
    }

    public static void migrate() {
        Path legacy = FMLPaths.CONFIGDIR.get().resolve(ConfigFiles.LEGACY_FILE);
        if (!Files.exists(legacy)) {
            return;
        }
        Path target = ConfigFiles.of(ConfigFiles.AC_MULTIPLIER);
        if (!Files.exists(target)) {
            alexsaccuratemobspawn.LOGGER.warn("Legacy config {} is present but {} does not exist, skipping migration",
                    legacy, target);
            return;
        }

        Map<String, Double> values = new LinkedHashMap<>();
        try (CommentedFileConfig in = CommentedFileConfig.builder(legacy).preserveInsertionOrder().build()) {
            in.load();
            collect(in, values);
        } catch (Exception e) {
            alexsaccuratemobspawn.LOGGER.error("Could not read legacy config {}, leaving it untouched", legacy, e);
            return;
        }

        if (values.isEmpty()) {
            alexsaccuratemobspawn.LOGGER.warn("Legacy config {} holds no usable entries; nothing migrated", legacy);
            return;
        }

        try (CommentedFileConfig out = ConfigStore.openForEdit(target)) {
            out.load();
            for (Map.Entry<String, Double> entry : values.entrySet()) {
                out.set(List.of("mobs", entry.getKey()), entry.getValue());
            }
            out.save();
        } catch (Exception e) {
            alexsaccuratemobspawn.LOGGER.error("Could not write migrated values into {}, legacy config kept", target, e);
            return;
        }

        alexsaccuratemobspawn.LOGGER.info("Migrated {} spawn multipliers from {} into {}", values.size(), legacy, target);
        values.forEach((key, value) ->
                alexsaccuratemobspawn.LOGGER.info("  migrated {} = {}", key, value));

        try {
            Files.delete(legacy);
            alexsaccuratemobspawn.LOGGER.info("Removed migrated legacy config {}", legacy);
        } catch (IOException e) {
            alexsaccuratemobspawn.LOGGER.warn("Migrated {} but could not delete it; you may delete it manually", legacy, e);
        }
    }

    private static void collect(Config root, Map<String, Double> out) {
        Object section = root.get("spawn_multipliers");
        if (!(section instanceof Config groups)) {
            // Tolerate a flat layout as well: <name> = <value> directly under spawn_multipliers.
            return;
        }
        for (UnmodifiableConfig.Entry group : groups.entrySet()) {
            if (group.getValue() instanceof Config entries) {
                for (UnmodifiableConfig.Entry entry : entries.entrySet()) {
                    Double value = toDouble(entry.getValue());
                    if (value != null) {
                        out.put("alexscaves:" + entry.getKey(), value);
                    } else {
                        alexsaccuratemobspawn.LOGGER.warn("Legacy config entry {} = {} is not a number, skipped",
                                entry.getKey(), entry.getValue());
                    }
                }
            } else {
                Double value = toDouble(group.getValue());
                if (value != null) {
                    out.put("alexscaves:" + group.getKey(), value);
                }
            }
        }
    }

    private static Double toDouble(Object raw) {
        if (raw instanceof Number number) {
            double value = number.doubleValue();
            return Double.isFinite(value) ? value : null;
        }
        if (raw instanceof String text) {
            try {
                return Double.parseDouble(text.trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}
