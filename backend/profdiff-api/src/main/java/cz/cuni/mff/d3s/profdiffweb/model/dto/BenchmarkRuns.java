package cz.cuni.mff.d3s.profdiffweb.model.dto;

import io.micronaut.core.annotation.Introspected;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Wrapper for metadata ({@link BenchmarkRunMetadata}) about all runs found in given path carrying
 * warnings occurred while parsing.
 *
 * @param benchmarkRuns All runs metadata found on given path.
 * @param generalWarnings General warnings occurred during runs' metadata parsing.
 */
@Introspected
public record BenchmarkRuns(
        @Schema(description = "All runs metadata found on given path", requiredMode = Schema.RequiredMode.REQUIRED)
                List<BenchmarkRunMetadata> benchmarkRuns,
        @Schema(
                        description = "General warnings occurred during runs' metadata parsing",
                        requiredMode = Schema.RequiredMode.NOT_REQUIRED)
                List<WarningMessage> generalWarnings) {}
