package cz.cuni.mff.d3s.profdiffweb.model.dto;

import io.micronaut.core.annotation.Introspected;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Represents all parsed metadata of benchmark run.
 *
 * <p>NOTE: The object is only created if run contains Optimization's Log Directory. The profiler
 * file and the bench-results.json file are optional, so the object can be created even if one of
 * them is missing.
 *
 * @param runName Name of the run, which is the name of the subdirectory containing the profiles and
 *     optimizations logs.
 * @param benchResultsMetadata Metadata from the bench-results.json file, if not found then null.
 * @param profileMetadata Metadata from the profiler JSON file, if not found then null.
 * @param warnings Specific warnings to this benchmark run occurred during metadata parsing.
 */
@Introspected
public record BenchmarkRunMetadata(
        @Schema(example = "run_20250506_170013_15", requiredMode = Schema.RequiredMode.REQUIRED) String runName,
        @Schema(
                        description = "Metadata from the bench-results.json file, if not found then null.",
                        requiredMode = Schema.RequiredMode.NOT_REQUIRED)
                BenchResultsMetadata benchResultsMetadata,
        @Schema(
                        description = "Metadata from the profiler JSON file, if not found then null.",
                        requiredMode = Schema.RequiredMode.NOT_REQUIRED)
                ProfileMetadata profileMetadata,
        @Schema(
                        description = "Specific warnings to this benchmark run occurred during metadata parsing",
                        requiredMode = Schema.RequiredMode.NOT_REQUIRED)
                List<WarningMessage> warnings) {}
