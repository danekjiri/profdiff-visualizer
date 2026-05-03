package cz.cuni.mff.d3s.profdiffweb.controller.homepage;

import static org.junit.jupiter.api.Assertions.*;

import cz.cuni.mff.d3s.profdiffweb.model.dto.BenchmarkRunMetadata;
import cz.cuni.mff.d3s.profdiffweb.model.dto.BenchmarkRuns;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@MicronautTest
class RunsControllerTest {

    @Inject
    @Client("/")
    HttpClient client;

    private Path benchmarksPath;

    @BeforeEach
    void setUp() throws URISyntaxException {
        URL resource = getClass().getResource("/benchmarks");
        assertNotNull(resource, "Test resources directory '/benchmarks' not found.");
        benchmarksPath = Paths.get(resource.toURI());
    }

    @Test
    void getAllRunsMetadata_withValidDirectory_shouldSucceed() {
        HttpRequest<?> request = HttpRequest.GET("/api/runs?path=" + benchmarksPath);

        BenchmarkRuns runs = client.toBlocking().retrieve(request, BenchmarkRuns.class);

        assertNotNull(runs);
        assertEquals(2, runs.benchmarkRuns().size(), "Should find two valid run directories.");

        BenchmarkRunMetadata run1 = runs.benchmarkRuns().stream()
                .filter(r -> r.runName().equals("run_20250506_165951_1"))
                .findFirst()
                .orElse(null);
        assertNotNull(run1, "Run 'run_20250630_165951_1' should be present.");
    }

    @Test
    void getAllRunsMetadata_withNonExistentPath_shouldReturnNotFound() {
        Path nonExistentPath = benchmarksPath.resolve("this-directory-does-not-exist");
        HttpRequest<?> request = HttpRequest.GET("/api/runs?path=" + nonExistentPath);

        HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class, () -> client.toBlocking().exchange(request));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        String responseBody = exception.getResponse().getBody(String.class).orElse("");
        assertTrue(responseBody.contains("is not a valid directory"));
    }

    @Test
    void getAllRunsMetadata_withFilePath_shouldReturnNotFound() {
        Path filePath = benchmarksPath.resolve("run_20250630_165951_1/bench-results.json");
        HttpRequest<?> request = HttpRequest.GET("/api/runs?path=" + filePath);

        HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class, () -> client.toBlocking().exchange(request));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        String responseBody = exception.getResponse().getBody(String.class).orElse("");
        assertTrue(responseBody.contains("is not a valid directory"));
    }

    @Test
    void getAllRunsMetadata_withoutPathParameter_shouldReturnBadRequest() {
        HttpRequest<?> request = HttpRequest.GET("/api/runs");

        HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class, () -> client.toBlocking().exchange(request));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }
}
