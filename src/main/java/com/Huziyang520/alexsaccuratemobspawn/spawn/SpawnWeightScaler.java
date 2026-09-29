package com.Huziyang520.alexsaccuratemobspawn.spawn;

import com.Huziyang520.alexsaccuratemobspawn.alexsaccuratemobspawn;
import com.Huziyang520.alexsaccuratemobspawn.config.MobRules;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.level.biome.MobSpawnSettings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Applies the probability rules to a vanilla spawn list.
 *
 * <p>Probability means "spawn frequency": instead of blocking an entity after it was created, the
 * weight of the matching entries inside the {@link MobSpawnSettings.SpawnerData} list that vanilla
 * uses to pick a mob is scaled. A weight of 0 removes the entry from the list, which is how
 * probability {@code 0.0} disables a mob.</p>
 *
 * <p>All entries are multiplied by the same {@link #SCALE} factor before rounding, which keeps the
 * relative odds between unconfigured entries intact and still gives {@code 1/SCALE} resolution for
 * probabilities below 1.</p>
 *
 * <p>The vanilla lists are shared, cached instances, so they are never mutated: a brand new list with
 * brand new {@code SpawnerData} objects is built instead.</p>
 */
public final class SpawnWeightScaler {

    /** Extra resolution for probabilities below 1. 1/256 = 0.4%. */
    private static final int SCALE = 256;

    /** Keep the summed weight of a rebuilt list well below {@link Integer#MAX_VALUE}. */
    private static final long MAX_TOTAL_WEIGHT = 1L << 30;

    private static final int MAX_CACHE_ENTRIES = 8192;

    private static final Map<WeightedRandomList<MobSpawnSettings.SpawnerData>, Cached> CACHE =
            Collections.synchronizedMap(new IdentityHashMap<>());

    /** Identity set of the lists this class produced, so they are never scaled a second time. */
    private static final Set<WeightedRandomList<MobSpawnSettings.SpawnerData>> PRODUCED =
            Collections.newSetFromMap(Collections.synchronizedMap(new IdentityHashMap<>()));

    private static volatile int cachedRevision = -1;

    private SpawnWeightScaler() {
    }

    /**
     * True when {@code list} was produced by {@link #scale} and therefore already carries the
     * probability rules. Used by the {@code NaturalSpawner} hook to avoid scaling the biome list a
     * second time after the {@code MobSpawnSettings} hook already handled it.
     */
    public static boolean isProduced(WeightedRandomList<MobSpawnSettings.SpawnerData> list) {
        return PRODUCED.contains(list);
    }

    public static WeightedRandomList<MobSpawnSettings.SpawnerData> scale(
            WeightedRandomList<MobSpawnSettings.SpawnerData> original) {
        if (original == null || original.isEmpty() || !MobRules.hasProbabilityRules()) {
            return original;
        }

        int revision = MobRules.revision();
        if (revision != cachedRevision) {
            CACHE.clear();
            PRODUCED.clear();
            cachedRevision = revision;
        }

        Cached cached = CACHE.get(original);
        if (cached != null) {
            return cached.scaled;
        }

        List<MobSpawnSettings.SpawnerData> entries = original.unwrap();
        boolean configured = false;
        for (MobSpawnSettings.SpawnerData data : entries) {
            if (MobRules.probability(data.type) != 1.0D) {
                configured = true;
                break;
            }
        }

        WeightedRandomList<MobSpawnSettings.SpawnerData> result = original;
        if (configured) {
            WeightedRandomList<MobSpawnSettings.SpawnerData> rebuilt = rebuild(entries, SCALE);
            if (rebuilt == null) {
                // Weights would overflow int: fall back to unscaled rounding.
                rebuilt = rebuild(entries, 1);
            }
            if (rebuilt == null) {
                alexsaccuratemobspawn.LOGGER.warn("Spawn weights overflowed, probability rules skipped for one list");
            } else {
                result = rebuilt;
            }
        }

        if (CACHE.size() < MAX_CACHE_ENTRIES) {
            CACHE.put(original, new Cached(revision, result));
        }
        if (result != original && PRODUCED.size() < MAX_CACHE_ENTRIES) {
            PRODUCED.add(result);
        }
        return result;
    }

    /** @return the rebuilt list, or {@code null} if the total weight would overflow. */
    private static WeightedRandomList<MobSpawnSettings.SpawnerData> rebuild(
            List<MobSpawnSettings.SpawnerData> entries, int scale) {
        List<MobSpawnSettings.SpawnerData> out = new ArrayList<>(entries.size());
        long total = 0L;
        for (MobSpawnSettings.SpawnerData data : entries) {
            double probability = MobRules.probability(data.type);
            if (probability <= 0.0D) {
                continue;
            }
            long weight = Math.round((double) data.getWeight().asInt() * probability * (double) scale);
            if (weight < 1L) {
                weight = 1L;
            }
            total += weight;
            if (total > MAX_TOTAL_WEIGHT) {
                return null;
            }
            out.add(new MobSpawnSettings.SpawnerData(data.type, (int) weight, data.minCount, data.maxCount));
        }
        return WeightedRandomList.create(out);
    }

    private record Cached(int revision, WeightedRandomList<MobSpawnSettings.SpawnerData> scaled) {
    }
}
