package com.Huziyang520.alexsaccuratemobspawn.config;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;
import com.Huziyang520.alexsaccuratemobspawn.alexsaccuratemobspawn;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * Self managed TOML configuration layer.
 *
 * <p>Uses the NightConfig library that ships with Forge. Deliberately does NOT use
 * {@code ModLoadingContext.registerConfig}/{@code ForgeConfigSpec}: a fixed spec cannot accept the
 * arbitrary extra files players are allowed to drop into the config folder, and the required file
 * names / folder layout cannot be expressed with the default Forge naming scheme.</p>
 *
 * <p>Layout: {@code config/&lt;modid&gt;/} holds {@code common.toml}, the four built in rule files and any
 * number of player authored {@code .toml} files. Every {@code .toml} other than {@code common.toml}
 * is parsed as a rule file; later files override earlier ones.</p>
 *
 * <p>Changes are picked up by a 2 second poll of file names and timestamps. Polling instead of a
 * WatchService on purpose: it also notices brand new or deleted files and it survives editors that
 * replace files by rename.</p>
 */
public final class ConfigStore {

    private static final long POLL_INTERVAL_MILLIS = 2000L;

    /** Small settle delay so a multi step save (truncate + write) is finished before reading. */
    private static final long RELOAD_DELAY_MILLIS = 300L;

