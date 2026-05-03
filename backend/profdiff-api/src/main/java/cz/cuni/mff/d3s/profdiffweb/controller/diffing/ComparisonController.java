package cz.cuni.mff.d3s.profdiffweb.controller.diffing;

import cz.cuni.mff.d3s.profdiffweb.controller.annotations.StandardErrorResponses;
import cz.cuni.mff.d3s.profdiffweb.model.dto.*;
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
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import java.util.Collection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller for handling requests related to comparison between two runs.
 *
 * <p>Provides endpoints to retrieve compared data using Profdiff's data structures and methods.
 */
@ExecuteOn(TaskExecutors.BLOCKING)
@Controller("/api/compare")
@Tag(name = "Comparison")
public class ComparisonController {
    private static final Logger LOGGER = LoggerFactory.getLogger(ComparisonController.class);

    private final RunsService runsService;

    @Inject
    public ComparisonController(RunsService runsService) {
        this.runsService = runsService;
    }

    /**
     * Endpoint to retrieve the union of compiled Java Methods available for comparison between
     * two runs so that in each experiment at lest one compilation unit exists.
     *
     * @param path Absolute path to the directory containing runs metadata.
     * @param runName1 Name of the first run.
     * @param runName2 Name of the second run.
     * @return Collection of Java methods (union from both runs).
     */
    @Get("/methods-union")
    @Operation(summary = "Get union for all Java Methods for comparison")
    @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved all Java Methods for comparison.",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(type = "array", implementation = MethodComparisonPair.class)))
    @StandardErrorResponses
    public Collection<MethodComparisonPair> getCompiledMethodsUnion(
            @Parameter(
                            description = "The absolute path to the directory containing the benchmark runs.",
                            required = true,
                            example = "/default-benchmarks")
                    @QueryValue("path")
                    String path,
            @Parameter(
                            description = "The directory name of the first run.",
                            required = true,
                            example = "sd2-graal25-valid-slow")
                    @QueryValue("runName1")
                    String runName1,
            @Parameter(
                            description = "The directory name of the second run.",
                            required = true,
                            example = "sd1-graal25-valid-fast")
                    @QueryValue("runName2")
                    String runName2,
            @Parameter(description = "Options defining the policy for hot compilation units filtering.")
                    @Valid
                    @RequestBean
                    HotPolicyOptions hotOptions,
            @Parameter(description = "Options for processing and rendering the experiment trees.") @RequestBean
                    ExperimentProcessingOptions renderingOptions) {
        LOGGER.info(
                "Requesting union for all methods for comparison between runs '{}' and '{}' in path '{}', with hot-options '{}'",
                runName1,
                runName2,
                path,
                hotOptions);
        return runsService.getCompiledMethodsUnionPairs(path, runName1, runName2, renderingOptions, hotOptions);
    }

    /**
     * Endpoint to retrieve the Inlining compared tree between two specified runs and their
     * compilation units.
     *
     * @param path Absolute path to the directory containing runs metadata.
     * @param runName1 Name of the first run.
     * @param runName2 Name of the second run.
     * @param methodName The full name of method and its arguments types.
     * @param compilationId1 The ID that compiler assigned to first run's Compilation Unit.
     * @param compilationId2 The ID that compiler assigned to second run's Compilation Unit.
     * @param hotPolicyOptions Options defining the policy for hot compilation units filtering.
     * @param renderingOptions Options for processing and rendering the trees.
     * @return TreeResponse containing the Inlining tree comparison.
     */
    @Get(value = "/inlining-tree")
    @Operation(summary = "Get Inlining Tree comparison")
    @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved Inlining Tree comparison.",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = TreeResponse.class)))
    @StandardErrorResponses
    public TreeResponse getComparedInliningTree(
            @Parameter(
                            description = "The absolute path to the directory containing the benchmark runs.",
                            required = true,
                            example = "/default-benchmarks")
                    @QueryValue("path")
                    String path,
            @Parameter(
                            description = "The directory name of the first run.",
                            required = true,
                            example = "sd2-graal25-valid-slow")
                    @QueryValue("runName1")
                    String runName1,
            @Parameter(
                            description = "The directory name of the second run.",
                            required = true,
                            example = "sd1-graal25-valid-fast")
                    @QueryValue("runName2")
                    String runName2,
            @Parameter(
                            description = "The full name of method and its arguments types.",
                            required = true,
                            example = "scala.collection.IterableOnceOps.count(Function1)")
                    @QueryValue("methodName")
                    String methodName,
            @Parameter(description = "The ID that compiler assigned to first run's Compilation Unit.", example = "2502")
                    @QueryValue("compilationId1")
                    @Nullable
                    String compilationId1,
            @Parameter(
                            description = "The ID that compiler assigned to second run's Compilation Unit.",
                            example = "2504")
                    @QueryValue("compilationId2")
                    @Nullable
                    String compilationId2,
            @Parameter(description = "Options defining the policy for hot compilation units filtering.")
                    @Valid
                    @RequestBean
                    HotPolicyOptions hotPolicyOptions,
            @Parameter(description = "Options for processing and rendering the experiment trees.") @RequestBean
                    ExperimentProcessingOptions renderingOptions) {
        LOGGER.info(
                "Requesting compared Inlining Tree for method '{}' with compilation units '{}' vs '{}'",
                methodName,
                compilationId1,
                compilationId2);
        return runsService.getComparedInliningTree(
                path,
                runName1,
                runName2,
                methodName,
                compilationId1,
                compilationId2,
                renderingOptions,
                hotPolicyOptions);
    }

