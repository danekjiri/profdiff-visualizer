package cz.cuni.mff.d3s.profdiffweb.controller.report;

import static org.junit.jupiter.api.Assertions.*;

import cz.cuni.mff.d3s.profdiffweb.model.dto.JavaMethod;
import cz.cuni.mff.d3s.profdiffweb.model.dto.RunMetadata;
import cz.cuni.mff.d3s.profdiffweb.model.dto.TopMethod;
import cz.cuni.mff.d3s.profdiffweb.model.dto.TreeResponse;
import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.http.uri.UriBuilder;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@MicronautTest
class ReportControllerTest {

    @Inject
    @Client("/")
    HttpClient client;

    private Path benchmarksPath;
    private final String validRunName = "run_20250506_165951_1";
    private final String invalidRunName = "non-existent-run";

    @BeforeEach
    void setUp() throws URISyntaxException {
        URL resource = getClass().getResource("/benchmarks");
        assertNotNull(resource, "Test resources directory '/benchmarks' not found.");
        benchmarksPath = Paths.get(resource.toURI());
    }

    @Nested
    class GetRunMetadataTests {
        @Test
        void getReportMetadata_withValidParameters_shouldSucceed() {
            URI uri = UriBuilder.of("/api/report/metadata")
                    .queryParam("path", benchmarksPath.toString())
                    .queryParam("runName", validRunName)
                    .build();
            HttpRequest<?> request = HttpRequest.GET(uri);

            RunMetadata metadata = client.toBlocking().retrieve(request, RunMetadata.class);

            assertNotNull(metadata, "Metadata response should not be null.");
            assertNotNull(metadata.benchmarkRunMetadata(), "Benchmark run metadata should be populated.");
            assertEquals(validRunName, metadata.benchmarkRunMetadata().runName(), "Run names should match.");
            assertEquals(352016328276L, metadata.totalPeriod(), "Total period should match the parsed data.");
            assertEquals(
                    "JIT",
                    metadata.benchmarkRunMetadata().profileMetadata().compilationKind(),
                    "Compilation kind should be JIT.");
        }

        @Test
        void getReportMetadata_withInvalidRunName_shouldReturnNotFound() {
            URI uri = UriBuilder.of("/api/report/metadata")
                    .queryParam("path", benchmarksPath.toString())
                    .queryParam("runName", invalidRunName)
                    .build();
            HttpRequest<?> request = HttpRequest.GET(uri);

            HttpClientResponseException exception = assertThrows(
                    HttpClientResponseException.class,
                    () -> client.toBlocking().exchange(request),
                    "Invalid run name should throw an HTTP client exception.");
            assertEquals(HttpStatus.NOT_FOUND, exception.getStatus(), "Status should be 404 Not Found.");
        }

