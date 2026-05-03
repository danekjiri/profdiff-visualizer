package cz.cuni.mff.d3s.profdiffweb.controller.homepage;

import cz.cuni.mff.d3s.profdiffweb.controller.annotations.StandardErrorResponses;
import cz.cuni.mff.d3s.profdiffweb.model.dto.BenchmarkRuns;
import cz.cuni.mff.d3s.profdiffweb.service.RunsService;
import io.micronaut.cache.annotation.CacheInvalidate;
import io.micronaut.cache.annotation.Cacheable;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Put;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller for handling requests related to runs metadata.
 *
 * <p>Provides endpoints to retrieve metadata of benchmark runs from a specified directory path and
 * handles errors locally.
 */
@Controller("api/runs")
@Tag(name = "Runs")
public class RunsController {
    private static final Logger LOGGER = LoggerFactory.getLogger(RunsController.class);

    private final RunsService runsService;

    @Inject
    public RunsController(RunsService runsService) {
        this.runsService = runsService;
    }

    /**
     * Endpoint to retrieve all found runs metadata in a specified path. Due to heavy similar request
     * (same rutn path) to safe time and cpu resources it caches results.
     *
     * @param path Absolute path to the directory containing runs metadata.
     * @return A {@link BenchmarkRuns} object containing the list of runs metadata.
     */
    @Get()
    @Operation(summary = "Get All Runs' Metadata")
    @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved run metadata.",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = BenchmarkRuns.class)))
    @StandardErrorResponses
    @ExecuteOn(TaskExecutors.BLOCKING)
    @Cacheable("runs-metadata-cache")
    public BenchmarkRuns getAllRunsMetadata(
            @Parameter(
                            description = "The absolute path to the directory containing the benchmark runs.",
                            required = true,
                            example = "/default-benchmarks")
                    @QueryValue("path")
                    String path) {
        LOGGER.info("Retrieving runs metadata from path: '{}'", path);
        return runsService.getAllBenchmarksRunsMetadata(path);
    }

    /**
     * Force clears the cache for a specific path so the next GET request re-scans the directory.
     *
     * @param path Absolute path to the directory containing runs metadata.
     */
    @Put("/refresh")
    @Operation(summary = "Force Refresh Cached Benchmark Directory Data")
    @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved run metadata.")
    @CacheInvalidate(value = "runs-metadata-cache", parameters = "path")
    public void refreshRunsMetadata(
        @Parameter(
                            description = "The absolute path to the directory containing the benchmark runs.",
                            required = true,
                            example = "/default-benchmarks")
                @QueryValue("path") String path) {
        LOGGER.info("Manually invalidated runs metadata cache for path: '{}'", path);
    }
}
