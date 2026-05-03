package cz.cuni.mff.d3s.profdiffweb.model.dto;

import io.micronaut.core.annotation.Introspected;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.graalvm.profdiff.core.OptimizationContextTree;
import org.graalvm.profdiff.core.inlining.InliningTree;
import org.graalvm.profdiff.core.optimization.OptimizationTree;

/**
 * A single tree response representing either an {@link InliningTree}, {@link OptimizationTree}, or
 * {@link OptimizationContextTree} used for the Report and Comparison views.
 *
 * <p>The tree is converted into a fully structured {@link RenderedTreeNode} hierarchy suitable for
 * JSON serialization and interactive rendering on the frontend.
 *
 * @param tree The structured hierarchy representation of the requested tree.
 * @param warnings Non-fatal warnings that occurred during experiment parsing.
 */
@Introspected
public record TreeResponse(
        @Schema(
                        description = "The structured representation of the requested compilation tree.",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                RenderedTreeNode tree,
        @Schema(
                        description = "Non-fatal warnings that occurred during experiment parsing.",
                        example = "[METADATA_MISSING] Bench-results.json not found",
                        requiredMode = Schema.RequiredMode.NOT_REQUIRED)
                List<WarningMessage> warnings) {}