    /**
     * Endpoint to retrieve the Optimization compared tree between two specified runs and their
     * compilation units.
     *
     * @param path Absolute path to the directory containing runs metadata.
     * @param runName1 Name of the first run.
     * @param runName2 Name of the second run.
     * @param methodName The full name of method and its arguments types.
     * @param compilationId1 The ID that compiler assigned to first run's Compilation Unit.
     * @param compilationId2 The ID that compiler assigned to second run's Compilation Unit.
     * @param hotPolicyOptions Options defining the policy for hot compilation units filtering.
     * @param renderingOptions Options for processing and rendering the trees.
     * @return TreeResponse containing the Optimization tree comparison.
     */
    @Get(value = "/optimization-tree")
    @Operation(summary = "Get Optimization Tree comparison")
    @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved Optimization Tree comparison.",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = TreeResponse.class)))
    @StandardErrorResponses
    public TreeResponse getComparedOptimizationTree(
            @Parameter(
                            description = "The absolute path to the directory containing the benchmark runs.",
                            required = true,
                            example = "/default-benchmarks")
                    @QueryValue("path")
                    String path,
            @Parameter(
                            description = "The directory name of the first run.",
                            required = true,
                            example = "sd2-graal25-valid-slow")
                    @QueryValue("runName1")
                    String runName1,
            @Parameter(
                            description = "The directory name of the second run.",
                            required = true,
                            example = "sd1-graal25-valid-fast")
                    @QueryValue("runName2")
                    String runName2,
            @Parameter(
                            description = "The full name of method and its arguments types.",
                            required = true,
                            example = "scala.collection.IterableOnceOps.count(Function1)")
                    @QueryValue("methodName")
                    String methodName,
            @Parameter(description = "The ID that compiler assigned to first run's Compilation Unit.", example = "2502")
                    @QueryValue("compilationId1")
                    @Nullable
                    String compilationId1,
            @Parameter(
                            description = "The ID that compiler assigned to second run's Compilation Unit.",
                            example = "2504")
                    @QueryValue("compilationId2")
                    @Nullable
                    String compilationId2,
            @Parameter(description = "Options defining the policy for hot compilation units filtering.")
                    @Valid
                    @RequestBean
                    HotPolicyOptions hotPolicyOptions,
            @Parameter(description = "Options for processing and rendering the experiment trees.") @RequestBean
                    ExperimentProcessingOptions renderingOptions) {
        LOGGER.info(
                "Requesting compared Optimization Tree for method '{}' with compilation units '{}' vs '{}'",
                methodName,
                compilationId1,
                compilationId2);
        return runsService.getComparedOptimizationTree(
                path,
                runName1,
                runName2,
                methodName,
                compilationId1,
                compilationId2,
                renderingOptions,
                hotPolicyOptions);
    }

    /**
     * Endpoint to retrieve the Optimization-Context comparison tree between two specified runs and
     * their compilation units.
     *
     * @param path Absolute path to the directory containing runs metadata.
     * @param runName1 Name of the first run.
     * @param runName2 Name of the second run.
     * @param methodName The full name of method and its arguments types.
     * @param compilationId1 The ID that compiler assigned to first run's Compilation Unit.
     * @param compilationId2 The ID that compiler assigned to second run's Compilation Unit.
     * @param hotPolicyOptions Options defining the policy for hot compilation units filtering.
     * @param renderingOptions Options for processing and rendering the trees.
     * @return TreeResponse containing the Optimization-Context tree comparison.
     */
    @Get(value = "/optimization-context-tree")
    @Operation(summary = "Get Optimization-Context Tree comparison")
    @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved Optimization-Context Tree comparison.",
            content =
                    @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = TreeResponse.class)))
    @StandardErrorResponses
    public TreeResponse getComparedOptimizationContextTree(
            @Parameter(
                            description = "The absolute path to the directory containing the benchmark runs.",
                            required = true,
                            example = "/default-benchmarks")
                    @QueryValue("path")
                    String path,
            @Parameter(
                            description = "The directory name of the first run.",
                            required = true,
                            example = "sd2-graal25-valid-slow")
                    @QueryValue("runName1")
                    String runName1,
            @Parameter(
                            description = "The directory name of the second run.",
                            required = true,
                            example = "sd1-graal25-valid-fast")
                    @QueryValue("runName2")
                    String runName2,
            @Parameter(
                            description = "The full name of method and its arguments types.",
                            required = true,
                            example = "scala.collection.IterableOnceOps.count(Function1)")
                    @QueryValue("methodName")
                    String methodName,
            @Parameter(description = "The ID that compiler assigned to first run's Compilation Unit.", example = "2502")
                    @QueryValue("compilationId1")
                    @Nullable
                    String compilationId1,
            @Parameter(
                            description = "The ID that compiler assigned to second run's Compilation Unit.",
                            example = "2504")
                    @QueryValue("compilationId2")
                    @Nullable
                    String compilationId2,
            @Parameter(description = "Options defining the policy for hot compilation units filtering.")
                    @Valid
                    @RequestBean
                    HotPolicyOptions hotPolicyOptions,
            @Parameter(description = "Options for processing and rendering the experiment trees.") @RequestBean
                    ExperimentProcessingOptions renderingOptions) {
        LOGGER.info(
                "Requesting compared Optimization-Context Tree for method '{}' with compilation units '{}' vs '{}'",
                methodName,
                compilationId1,
                compilationId2);
        return runsService.getComparedOptimizationContextTree(
                path,
                runName1,
                runName2,
                methodName,
                compilationId1,
                compilationId2,
                renderingOptions,
                hotPolicyOptions);
    }
}
