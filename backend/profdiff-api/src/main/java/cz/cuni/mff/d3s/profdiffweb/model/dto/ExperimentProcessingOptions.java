package cz.cuni.mff.d3s.profdiffweb.model.dto;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.annotation.Nullable;
import io.swagger.v3.oas.annotations.media.Schema;
import org.graalvm.profdiff.core.OptionValues;

/**
 * DTO for Tree Rendering options mirroring the Profdiff's {@link OptionValues}.
 *
 * <p>Does NOT include Hotness options, diffCompilations, or optimizationContextTree.
 *
 * @param longBci Whether to use long byte code indices.
 * @param sortInliningTree Whether to sort the inlining tree nodes.
 * @param sortUnorderedPhases Whether to sort unordered compiler phases.
 * @param removeDetailedPhases Whether to remove highly detailed compiler phases from the output.
 * @param pruneIdentities Whether to prune identity transformations.
 * @param createFragments Whether to create compilation fragments.
 * @param inlinerReasoning Whether to include inliner reasoning in the tree output.
 */
@Introspected
public record ExperimentProcessingOptions(
        @Schema(
                        description = "Whether to use long byte code indices.",
                        example = "false",
                        requiredMode = Schema.RequiredMode.NOT_REQUIRED)
                @Nullable
                Boolean longBci,
        @Schema(
                        description = "Whether to sort the inlining tree nodes.",
                        example = "true",
                        requiredMode = Schema.RequiredMode.NOT_REQUIRED)
                @Nullable
                Boolean sortInliningTree,
        @Schema(
                        description = "Whether to sort unordered compiler phases.",
                        example = "true",
                        requiredMode = Schema.RequiredMode.NOT_REQUIRED)
                @Nullable
                Boolean sortUnorderedPhases,
        @Schema(
                        description = "Whether to remove highly detailed compiler phases from the output.",
                        example = "true",
                        requiredMode = Schema.RequiredMode.NOT_REQUIRED)
                @Nullable
                Boolean removeDetailedPhases,
        @Schema(
                        description = "Whether to prune identity transformations.",
                        example = "true",
                        requiredMode = Schema.RequiredMode.NOT_REQUIRED)
                @Nullable
                Boolean pruneIdentities,
        @Schema(
                        description = "Whether to create compilation fragments.",
                        example = "true",
                        requiredMode = Schema.RequiredMode.NOT_REQUIRED)
                @Nullable
                Boolean createFragments,
        @Schema(
                        description = "Whether to include inliner reasoning in the tree output.",
                        example = "false",
                        requiredMode = Schema.RequiredMode.NOT_REQUIRED)
                @Nullable
                Boolean inlinerReasoning) {}
