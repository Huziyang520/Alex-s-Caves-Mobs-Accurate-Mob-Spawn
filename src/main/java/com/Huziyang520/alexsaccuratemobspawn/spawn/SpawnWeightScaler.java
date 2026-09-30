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

/**
 * Spawn frequency boosts: probabilities above {@code 1.0} are applied by scaling the weights of the
 * biome's spawn list, because the vanilla spawn cycle picks the mob from that weighted list.
 *
 * <p>Probabilities below {@code 1.0} are deliberately NOT handled here - they are enforced once per
 * spawn attempt by {@link NaturalSpawnHandler}, which leaves the vanilla lists completely untouched for
 * the common "reduce or disable" case.</p>
 *
 * <h2>Why the identity contract is the whole ball game</h2>
 * Vanilla revalidates the mob it just picked with {@code mobsAt(...).unwrap().contains(data)}, and
 * {@code SpawnerData} has no {@code equals}, so that is pure reference equality. A rebuilt list is only
 * safe if the biome hands out <b>the same object</b> to both the selection and the revalidation inside
 * one spawn attempt. Therefore:
 * <ul>
 *   <li>the rebuilt list is <b>stored in the cache before it is returned</b>, so the second call of the
 *       same attempt already hits the cache and sees the identical object;</li>
 *   <li>the cache is keyed by the identity of the vanilla list and is only cleared when the
 *       configuration revision changes, which happens outside the hot path;</li>
 *   <li>whenever the cache cannot be used (bound reached, weights would overflow), the <b>original
 *       vanilla list is returned</b>. The worst case is then "the boost does not apply", never
 *       "mobs stop spawning".</li>
 * </ul>
 */
public final class SpawnWeightScaler {

    /** Extra resolution for fractional boosts. 1/256 = 0.4%. */
    private static final int SCALE = 256;

    /** Keep the summed weight of a rebuilt list well below {@link Integer#MAX_VALUE}. */
    private static final long MAX_TOTAL_WEIGHT = 1L << 30;

    /**
     * Only a few hundred distinct lists exist (one per biome and category, plus structure overrides),
     * so this bound is never reached in practice. If it ever is, boosts are switched off rather than
     * handing vanilla a list it cannot revalidate.
     */
    private static final int MAX_CACHE_ENTRIES = 8192;

    private static final Map<WeightedRandomList<MobSpawnSettings.SpawnerData>, WeightedRandomList<MobSpawnSettings.SpawnerData>> CACHE =
            Collections.synchronizedMap(new IdentityHashMap<>());

    private static volatile int cachedRevision = -1;

    /** Set when the cache became unusable; boosts are then skipped instead of rebuilt per call. */
    private static volatile boolean disabled;

    private SpawnWeightScaler() {
    }

    /**
     * @return the list vanilla should use: the boosted list (always the same object for the same input
     *     and revision), or {@code original} when nothing has to change or the boost cannot be applied.
     */
    public static WeightedRandomList<MobSpawnSettings.SpawnerData> apply(
            WeightedRandomList<MobSpawnSettings.SpawnerData> original) {
        if (original == null || original.isEmpty() || !MobRules.hasFrequencyBoosts() || disabled) {
            return original;
        }

        int revision = MobRules.revision();
        if (revision != cachedRevision) {
            CACHE.clear();
            cachedRevision = revision;
        }

        WeightedRandomList<MobSpawnSettings.SpawnerData> cached = CACHE.get(original);
        if (cached != null) {
            return cached;
        }

        WeightedRandomList<MobSpawnSettings.SpawnerData> boosted = boost(original.unwrap());
        if (boosted == null) {
            // No rule above 1.0 in this list: remember that and keep the identity untouched.
            remember(original, original);
            return original;
        }

        if (!remember(original, boosted)) {
            // We cannot promise a stable object for this list, so stay with vanilla behaviour.
            disabled = true;
            alexsaccuratemobspawn.LOGGER.warn(
                    "Spawn frequency boosts are disabled: the spawn list cache reached its bound of {} entries",
                    MAX_CACHE_ENTRIES);
            return original;
        }

        if (MobRules.logOnce("boost:" + original.unwrap().size())) {
            alexsaccuratemobspawn.LOGGER.info(
                    "Applied spawn frequency boosts (probability > 1) to a spawn list of {} entries",
                    original.unwrap().size());
        }
        return boosted;
    }

    /** @return true when the mapping could be stored. */
    private static boolean remember(WeightedRandomList<MobSpawnSettings.SpawnerData> original,
                                    WeightedRandomList<MobSpawnSettings.SpawnerData> scaled) {
        if (CACHE.size() >= MAX_CACHE_ENTRIES) {
            return false;
        }
        CACHE.put(original, scaled);
        return true;
    }

    /**
     * @return a new list whose entries carry the boosted weights, or {@code null} when no entry is
     *     above {@code 1.0} or the weights would overflow.
     */
    private static WeightedRandomList<MobSpawnSettings.SpawnerData> boost(
            List<MobSpawnSettings.SpawnerData> entries) {
        boolean boosted = false;
        for (MobSpawnSettings.SpawnerData data : entries) {
            if (MobRules.probability(data.type) > 1.0D) {
                boosted = true;
                break;
            }
        }
        if (!boosted) {
            return null;
        }

        WeightedRandomList<MobSpawnSettings.SpawnerData> scaled = build(entries, SCALE);
        if (scaled == null) {
            // Weights would overflow int: retry without the extra resolution.
            scaled = build(entries, 1);
        }
        return scaled;
    }

    /** @return the rebuilt list, or {@code null} if the total weight would overflow. */
    private static WeightedRandomList<MobSpawnSettings.SpawnerData> build(
            List<MobSpawnSettings.SpawnerData> entries, int scale) {
        List<MobSpawnSettings.SpawnerData> out = new ArrayList<>(entries.size());
        long total = 0L;
        for (MobSpawnSettings.SpawnerData data : entries) {
            // Probabilities below 1 are handled per spawn attempt, never here.
            double probability = Math.max(1.0D, MobRules.probability(data.type));
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
}