        @Test
        void getReportMetadata_withoutRequiredParameters_shouldReturnBadRequest() {
            URI uri = UriBuilder.of("/api/report/metadata")
                    .queryParam("path", benchmarksPath.toString())
                    .build();
            HttpRequest<?> request = HttpRequest.GET(uri);

            HttpClientResponseException exception = assertThrows(
                    HttpClientResponseException.class,
                    () -> client.toBlocking().exchange(request),
                    "Missing parameters should throw an HTTP client exception.");

            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus(), "Status should be 400 Bad Request.");
        }
    }

    @Nested
    class GetTopMethodsTests {
        @Test
        void getTopMethods_withValidParameters_shouldSucceed() {
            URI uri = UriBuilder.of("/api/report/top-methods")
                    .queryParam("path", benchmarksPath.toString())
                    .queryParam("runName", validRunName)
                    .build();
            HttpRequest<?> request = HttpRequest.GET(uri);

            List<TopMethod> topMethods = client.toBlocking().retrieve(request, Argument.listOf(TopMethod.class));

            assertNotNull(topMethods, "Top methods list should not be null.");
            assertFalse(topMethods.isEmpty(), "Top methods list should not be empty.");
            assertTrue(
                    topMethods.getFirst().executionPercentage().endsWith("%"),
                    "Execution should be formatted as a percentage.");
        }

        @Test
        void getTopMethods_withInvalidRunName_shouldReturnNotFound() {
            URI uri = UriBuilder.of("/api/report/top-methods")
                    .queryParam("path", benchmarksPath.toString())
                    .queryParam("runName", invalidRunName)
                    .build();
            HttpRequest<?> request = HttpRequest.GET(uri);

            HttpClientResponseException exception = assertThrows(
                    HttpClientResponseException.class,
                    () -> client.toBlocking().exchange(request),
                    "Invalid run name should throw an HTTP client exception.");
            assertEquals(HttpStatus.NOT_FOUND, exception.getStatus(), "Status should be 404 Not Found.");
        }
    }

    @Nested
    class GetAllMethodsTests {
        @Test
        void getAllMethods_withValidParameters_shouldSucceed() {
            URI uri = UriBuilder.of("/api/report/all-methods")
                    .queryParam("path", benchmarksPath.toString())
                    .queryParam("runName", validRunName)
                    .build();
            HttpRequest<?> request = HttpRequest.GET(uri);

            Collection<JavaMethod> allMethods =
                    client.toBlocking().retrieve(request, Argument.listOf(JavaMethod.class));

            assertNotNull(allMethods, "All methods collection should not be null.");
            assertFalse(allMethods.isEmpty(), "All methods collection should not be empty.");
        }

        @Test
        void getAllMethods_withInvalidRunName_shouldReturnNotFound() {
            URI uri = UriBuilder.of("/api/report/all-methods")
                    .queryParam("path", benchmarksPath.toString())
                    .queryParam("runName", invalidRunName)
                    .build();
            HttpRequest<?> request = HttpRequest.GET(uri);

            HttpClientResponseException exception = assertThrows(
                    HttpClientResponseException.class,
                    () -> client.toBlocking().exchange(request),
                    "Invalid run name should throw an HTTP client exception.");
            assertEquals(HttpStatus.NOT_FOUND, exception.getStatus(), "Status should be 404 Not Found.");
        }
    }

    @Nested
    class GetMethodsReportTreeTests {
        private final String validMethodName = "java.util.zip.ZipUtils.SH(byte[], int)";
        private final String validCompId = "152";

        @Test
        void getReportInliningTree_withValidParameters_shouldSucceed() {
            URI uri = UriBuilder.of("/api/report/inlining-tree")
                    .queryParam("path", benchmarksPath.toString())
                    .queryParam("runName", validRunName)
                    .queryParam("methodName", validMethodName)
                    .queryParam("compilationId", validCompId)
                    .build();
            HttpRequest<?> request = HttpRequest.GET(uri);
            TreeResponse response = client.toBlocking().retrieve(request, TreeResponse.class);

            assertNotNull(response, "Tree response should not be null.");
            assertNotNull(response.tree(), "Rendered tree root should be present.");
        }

        @Test
        void getReportOptimizationTree_withValidParameters_shouldSucceed() {
            URI uri = UriBuilder.of("/api/report/optimization-tree")
                    .queryParam("path", benchmarksPath.toString())
                    .queryParam("runName", validRunName)
                    .queryParam("methodName", validMethodName)
                    .queryParam("compilationId", validCompId)
                    .build();
            HttpRequest<?> request = HttpRequest.GET(uri);
            TreeResponse response = client.toBlocking().retrieve(request, TreeResponse.class);

            assertNotNull(response, "Tree response should not be null.");
            assertNotNull(response.tree(), "Rendered tree root should be present.");
        }

        @Test
        void getReportOptimizationContextTree_withValidParameters_shouldSucceed() {
            URI uri = UriBuilder.of("/api/report/optimization-context-tree")
                    .queryParam("path", benchmarksPath.toString())
                    .queryParam("runName", validRunName)
                    .queryParam("methodName", validMethodName)
                    .queryParam("compilationId", validCompId)
                    .build();
            HttpRequest<?> request = HttpRequest.GET(uri);

            TreeResponse response = client.toBlocking().retrieve(request, TreeResponse.class);

            assertNotNull(response, "Tree response should not be null.");
            assertNotNull(response.tree(), "Rendered tree root should be present.");
        }

        @Test
        void getReportOptimizationContextTree_withInvalidMethodName_shouldReturnNotFoundError() {
            URI uri = UriBuilder.of("/api/report/optimization-context-tree")
                    .queryParam("path", benchmarksPath.toString())
                    .queryParam("runName", validRunName)
                    .queryParam("methodName", "non.existent.method")
                    .queryParam("compilationId", "42")
                    .build();
            HttpRequest<?> request = HttpRequest.GET(uri);

            HttpClientResponseException exception = assertThrows(
                    HttpClientResponseException.class,
                    () -> client.toBlocking().exchange(request),
                    "Invalid method name should throw an HTTP client exception.");

            assertEquals(HttpStatus.NOT_FOUND, exception.getStatus(), "Status should be 404 Not Found.");
            String responseBody = exception.getResponse().getBody(String.class).orElse("");
            assertTrue(
                    responseBody.contains("Method 'non.existent.method' not found."),
                    "Error body should explain the missing method.");
        }
    }
}
