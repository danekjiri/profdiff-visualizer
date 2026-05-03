package cz.cuni.mff.d3s.profdiffweb.controller.diffing;

import static org.junit.jupiter.api.Assertions.*;

import cz.cuni.mff.d3s.profdiffweb.model.dto.MethodComparisonPair;
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
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@MicronautTest
class ComparisonControllerTest {

    @Inject
    @Client("/")
    HttpClient client;

    private Path benchmarksPath;
    private final String validRunName1 = "run_20250506_165951_1";
    private final String validCompilationId1 = "2084";
    private final String validRunName2 = "run_20250506_145929_2";
    private final String validCompilationId2 = "2050";
    private final String validMethodName = "java.util.regex.Matcher.search(int)";

    @BeforeEach
    void setUp() throws URISyntaxException {
        URL resource = getClass().getResource("/benchmarks");
        assertNotNull(resource, "Test resources directory '/benchmarks' not found.");
        benchmarksPath = Paths.get(resource.toURI());
    }

    @Nested
    class GetMethodsUnionTests {
        @Test
        void getCompiledMethodsUnion_withValidParameters_shouldSucceed() {
            URI uri = UriBuilder.of("/api/compare/methods-union")
                    .queryParam("path", benchmarksPath.toString())
                    .queryParam("runName1", validRunName1)
                    .queryParam("runName2", validRunName2)
                    .build();
            HttpRequest<?> request = HttpRequest.GET(uri);

            List<MethodComparisonPair> response =
                    client.toBlocking().retrieve(request, Argument.listOf(MethodComparisonPair.class));

            assertNotNull(response, "Methods union response should not be null.");
            assertFalse(response.isEmpty(), "Methods union list should not be empty.");
        }
    }

    @Nested
    class GetComparedTreesTests {
        @Test
        void getComparedInliningTree_withValidParameters_shouldSucceed() {
            URI uri = UriBuilder.of("/api/compare/inlining-tree")
                    .queryParam("path", benchmarksPath.toString())
                    .queryParam("runName1", validRunName1)
                    .queryParam("runName2", validRunName2)
                    .queryParam("methodName", validMethodName)
                    .queryParam("compilationId1", validCompilationId1)
                    .queryParam("compilationId2", validCompilationId2)
                    .build();
            HttpRequest<?> request = HttpRequest.GET(uri);

            TreeResponse treeResponse = client.toBlocking().retrieve(request, TreeResponse.class);

            assertNotNull(treeResponse, "Tree response should not be null.");
            assertNotNull(treeResponse.tree(), "Rendered tree root should be present.");
        }

        @Test
        void getComparedOptimizationTree_withValidParameters_shouldSucceed() {
            URI uri = UriBuilder.of("/api/compare/optimization-tree")
                    .queryParam("path", benchmarksPath.toString())
                    .queryParam("runName1", validRunName1)
                    .queryParam("runName2", validRunName2)
                    .queryParam("methodName", validMethodName)
                    .queryParam("compilationId1", validCompilationId1)
                    .queryParam("compilationId2", validCompilationId2)
                    .build();
            HttpRequest<?> request = HttpRequest.GET(uri);

            TreeResponse treeResponse = client.toBlocking().retrieve(request, TreeResponse.class);

            assertNotNull(treeResponse, "Tree response should not be null.");
            assertNotNull(treeResponse.tree(), "Rendered tree root should be present.");
        }

        @Test
        void getComparedOptimizationContextTree_withValidParameters_shouldSucceed() {
            URI uri = UriBuilder.of("/api/compare/optimization-context-tree")
                    .queryParam("path", benchmarksPath.toString())
                    .queryParam("runName1", validRunName1)
                    .queryParam("runName2", validRunName2)
                    .queryParam("methodName", validMethodName)
                    .queryParam("compilationId1", validCompilationId1)
                    .queryParam("compilationId2", validCompilationId2)
                    .build();
            HttpRequest<?> request = HttpRequest.GET(uri);

            TreeResponse treeResponse = client.toBlocking().retrieve(request, TreeResponse.class);

            assertNotNull(treeResponse, "Tree response should not be null.");
            assertNotNull(treeResponse.tree(), "Rendered tree root should be present.");
        }

        @Test
        void getComparedOptimizationContextTree_withInvalidPath_shouldReturnNotFound() {
            String invalidRunName = "non-existent-run";
            Path nonExistentPath = benchmarksPath.resolve(invalidRunName);
            URI uri = UriBuilder.of("/api/compare/optimization-context-tree")
                    .queryParam("path", benchmarksPath.toString())
                    .queryParam("runName1", nonExistentPath.getFileName().toString())
                    .queryParam("runName2", validRunName2)
                    .queryParam("methodName", validMethodName)
                    .queryParam("compilationId1", validCompilationId1)
                    .queryParam("compilationId2", validCompilationId2)
                    .build();
            HttpRequest<?> request = HttpRequest.GET(uri);

            HttpClientResponseException exception = assertThrows(
                    HttpClientResponseException.class,
                    () -> client.toBlocking().exchange(request),
                    "Invalid path should throw an HTTP client exception.");

            assertEquals(HttpStatus.NOT_FOUND, exception.getStatus(), "Status should be 404 Not Found.");
            String responseBody = exception.getResponse().getBody(String.class).orElse("");
            assertTrue(
                    responseBody.contains("Run directory '" + invalidRunName + "' does not exist."),
                    "Response body did not contain expected error message for missing directory.");
        }
    }
}
