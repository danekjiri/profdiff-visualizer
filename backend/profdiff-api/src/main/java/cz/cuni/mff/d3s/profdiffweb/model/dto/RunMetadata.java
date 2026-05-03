package cz.cuni.mff.d3s.profdiffweb.model.dto;

import io.micronaut.core.annotation.Introspected;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Represents metadata about a profiling report, details about the run {@link BenchmarkRunMetadata},
 * and other statistics extracted from the {@link org.graalvm.profdiff.core.Experiment}.
 *
 * @param benchmarkRunMetadata Metadata about the profiling run.
 * @param executionId Unique identifier for the profiler execution.
 * @param totalPeriod Total time sampling period of the profiling run.
 * @param graalPeriod Sampling period during which GraalVM compiled methods were active.
 * @param compilationUnitsCount Total number of GraalVM compilation units in the run.
 * @param proftoolMethodsCount Total number of methods analyzed by profiler.
 */
@Introspected
public record RunMetadata(
        @Schema(description = "Metadata about the profiling run.", requiredMode = Schema.RequiredMode.REQUIRED)
                BenchmarkRunMetadata benchmarkRunMetadata,
        @Schema(
                        description = "Unique identifier for the profiler execution.",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                String executionId,
        @Schema(
                        description = "Total time sampling period of the profiling run.",
                        requiredMode = Schema.RequiredMode.NOT_REQUIRED)
                long totalPeriod,
        @Schema(
                        description = "Sampling period during which GraalVM compiled methods were active.",
                        requiredMode = Schema.RequiredMode.NOT_REQUIRED)
                long graalPeriod,
        @Schema(
                        description = "Total number of GraalVM compilation units in the run.",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                long compilationUnitsCount,
        @Schema(
                        description = "Total number of methods analyzed by profiler.",
                        requiredMode = Schema.RequiredMode.NOT_REQUIRED)
                long proftoolMethodsCount) {}
