package cz.cuni.mff.d3s.profdiffweb.port.profdiff.internal.cache;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import cz.cuni.mff.d3s.profdiffweb.port.profdiff.model.ExperimentResult;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.graalvm.profdiff.core.HotCompilationUnitPolicy;
import org.graalvm.profdiff.core.OptionValues;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PreparedExperimentCacheTest {

    private PreparedExperimentCache cache;

    private PreparedExperimentCache.Key createKey(String runPath) {
        return new PreparedExperimentCache.Key(runPath, null, 1, 100, 0.9, true);
    }

    private ExperimentResult createMockExperiment() {
        return mock(ExperimentResult.class);
    }

    @Nested
    class BasicOperationsTests {
        @Test
        void get_whenCacheIsEmpty_shouldReturnEmptyOptional() {
            cache = new PreparedExperimentCache(3);
            var result = cache.get(createKey("nonexistent"));
            assertTrue(result.isEmpty(), "Empty cache should return an empty Optional.");
        }

        @Test
        void get_whenKeyDoesNotExist_shouldReturnEmptyOptional() {
            cache = new PreparedExperimentCache(3);
            cache.put(createKey("path1"), createMockExperiment());

            var result = cache.get(createKey("nonexistent"));
            assertTrue(result.isEmpty(), "Querying a non-existent key should return an empty Optional.");
        }

        @Test
        void get_whenKeyExists_shouldReturnExperimentWithoutDeepCopying() {
            cache = new PreparedExperimentCache(3);
            var experiment = createMockExperiment();
            var key = createKey("test_path");
            cache.put(key, experiment);

            var result = cache.get(key);
            assertTrue(result.isPresent(), "Existing key should return an Optional with a value.");

            assertSame(
                    experiment,
                    result.get(),
                    "Returned experiment must be the exact same instance (shared reference).");
        }

        @Test
        void put_whenKeyAlreadyExists_shouldOverwrite() {
            cache = new PreparedExperimentCache(3);
            var experiment1 = createMockExperiment();
            var experiment2 = createMockExperiment();
            var key = createKey("test_path");

            cache.put(key, experiment1);
            cache.put(key, experiment2);

            var result = cache.get(key);
            assertTrue(result.isPresent(), "Overwritten key should still be present.");
            assertSame(experiment2, result.get(), "The cache should return the most recently put object for a key.");
        }
    }

    @Nested
    class KeyGenerationTests {
        @Test
        void forSeparate_shouldMapPropertiesCorrectly() {
            var options = mock(OptionValues.class);
            var policy = mock(HotCompilationUnitPolicy.class);
            when(options.getHotCompilationUnitPolicy()).thenReturn(policy);
            when(options.shouldCreateFragments()).thenReturn(true);
            when(policy.getHotMinLimit()).thenReturn(10);
            when(policy.getHotMaxLimit()).thenReturn(50);
            when(policy.getHotPercentile()).thenReturn(0.85);

            var key = PreparedExperimentCache.Key.forSeparate("run_path", options);

            assertEquals("run_path", key.runPath(), "Run path should match.");
            assertNull(key.partnerRunPath(), "Partner run path should be null for separate context.");
            assertEquals(10, key.hotMinLimit(), "Hot min limit should match policy.");
            assertEquals(50, key.hotMaxLimit(), "Hot max limit should match policy.");
            assertEquals(0.85, key.hotPercentile(), "Hot percentile should match policy.");
            assertTrue(key.createFragments(), "Create fragments flag should match options.");
        }

        @Test
        void forComparison_shouldMapPropertiesCorrectly() {
            var options = mock(OptionValues.class);
            var policy = mock(HotCompilationUnitPolicy.class);
            when(options.getHotCompilationUnitPolicy()).thenReturn(policy);
            when(options.shouldCreateFragments()).thenReturn(false);
            when(policy.getHotMinLimit()).thenReturn(5);

            var key = PreparedExperimentCache.Key.forComparison("run1", "run2", options);

            assertEquals("run1", key.runPath(), "Run path should match.");
            assertEquals("run2", key.partnerRunPath(), "Partner run path should match.");
            assertEquals(5, key.hotMinLimit(), "Hot min limit should match policy.");
            assertFalse(key.createFragments(), "Create fragments flag should match options.");
        }
    }

    @Nested
    class StrictCacheBehaviorTests {

        @Test
        void get_shouldUpdateLruAccessOrder() {
            cache = new PreparedExperimentCache(2);
            var key1 = createKey("path1");
            var key2 = createKey("path2");
            var key3 = createKey("path3");

            cache.put(key1, createMockExperiment());
            cache.put(key2, createMockExperiment());

            cache.get(key1);

            cache.put(key3, createMockExperiment());

            assertTrue(cache.get(key1).isPresent(), "Key1 should NOT be evicted because it was recently accessed.");
            assertTrue(cache.get(key2).isEmpty(), "Key2 should be evicted as it is the least recently accessed.");
            assertTrue(cache.get(key3).isPresent(), "Key3 should be present as the newest insertion.");
        }

        @Test
        void nullInputs_shouldThrowNullPointerException() {
            cache = new PreparedExperimentCache(3);
            var validKey = createKey("valid");
            var validExp = createMockExperiment();

            assertThrows(
                    NullPointerException.class, () -> cache.put(null, validExp), "Null key on put should fail fast.");
            assertThrows(
                    NullPointerException.class, () -> cache.put(validKey, null), "Null value on put should fail fast.");
            assertThrows(NullPointerException.class, () -> cache.get(null), "Null key on get should fail fast.");
        }
    }

    @Nested
    class ConcurrencyTests {
        @Test
        void concurrentAccess_shouldNotThrowConcurrentModificationException() throws InterruptedException {
            cache = new PreparedExperimentCache(10);
            int threadCount = 20;
            try (ExecutorService executor = Executors.newFixedThreadPool(threadCount)) {
                CountDownLatch startLatch = new CountDownLatch(1);
                CountDownLatch doneLatch = new CountDownLatch(threadCount);

                for (int i = 0; i < 5; i++) {
                    cache.put(createKey("key" + i), createMockExperiment());
                }

                for (int i = 0; i < threadCount; i++) {
                    final int index = i;
                    executor.submit(() -> {
                        try {
                            startLatch.await();

                            cache.put(createKey("key" + index), createMockExperiment());
                            cache.get(createKey("key" + (index % 5)));
                        } catch (Exception e) {
                            fail("Exception occurred during concurrent cache access: " + e.getMessage());
                        } finally {
                            doneLatch.countDown();
                        }
                    });
                }
                startLatch.countDown();
                boolean completed = doneLatch.await(5, TimeUnit.SECONDS);

                executor.shutdownNow();
                assertTrue(completed, "All threads should complete without hanging or throwing exceptions.");
            }
        }
    }
}
