package cz.cuni.mff.d3s.profdiffweb.model.dto;

import io.micronaut.core.annotation.Introspected;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Represents a directory within the workspace. Used to provide path autocomplete suggestions and
 * visually indicate which directories are valid benchmark roots.
 *
 * @param path The absolute or normalized path of the directory.
 * @param hasRuns Flag indicating whether this directory contains valid benchmark runs.
 */
@Introspected
public record WorkspaceDirectory(
        @Schema(
                        description = "The absolute or normalized path of the directory.",
                        example = "/workspace/benchmarks/scrabble",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                String path,
        @Schema(
                        description = "Flag indicating whether this directory contains valid benchmark runs.",
                        example = "true",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                boolean hasRuns) {}
