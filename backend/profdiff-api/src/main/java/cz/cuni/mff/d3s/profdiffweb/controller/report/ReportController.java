package cz.cuni.mff.d3s.profdiffweb.controller.report;

import cz.cuni.mff.d3s.profdiffweb.controller.annotations.StandardErrorResponses;
import cz.cuni.mff.d3s.profdiffweb.model.dto.*;
import cz.cuni.mff.d3s.profdiffweb.model.dto.JavaMethod;
import cz.cuni.mff.d3s.profdiffweb.service.RunsService;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import java.util.Collection;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller for handling requests related to Profdiff's Reports.
 *
 * <p>Provides endpoints to retrieve JSONified parts of Profdiff Reports. Absolute path and runName
 * for identification is required. All errors are handled locally by returning appropriate HTTP
 * status codes and JSONified error messages.
 */
@ExecuteOn(TaskExecutors.BLOCKING)
@Controller("/api/report")
@Tag(name = "Report")
public class ReportController {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReportController.class);

    private final RunsService runsService;

    @Inject
    public ReportController(RunsService runsService) {
        this.runsService = runsService;
    }

    /**
     * Endpoint to retrieve General Metadata of a specified run.
     *
     * @param path Absolute path to the directory containing the run.
     * @param runName Name of the run to retrieve metadata for.
     * @return JSONified object containing the run's metadata.
     */
    @Get("/metadata")
    @Operation(summary = "Get Report's Metadata")
    @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved report metadata.",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = RunMetadata.class)))
    @StandardErrorResponses
    public RunMetadata getRunMetadata(
            @Parameter(
                            description = "The absolute path to the directory containing the benchmark runs.",
                            required = true,
                            example = "/default-benchmarks")
                    @QueryValue("path")
                    String path,
            @Parameter(
                            description = "The directory name of the run.",
                            required = true,
                            example = "sd1-graal25-valid-fast")
                    @QueryValue("runName")
                    String runName) {
        LOGGER.info("Requesting report metadata for single run '{}' in path '{}'", runName, path);
        return runsService.getRunMetadata(path, runName);
    }

    /**
     * Endpoint to retrieve the Top Methods of specified run.
     *
     * @param path Absolute path to the directory containing the run.
     * @param runName Name of the run to retrieve top methods for.
     * @param hotOptions Options defining the policy for filtering hot compilation units.
     * @return List of top methods in the specified run.
     */
    @Get("/top-methods")
    @Operation(summary = "Get Report's Top Methods")
    @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved report's top methods.",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(type = "array", implementation = TopMethod.class)))
    @StandardErrorResponses
    public List<TopMethod> getTopHotMethods(
            @Parameter(
                            description = "The absolute path to the directory containing the benchmark runs.",
                            required = true,
                            example = "/default-benchmarks")
                    @QueryValue("path")
                    String path,
            @Parameter(
                            description = "The directory name of the run.",
                            required = true,
                            example = "sd1-graal25-valid-fast")
                    @QueryValue("runName")
                    String runName,
            @Parameter(description = "Options defining the policy for filtering hot compilation units.")
                    @Valid
                    @RequestBean
                    HotPolicyOptions hotOptions) {
        LOGGER.info(
                "Requesting top methods for single run '{}' in path '{}', with hot-options '{}'",
                runName,
                path,
                hotOptions);
        return runsService.getTopHotMethods(path, runName, hotOptions);
    }

    /**
     * Endpoint to retrieve all Java Methods with its compilation's units compiled by GRAALVM of
     * specified run.
     *
     * @param path Absolute path to the directory containing the run.
     * @param runName Name of the run to retrieve methods for.
     * @param hotOptions Options defining the policy for filtering hot compilation units.
     * @param renderingOptions Options configuring how the experiment data and trees are processed and
     *     rendered.
     * @return Collection of all Java methods in the specified run.
     */
    @Get(value = "/all-methods")
    @Operation(summary = "Get All Java Methods in the Report")
    @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved all Java methods in the report.",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(type = "array", implementation = JavaMethod.class)))
    @StandardErrorResponses
    public Collection<JavaMethod> getAllCompiledMethods(
            @Parameter(
                            description = "The absolute path to the directory containing the benchmark runs.",
                            required = true,
                            example = "/default-benchmarks")
                    @QueryValue("path")
                    String path,
            @Parameter(
                            description = "The directory name of the run.",
                            required = true,
                            example = "sd1-graal25-valid-fast")
                    @QueryValue("runName")
                    String runName,
            @Parameter(description = "Options defining the policy for filtering hot compilation units.")
                    @Valid
                    @RequestBean
                    HotPolicyOptions hotOptions,
            @Parameter(
                            description =
                                    "Options configuring how the experiment data and trees are processed and rendered.")
                    @RequestBean
                    ExperimentProcessingOptions renderingOptions) {
        LOGGER.info(
                "Requesting all methods for single run '{}' in path '{}', with hot-options '{}' and rendering-options '{}'",
                runName,
                path,
                hotOptions,
                renderingOptions);
        return runsService.getCompiledMethods(path, runName, renderingOptions, hotOptions);
    }

    /**
     * Endpoint to retrieve the Inlining Tree for a specific compilation unit.
     *
     * @param path Absolute path to the directory containing the benchmark runs.
     * @param runName The directory name of the run.
     * @param methodName The full name of method and its arguments types.
     * @param compilationId The ID that the compiler assigned to the Compilation Unit.
     * @param hotPolicyOptions Options defining the policy for filtering hot compilation units.
     * @param renderingOptions Options configuring how the experiment data and trees are processed and
     *     rendered.
     * @return TreeResponse containing the Inlining Tree.
     */
    @Get(value = "/inlining-tree")
    @Operation(summary = "Get Report's Inlining Tree")
    @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved the Inlining Tree.",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = TreeResponse.class)))
    @StandardErrorResponses
    public TreeResponse getReportInliningTree(
            @Parameter(
                            description = "The absolute path to the directory containing the benchmark runs.",
                            required = true,
                            example = "/default-benchmarks")
                    @QueryValue("path")
                    String path,
            @Parameter(
                            description = "The directory name of the run.",
                            required = true,
                            example = "sd1-graal25-valid-fast")
                    @QueryValue("runName")
                    String runName,
            @Parameter(
                            description = "The full name of method and its arguments types.",
                            required = true,
                            example = "scala.collection.IterableOnceOps.count(Function1)")
                    @QueryValue("methodName")
                    String methodName,
            @Parameter(
                            description = "The ID that the compiler assigned to the Compilation Unit.",
                            required = true,
                            example = "2504")
                    @QueryValue("compilationId")
                    String compilationId,
            @Parameter(description = "Options defining the policy for filtering hot compilation units.")
                    @Valid
                    @RequestBean
                    HotPolicyOptions hotPolicyOptions,
            @Parameter(
                            description =
                                    "Options configuring how the experiment data and trees are processed and rendered.")
                    @RequestBean
                    ExperimentProcessingOptions renderingOptions) {
        LOGGER.info(
                "Requesting Report Inlining Tree for method '{}' with compilation ID '{}'", methodName, compilationId);
        return runsService.getReportInliningTree(
                path, runName, methodName, compilationId, renderingOptions, hotPolicyOptions);
    }

    /**
     * Endpoint to retrieve the Optimization Tree for a specific compilation unit.
     *
     * @param path Absolute path to the directory containing the benchmark runs.
     * @param runName The directory name of the run.
     * @param methodName The full name of method and its arguments types.
     * @param compilationId The ID that the compiler assigned to the Compilation Unit.
     * @param hotPolicyOptions Options defining the policy for filtering hot compilation units.
     * @param renderingOptions Options configuring how the experiment data and trees are processed and
     *     rendered.
     * @return TreeResponse containing the Inlining Tree.
     */
    @Get(value = "/optimization-tree")
    @Operation(summary = "Get Report's Optimization Tree")
    @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved the Optimization Tree.",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = TreeResponse.class)))
    @StandardErrorResponses
    public TreeResponse getReportOptimizationTree(
            @Parameter(
                            description = "The absolute path to the directory containing the benchmark runs.",
                            example = "/default-benchmarks",
                            required = true)
                    @QueryValue("path")
                    String path,
            @Parameter(
                            description = "The directory name of the run.",
                            example = "sd1-graal25-valid-fast",
                            required = true)
                    @QueryValue("runName")
                    String runName,
            @Parameter(
                            description = "The full name of method and its arguments types.",
                            example = "scala.collection.IterableOnceOps.count(Function1)",
                            required = true)
                    @QueryValue("methodName")
                    String methodName,
            @Parameter(
                            description = "The ID that the compiler assigned to the Compilation Unit.",
                            example = "2504",
                            required = true)
                    @QueryValue("compilationId")
                    String compilationId,
            @Parameter(description = "Options defining the policy for filtering hot compilation units.")
                    @Valid
                    @RequestBean
                    HotPolicyOptions hotPolicyOptions,
            @Parameter(
                            description =
                                    "Options configuring how the experiment data and trees are processed and rendered.")
                    @RequestBean
                    ExperimentProcessingOptions renderingOptions) {
        LOGGER.info(
                "Requesting Report Optimization Tree for method '{}' with compilation ID '{}'",
                methodName,
                compilationId);
        return runsService.getReportOptimizationTree(
                path, runName, methodName, compilationId, renderingOptions, hotPolicyOptions);
    }

    /**
     * Endpoint to retrieve the Optimization-Context tree for a specific compilation unit.
     *
     * @param path Absolute path to the directory containing the benchmark runs.
     * @param runName The directory name of the run.
     * @param methodName The full name of method and its arguments types.
     * @param compilationId The ID that the compiler assigned to the Compilation Unit.
     * @param hotPolicyOptions Options defining the policy for filtering hot compilation units.
     * @param renderingOptions Options configuring how the experiment data and trees are processed and
     *     rendered.
     * @return TreeResponse containing the Inlining Tree.
     */
    @Get(value = "/optimization-context-tree")
    @Operation(summary = "Get Report's Optimization-Context Tree")
    @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved the Optimization-Context Tree.",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = TreeResponse.class)))
    @StandardErrorResponses
    public TreeResponse getReportOptimizationContextTree(
            @Parameter(
                            description = "The absolute path to the directory containing the benchmark runs.",
                            example = "/default-benchmarks",
                            required = true)
                    @QueryValue("path")
                    String path,
            @Parameter(
                            description = "The directory name of the run.",
                            example = "sd1-graal25-valid-fast",
                            required = true)
                    @QueryValue("runName")
                    String runName,
            @Parameter(
                            description = "The full name of method and its arguments types.",
                            example = "scala.collection.IterableOnceOps.count(Function1)",
                            required = true)
                    @QueryValue("methodName")
                    String methodName,
            @Parameter(
                            description = "The ID that the compiler assigned to the Compilation Unit.",
                            example = "2504",
                            required = true)
                    @QueryValue("compilationId")
                    String compilationId,
            @Parameter(description = "Options defining the policy for filtering hot compilation units.")
                    @Valid
                    @RequestBean
                    HotPolicyOptions hotPolicyOptions,
            @Parameter(
                            description =
                                    "Options configuring how the experiment data and trees are processed and rendered.")
                    @RequestBean
                    ExperimentProcessingOptions renderingOptions) {
        LOGGER.info(
                "Requesting Report Optimization-Context Tree for method '{}' with compilation ID '{}'",
                methodName,
                compilationId);
        return runsService.getReportOptimizationContextTree(
                path, runName, methodName, compilationId, renderingOptions, hotPolicyOptions);
    }
}
