package cz.cuni.mff.d3s.profdiffweb.port.profdiff.internal.cache;

import cz.cuni.mff.d3s.profdiffweb.port.profdiff.model.ExperimentResult;
import io.micronaut.context.annotation.Value;
import jakarta.inject.Singleton;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple thread-safe LRU cache for Experiment objects.
 *
 * <p>Parsing experiment files is slow. Since this class is a {@code @Singleton}, it stays alive
 * across different HTTP requests. This lets us parse an experiment with its metadata once and reuse
 * later. Multiple requests might claim data at the same time. We use {@link
 * Collections#synchronizedMap} so that concurrent threads don't corrupt the map while reading or
 * writing.
 *
 * <p><strong>NOTE:</strong> The objects in this cache are shared "prototypes". The cache performs a
 * {@link ExperimentResult#deepCopy()} on both {@code put} (to snapshot the state) and {@code get}
 * (to provide a thread-safe instance for further processing).
 */
@Singleton
public class ParsedExperimentCache {
    private static final Logger LOGGER = LoggerFactory.getLogger(ParsedExperimentCache.class);
    private final Map<String, ExperimentResult> cache;

    public ParsedExperimentCache(@Value("${cache.parsed-experiment.size:5}") int capacity) {
        final int initialCapacity = (int) Math.ceil(capacity / 0.75f) + 1;
        ///  capacity >> cache size (regarding the loadFactor) to prevent auto-resize
        this.cache = Collections.synchronizedMap(new LinkedHashMap<>(initialCapacity, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, ExperimentResult> eldest) {
                /// when size exceeds capacity, drop the oldest used item
                return size() > capacity;
            }
        });

        LOGGER.info("ParsedExperimentCache initialized with capacity: {}", capacity);
    }

    /**
     * Gets the raw experiment from cache.
     *
     * <p><strong>NOTE:</strong> Returns a deep copy to ensure thread safety.
     *
     * @param key The key - absolute path for which we request an experiment instance
     * @return An Optional containing a safe copy of the raw experiment, if found.
     */
    public Optional<ExperimentResult> get(String key) {
        Objects.requireNonNull(key, "Cache key cannot be null");
        var experimentResult = cache.get(key);
        if (experimentResult == null) {
            return Optional.empty();
        }
        return Optional.of(experimentResult.deepCopy());
    }

    /**
     * Store raw experiment instance by its name - absolute path.
     *
     * <p><strong>NOTE:</strong> Stores a deep copy to prevent external modification of the cached
     * prototype.
     *
     * @param key The key - absolute path for which we request an experiment instance
     * @param experimentResult The instance of experiment with all its metadata
     */
    public void put(String key, ExperimentResult experimentResult) {
        Objects.requireNonNull(key, "Cache key cannot be null");
        Objects.requireNonNull(experimentResult, "ExperimentResult cannot be null");
        cache.put(key, experimentResult.deepCopy());
    }
}