    private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "alexsaccuratemobspawn-config");
        thread.setDaemon(true);
        return thread;
    });

    private static Map<String, Long> lastSnapshot = Map.of();
    private static boolean bootstrapped;

    private ConfigStore() {
    }

    // ------------------------------------------------------------------ startup

    public static void bootstrap() {
        if (bootstrapped) {
            return;
        }
        bootstrapped = true;
        try {
            Files.createDirectories(ConfigFiles.DIR);
        } catch (IOException e) {
            alexsaccuratemobspawn.LOGGER.error("Could not create config directory {}", ConfigFiles.DIR, e);
        }
        createMissingDefaults();
        LegacyConfigMigrator.migrate();
        reload("startup");
        lastSnapshot = snapshot();
        SCHEDULER.scheduleWithFixedDelay(ConfigStore::poll, POLL_INTERVAL_MILLIS, POLL_INTERVAL_MILLIS,
                TimeUnit.MILLISECONDS);
    }

    private static void createMissingDefaults() {
        copyTemplateIfMissing(ConfigFiles.of(ConfigFiles.COMMON), ConfigFiles.COMMON);
        for (String name : ConfigFiles.BUILTIN) {
            copyTemplateIfMissing(ConfigFiles.of(name), name);
        }
    }

    private static void copyTemplateIfMissing(Path target, String templateName) {
        if (Files.exists(target)) {
            return;
        }
        String resource = ConfigFiles.TEMPLATE_ROOT + templateName;
        try (InputStream in = ConfigStore.class.getResourceAsStream(resource)) {
            if (in == null) {
                alexsaccuratemobspawn.LOGGER.error("Bundled config template {} is missing from the jar", resource);
                return;
            }
            Files.createDirectories(target.getParent());
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            alexsaccuratemobspawn.LOGGER.info("Created default config {}", target);
        } catch (IOException e) {
            alexsaccuratemobspawn.LOGGER.error("Could not create default config {}", target, e);
        }
    }

    // ------------------------------------------------------------------ watching

    private static void poll() {
        try {
            Map<String, Long> current = snapshot();
            if (!current.equals(lastSnapshot)) {
                lastSnapshot = current;
                SCHEDULER.schedule(() -> reload("file change"), RELOAD_DELAY_MILLIS, TimeUnit.MILLISECONDS);
            }
        } catch (Throwable t) {
            alexsaccuratemobspawn.LOGGER.debug("Config poll failed", t);
        }
    }

    /** Maps every known config file name to its last modified time; used to detect any change. */
    private static Map<String, Long> snapshot() {
        Map<String, Long> map = new HashMap<>();
        for (Path file : allRuleFiles()) {
            map.put(file.getFileName().toString(), lastModified(file));
        }
        Path common = ConfigFiles.of(ConfigFiles.COMMON);
        if (Files.exists(common)) {
            map.put(ConfigFiles.COMMON, lastModified(common));
        }
        return map;
    }

    private static long lastModified(Path file) {
        try {
            return Files.getLastModifiedTime(file).toMillis();
        } catch (IOException e) {
            return -1L;
        }
    }

    // ------------------------------------------------------------------ loading

    /**
     * Re-reads every config file. On any failure the previously published tables are kept, so a half
     * written or broken file can never turn the mod into "everything spawns at defaults".
     */
    public static void reload(String reason) {
        Map<ResourceLocation, Double> multipliers = new LinkedHashMap<>();
        Map<ResourceLocation, Double> probabilities = new LinkedHashMap<>();
        List<String> warnings = new ArrayList<>();
        boolean[] switches = readCommon(warnings);

        for (Path file : allRuleFiles()) {
            readRuleFile(file, file.getFileName().toString(), multipliers, probabilities, warnings);
        }

        MobRules.publish(multipliers, probabilities, switches[0], switches[1], switches[2]);

        alexsaccuratemobspawn.LOGGER.info(
                "Config reloaded ({}): {} spawn settings, {} probability settings, switches[multiplier={}, probability={}, notice={}]",
                reason, multipliers.size(), probabilities.size(), switches[0], switches[1], switches[2]);
        for (String warning : warnings) {
            alexsaccuratemobspawn.LOGGER.warn("Config: {}", warning);
        }
    }

    /** Built in files in their fixed order, followed by player files sorted by name (they override). */
    private static List<Path> allRuleFiles() {
        List<Path> ordered = new ArrayList<>();
        for (String name : ConfigFiles.BUILTIN) {
            Path path = ConfigFiles.of(name);
            if (Files.exists(path)) {
                ordered.add(path);
            }
        }
        ordered.addAll(listExtraFiles());
        return ordered;
    }

    /** @return {enableMultiplier, enableProbability, showChatNotice} */
    private static boolean[] readCommon(List<String> warnings) {
        boolean enableMultiplier = true;
        boolean enableProbability = true;
        boolean showNotice = true;
        Path file = ConfigFiles.of(ConfigFiles.COMMON);
        if (!Files.exists(file)) {
            return new boolean[] { enableMultiplier, enableProbability, showNotice };
        }
        try (CommentedFileConfig cfg = CommentedFileConfig.builder(file).preserveInsertionOrder().build()) {
            cfg.load();
            enableMultiplier = readBoolean(cfg, "enableMultiplier", true, warnings);
            enableProbability = readBoolean(cfg, "enableProbability", true, warnings);
            showNotice = readBoolean(cfg, "showChatNotice", true, warnings);
        } catch (Exception e) {
            warnings.add("failed to read " + ConfigFiles.COMMON + " (" + e.getMessage() + "), using defaults");
        }
        return new boolean[] { enableMultiplier, enableProbability, showNotice };
    }

    private static boolean readBoolean(Config cfg, String key, boolean fallback, List<String> warnings) {
        Object raw = cfg.get(key);
        if (raw == null) {
            return fallback;
        }
        if (raw instanceof Boolean value) {
            return value;
        }
        warnings.add("common.toml: '" + key + "' should be true/false, got '" + raw + "'; using default " + fallback);
        return fallback;
    }

    private static void readRuleFile(Path file, String fileName,
                                     Map<ResourceLocation, Double> multipliers,
                                     Map<ResourceLocation, Double> probabilities,
                                     List<String> warnings) {
        try (CommentedFileConfig cfg = CommentedFileConfig.builder(file).preserveInsertionOrder().build()) {
            cfg.load();
            boolean probabilityFile = resolveKind(cfg, fileName, warnings);
            Object mobs = cfg.get("mobs");
            if (!(mobs instanceof Config table)) {
                warnings.add(fileName + ": missing [mobs] table, nothing loaded");
                return;
            }
            for (UnmodifiableConfig.Entry entry : table.entrySet()) {
                String rawKey = entry.getKey();
                ResourceLocation id = ResourceLocation.tryParse(rawKey);
                if (id == null) {
                    warnings.add(fileName + ": '" + rawKey + "' is not a valid entity id (expected namespace:path)");
                    continue;
                }
                Double value = toDouble(entry.getValue());
                if (value == null) {
                    warnings.add(fileName + ": '" + rawKey + "' has a non numeric value '" + entry.getValue() + "'");
                    continue;
                }
                double min = probabilityFile ? MobRules.MIN_PROBABILITY : MobRules.MIN_MULTIPLIER;
                double max = probabilityFile ? MobRules.MAX_PROBABILITY : MobRules.MAX_MULTIPLIER;
                if (value < min || value > max) {
                    double clamped = Math.max(min, Math.min(max, value));
                    warnings.add(fileName + ": '" + rawKey + "' = " + value + " is outside " + min + "~" + max
                            + ", clamped to " + clamped);
                    value = clamped;
                }
                if (probabilityFile) {
                    probabilities.put(id, value);
                } else {
                    multipliers.put(id, value);
                }
            }
        } catch (Exception e) {
            warnings.add(fileName + ": failed to parse (" + e.getMessage() + "); previous values kept for this file");
        }
    }

    /**
     * Decides whether a file holds multipliers or probabilities.
     * Order: explicit {@code type} key, then the file name, then multiplier as the safe default.
     */
    private static boolean resolveKind(Config cfg, String fileName, List<String> warnings) {
        if (ConfigFiles.BUILTIN_MULTIPLIER.contains(fileName)) {
            return false;
        }
        if (ConfigFiles.BUILTIN.contains(fileName)) {
            return true;
        }
        Object declared = cfg.get("type");
        if (declared != null) {
            String text = String.valueOf(declared).trim().toLowerCase();
            if (text.startsWith("prob")) {
                return true;
            }
            if (text.startsWith("mult")) {
                return false;
            }
            warnings.add(fileName + ": unknown type '" + declared + "', expected 'multiplier' or 'probability'");
        }
        String lower = fileName.toLowerCase();
        if (lower.contains("probability") || lower.contains("prob")) {
            return true;
        }
        if (!lower.contains("multiplier") && !lower.contains("mult")) {
            warnings.add(fileName + ": no 'type' key and no multiplier/probability hint in the name,"
                    + " treating it as a multiplier file");
        }
        return false;
    }

    @Nullable
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

    private static List<Path> listExtraFiles() {
        List<Path> extras = new ArrayList<>();
        if (!Files.isDirectory(ConfigFiles.DIR)) {
            return extras;
        }
        try (Stream<Path> stream = Files.list(ConfigFiles.DIR)) {
            stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase().endsWith(".toml"))
                    .filter(path -> !ConfigFiles.BUILTIN.contains(path.getFileName().toString()))
                    .filter(path -> !ConfigFiles.COMMON.equals(path.getFileName().toString()))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .forEach(extras::add);
        } catch (IOException e) {
            alexsaccuratemobspawn.LOGGER.warn("Could not list config directory {}", ConfigFiles.DIR, e);
        }
        return extras;
    }

    /** Used by the migrator to open an existing file for editing while keeping its comments. */
    static CommentedFileConfig openForEdit(Path file) {
        return CommentedFileConfig.builder(file)
                .preserveInsertionOrder()
                .writingMode(WritingMode.REPLACE)
                .build();
    }
}
