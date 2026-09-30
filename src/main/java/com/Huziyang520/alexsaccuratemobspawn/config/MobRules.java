package com.Huziyang520.alexsaccuratemobspawn.config;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The live rule table.
 *
 * <p>Written by {@link ConfigStore} on (re)load, read from the spawn hooks. Values are looked up by
 * {@link ResourceLocation} so that unloaded / unregistered entity types behave like "not configured".
 * This class is thread safe: the maps are replaced as a whole and readers only ever see a complete
 * snapshot.</p>
 */
public final class MobRules {

    /** Accepted range for spawn multipliers (see the multiplier config files). */
    public static final double MIN_MULTIPLIER = 0.0D;
    public static final double MAX_MULTIPLIER = 10.0D;

    /** Accepted range for spawn probabilities (see the probability config files). */
    public static final double MIN_PROBABILITY = 0.0D;
    public static final double MAX_PROBABILITY = 100.0D;

    private static volatile Map<ResourceLocation, Double> multipliers = Collections.emptyMap();
    private static volatile Map<ResourceLocation, Double> probabilities = Collections.emptyMap();

    private static volatile boolean multiplierEnabled = true;
    private static volatile boolean probabilityEnabled = true;
    private static volatile boolean showChatNotice = true;

    /** Bumped on every successful reload; used to invalidate caches. */
    private static volatile int revision = 0;

    /** Keys already logged since the last reload; keeps the runtime log bounded. */
    private static final Set<String> LOGGED = ConcurrentHashMap.newKeySet();
    private static volatile int loggedRevision = -1;

    private MobRules() {
    }

    /** Package private: only {@link ConfigStore} publishes new tables. */
    static void publish(Map<ResourceLocation, Double> newMultipliers, Map<ResourceLocation, Double> newProbabilities,
                        boolean enableMultiplier, boolean enableProbability, boolean notice) {
        multipliers = Map.copyOf(newMultipliers);
        probabilities = Map.copyOf(newProbabilities);
        multiplierEnabled = enableMultiplier;
        probabilityEnabled = enableProbability;
        showChatNotice = notice;
        revision++;
    }

    public static double multiplier(EntityType<?> type) {
        if (!multiplierEnabled) {
            return 1.0D;
        }
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(type);
        if (id == null) {
            return 1.0D;
        }
        Double value = multipliers.get(id);
        return value == null ? 1.0D : value;
    }

    public static double probability(EntityType<?> type) {
        if (!probabilityEnabled) {
            return 1.0D;
        }
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(type);
        if (id == null) {
            return 1.0D;
        }
        Double value = probabilities.get(id);
        return value == null ? 1.0D : value;
    }

    /** True when at least one probability rule is configured; lets hot paths skip all work. */
    public static boolean hasProbabilityRules() {
        return probabilityEnabled && !probabilities.isEmpty();
    }

    public static boolean hasMultiplierRules() {
        return multiplierEnabled && !multipliers.isEmpty();
    }

    public static boolean multiplierEnabled() {
        return multiplierEnabled;
    }

    public static boolean probabilityEnabled() {
        return probabilityEnabled;
    }

    public static boolean showChatNotice() {
        return showChatNotice;
    }

    public static int revision() {
        return revision;
    }

    /**
     * True the first time a key is seen after the last reload. Used so the runtime handlers can
     * report what they did (proof the configuration is live) without logging once per spawn event.
     */
    public static boolean logOnce(String key) {
        if (revision != loggedRevision) {
            LOGGED.clear();
            loggedRevision = revision;
        }
        return LOGGED.add(key);
    }
}
