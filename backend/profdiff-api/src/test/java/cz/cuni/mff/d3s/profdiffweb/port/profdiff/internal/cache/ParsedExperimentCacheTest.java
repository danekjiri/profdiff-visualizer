package cz.cuni.mff.d3s.profdiffweb.port.profdiff.internal.cache;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import cz.cuni.mff.d3s.profdiffweb.port.profdiff.model.ExperimentResult;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ParsedExperimentCacheTest {

    private ParsedExperimentCache cache;

    private ExperimentResult createMockExperiment() {
        ExperimentResult mockExp = mock(ExperimentResult.class);
        lenient().when(mockExp.deepCopy()).thenReturn(mockExp);
        return mockExp;
    }

    @Nested
    class BasicOperationsTests {
        @Test
        void get_whenCacheIsEmpty_shouldReturnEmptyOptional() {
            cache = new ParsedExperimentCache(3);
            var result = cache.get("nonexistent_path");
            assertTrue(result.isEmpty(), "Empty cache should return an empty Optional.");
        }

        @Test
        void get_whenKeyDoesNotExist_shouldReturnEmptyOptional() {
            cache = new ParsedExperimentCache(3);
            cache.put("path1", createMockExperiment());
            cache.put("path2", createMockExperiment());

            var result = cache.get("nonexistent_path");
            assertTrue(result.isEmpty(), "Querying a non-existent key should return an empty Optional.");
        }

        @Test
        void get_whenKeyExists_shouldReturnExperiment() {
            cache = new ParsedExperimentCache(3);
            var experiment = createMockExperiment();
            cache.put("test_path", experiment);

            var result = cache.get("test_path");
            assertTrue(result.isPresent(), "Existing key should return an Optional with a value.");
            assertEquals(experiment, result.get(), "Returned experiment should match the stored one.");
        }

        @Test
        void put_whenKeyAlreadyExists_shouldOverwrite() {
            cache = new ParsedExperimentCache(3);
            var experiment1 = createMockExperiment();
            var experiment2 = createMockExperiment();

            cache.put("test_path", experiment1);
            cache.put("test_path", experiment2);

            var result = cache.get("test_path");
            assertTrue(result.isPresent(), "Overwritten key should still be present.");
            assertEquals(experiment2, result.get(), "The cache should return the most recently put object for a key.");
        }
    }

    @Nested
    class StrictCacheBehaviorTests {

        @Test
        void put_and_get_shouldEnforceDeepCopyIsolation() {
            cache = new ParsedExperimentCache(3);

            var externalOriginal = mock(ExperimentResult.class);
            var cachedInternalCopy = mock(ExperimentResult.class);
            var returnedExternalCopy = mock(ExperimentResult.class);

            when(externalOriginal.deepCopy()).thenReturn(cachedInternalCopy);
            when(cachedInternalCopy.deepCopy()).thenReturn(returnedExternalCopy);

            cache.put("path", externalOriginal);
            verify(externalOriginal, times(1)).deepCopy();

            var result = cache.get("path");
            verify(cachedInternalCopy, times(1)).deepCopy();

            assertTrue(result.isPresent(), "Result should be present.");
            assertEquals(
                    returnedExternalCopy,
                    result.get(),
                    "The returned object must be the copied instance, not the original.");
        }

        @Test
        void get_shouldUpdateLruAccessOrder() {
            cache = new ParsedExperimentCache(2);
            var exp1 = createMockExperiment();
            var exp2 = createMockExperiment();
            var exp3 = createMockExperiment();

            cache.put("path1", exp1);
            cache.put("path2", exp2);

            cache.get("path1");

            cache.put("path3", exp3);

            assertTrue(cache.get("path1").isPresent(), "Path1 should NOT be evicted because it was recently accessed.");
            assertTrue(cache.get("path2").isEmpty(), "Path2 should be evicted as it is the least recently accessed.");
            assertTrue(cache.get("path3").isPresent(), "Path3 should be present as the newest insertion.");
        }

        @Test
        void nullInputs_shouldThrowNullPointerException() {
            cache = new ParsedExperimentCache(3);

            assertThrows(
                    NullPointerException.class,
                    () -> cache.put(null, createMockExperiment()),
                    "Null key on put should fail fast.");
            assertThrows(
                    NullPointerException.class, () -> cache.put("key", null), "Null value on put should fail fast.");
            assertThrows(NullPointerException.class, () -> cache.get(null), "Null key on get should fail fast.");
        }
    }

    @Nested
    class ConcurrencyTests {
        @Test
        void concurrentAccess_shouldNotThrowConcurrentModificationException() throws InterruptedException {
            cache = new ParsedExperimentCache(10);
            int threadCount = 100;
            try (ExecutorService executor = Executors.newFixedThreadPool(threadCount)) {
                CountDownLatch startLatch = new CountDownLatch(1);
                CountDownLatch doneLatch = new CountDownLatch(threadCount);

                for (int i = 0; i < 5; i++) {
                    cache.put("key" + i, createMockExperiment());
                }

                for (int i = 0; i < threadCount; i++) {
                    final int index = i;
                    executor.submit(() -> {
                        try {
                            startLatch.await();

                            cache.put("key" + index, createMockExperiment());
                            cache.get("key" + (index % 5));
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
