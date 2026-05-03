package cz.cuni.mff.d3s.profdiffweb.model.dto;

import io.micronaut.core.annotation.Introspected;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Represents a compilation unit with its ID given by HotSpot VM compiler, measured running period,
 * and hot status.
 *
 * @param id Unique identifier given by GraalVM compiler.
 * @param period The number of cycles sampled by perf for the given compilation unit.
 * @param isHot Indication whether the Profdiff marked the compilation unit as "hot" - the method
 *     period is great enough.
 * @param isFragment Indication whether the Compilation Unit represents a subtree of a larger,
 *     original compilation.
 */
@Introspected
public record CompilationUnitMetadata(
        @Schema(
                        description = "Unique identifier given by GraalVM compiler.",
                        example = "2110",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                String id,
        @Schema(
                        description = "The number of cycles sampled by perf for the given compilation unit.",
                        example = "65889749",
                        requiredMode = Schema.RequiredMode.NOT_REQUIRED)
                long period,
        @Schema(
                        description = "Indication whether the Profdiff marked the compilation unit as 'hot'.",
                        example = "true",
                        requiredMode = Schema.RequiredMode.NOT_REQUIRED)
                boolean isHot,
        @Schema(
                        description =
                                "Indication whether the Compilation Unit represents a subtree of a larger, original compilation.",
                        example = "true",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                boolean isFragment) {}
