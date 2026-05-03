package cz.cuni.mff.d3s.profdiffweb.controller.homepage;

import cz.cuni.mff.d3s.profdiffweb.controller.annotations.StandardErrorResponses;
import cz.cuni.mff.d3s.profdiffweb.model.dto.WorkspaceDirectory;
import cz.cuni.mff.d3s.profdiffweb.service.FileFinderService;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import java.util.List;

/**
 * Controller for handling workspace directory retrieval.
 *
 * <p>Provides endpoints to list subdirectories of a given path, marking those that contain
 * benchmark runs.
 */
@Controller("api/workspace")
@Tag(name = "Workspace")
public class WorkspaceController {

    private final FileFinderService fileFinderService;

    @Inject
    public WorkspaceController(FileFinderService fileFinderService) {
        this.fileFinderService = fileFinderService;
    }

    /**
     * Endpoint to retrieve a list of subdirectories for a given parent path.
     *
     * @param path The absolute parent directory path to scan.
     * @return A list of {@link WorkspaceDirectory} objects, or an empty list if the path is invalid
     *     or without subdirectories.
     */
    @Get(uri = "/directories", produces = MediaType.APPLICATION_JSON)
    @ExecuteOn(TaskExecutors.BLOCKING)
    @Operation(summary = "List Directories for Autocomplete")
    @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved the list of subdirectories.",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            array = @ArraySchema(schema = @Schema(implementation = WorkspaceDirectory.class))))
    @StandardErrorResponses
    public List<WorkspaceDirectory> getMarkedSubdirectories(
            @Parameter(
                            description =
                                    "The absolute or normalized parent directory path to search for subdirectories.",
                            example = "/workspace/",
                            required = true)
                    @QueryValue("path")
                    String path) {
        return fileFinderService.getMarkedSubdirectories(path);
    }
}
