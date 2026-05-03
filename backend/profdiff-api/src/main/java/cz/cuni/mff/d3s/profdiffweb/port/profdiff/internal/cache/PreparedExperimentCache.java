package cz.cuni.mff.d3s.profdiffweb.port.profdiff.internal.cache;

import cz.cuni.mff.d3s.profdiffweb.port.profdiff.model.ExperimentResult;
import io.micronaut.context.annotation.Value;
import jakarta.inject.Singleton;
import java.util.*;
import org.graalvm.profdiff.core.OptionValues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple thread-safe LRU cache for Prepared Experiment objects.
 * <br><b>NOTE:</b>The values are mutable, should use for read-only purposes.
 *
 * <p>While {@link ParsedExperimentCache} stores the raw result of parsing files, this cache stores
 * experiments that have undergone expensive post-processing, specifically:
 *
 * <ul>
 *   <li>Marking hot compilation units based on a specific {@link
 *       org.graalvm.profdiff.core.HotCompilationUnitPolicy}.
 *   <li>Extracting compilation fragments (which requires traversing often large inlining trees).
 * </ul>
 *
 * <p>This caching is critical for the "Comparison/Report" view, where multiple simultaneous
 * requests (e.g., calculating the method union, and rendering tree triplets) share the same
 * configuration. Without this cache, the application would repeatedly re-traverse massive (AOT)
 * logs for every component on the page.
 */
@Singleton
public class PreparedExperimentCache {
    private static final Logger LOGGER = LoggerFactory.getLogger(PreparedExperimentCache.class);
    private final Map<Key, ExperimentResult> cache;

    public record Key(
            String runPath,
            String partnerRunPath,
            int hotMinLimit,
            int hotMaxLimit,
            double hotPercentile,
            boolean createFragments) {

        public static Key forSeparate(String runPath, OptionValues options) {
            var policy = options.getHotCompilationUnitPolicy();
            return new Key(
                    runPath,
                    null,
                    policy.getHotMinLimit(),
                    policy.getHotMaxLimit(),
                    policy.getHotPercentile(),
                    options.shouldCreateFragments());
        }

        public static Key forComparison(String runPath, String partnerRunPath, OptionValues options) {
            var policy = options.getHotCompilationUnitPolicy();
            return new Key(
                    runPath,
                    partnerRunPath,
                    policy.getHotMinLimit(),
                    policy.getHotMaxLimit(),
                    policy.getHotPercentile(),
                    options.shouldCreateFragments());
        }
    }

    public PreparedExperimentCache(@Value("${cache.prepared-experiment.size:5}") int capacity) {
        final int initialCapacity = (int) Math.ceil(capacity / 0.75f) + 1;
        this.cache = Collections.synchronizedMap(new LinkedHashMap<>(initialCapacity, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Key, ExperimentResult> eldest) {
                return size() > capacity;
            }
        });
        LOGGER.info("PreparedExperimentCache initialized with capacity: {}", capacity);
    }

    /**
     * Gets a prepared experiment from the cache.
     *
     * @param key The configuration key identifying the experiment run and processing options.
     * @return An Optional containing Prepared Experiment that should be used as Read Only
     */
    public Optional<ExperimentResult> get(Key key) {
        Objects.requireNonNull(key, "Cache key cannot be null");
        return Optional.ofNullable(cache.get(key));
    }

    /**
     * Stores a prepared experiment in the cache.
     *
     * @param key The configuration key.
     * @param result The prepared experiment result to cache.
     */
    public void put(Key key, ExperimentResult result) {
        Objects.requireNonNull(key, "Cache key cannot be null");
        Objects.requireNonNull(result, "ExperimentResult cannot be null");
        cache.put(key, result);
    }
}
