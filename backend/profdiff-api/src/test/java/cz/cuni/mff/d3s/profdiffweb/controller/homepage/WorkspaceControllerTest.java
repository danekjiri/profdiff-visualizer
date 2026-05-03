package cz.cuni.mff.d3s.profdiffweb.controller.homepage;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import cz.cuni.mff.d3s.profdiffweb.model.dto.WorkspaceDirectory;
import cz.cuni.mff.d3s.profdiffweb.service.FileFinderService;
import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import java.util.List;
import org.junit.jupiter.api.Test;

@MicronautTest
class WorkspaceControllerTest {

    @Inject
    @Client("/")
    HttpClient client;

    @Inject
    FileFinderService fileFinderService;

    @MockBean(FileFinderService.class)
    FileFinderService fileFinderService() {
        return mock(FileFinderService.class);
    }

    @Test
    void getDirectories_withValidPath_shouldReturnMarkedSubdirectories() {
        String testPath = "/mock/workspace/path";

        WorkspaceDirectory dir1 = new WorkspaceDirectory("/mock/workspace/path/run1", true);
        WorkspaceDirectory dir2 = new WorkspaceDirectory("/mock/workspace/path/fake-run", false);
        List<WorkspaceDirectory> mockResponse = List.of(dir1, dir2);

        when(fileFinderService.getMarkedSubdirectories(testPath)).thenReturn(mockResponse);

        HttpRequest<?> request = HttpRequest.GET("/api/workspace/directories?path=" + testPath);
        List<WorkspaceDirectory> result =
                client.toBlocking().retrieve(request, Argument.listOf(WorkspaceDirectory.class));

        assertNotNull(result, "The response list should not be null.");
        assertEquals(2, result.size(), "The response should contain 2 directories.");

        assertEquals("/mock/workspace/path/run1", result.getFirst().path());
        assertTrue(result.getFirst().hasRuns(), "The first directory should be marked as valid.");

        verify(fileFinderService, times(1)).getMarkedSubdirectories(testPath);
    }

    @Test
    void getMarkedSubdirectories_withoutPathParameter_shouldReturnBadRequest() {
        HttpRequest<?> request = HttpRequest.GET("/api/workspace/directories");

        HttpClientResponseException exception = assertThrows(
                HttpClientResponseException.class, () -> client.toBlocking().exchange(request));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }
}
