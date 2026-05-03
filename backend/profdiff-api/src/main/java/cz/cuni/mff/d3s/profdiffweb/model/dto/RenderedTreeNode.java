package cz.cuni.mff.d3s.profdiffweb.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.micronaut.core.annotation.Introspected;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Structured representation of a node in the UI used for highlighting,...
 *
 * @param marker The diff status of this node (e.g., INSERT, DELETED, ...).
 * @param children List of child nodes beneath this node in the hierarchy.
 */
@Introspected
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RenderedTreeNode(
        @Schema(
                        description = "The comparison status of this node.",
                        example = "INSERT",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                Marker marker,
        @Schema(
                        description =
                                "The primary content to render (represents the only state for IDENTITY, INSERT, DELETE, and the NEW state for RELABEL).",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                NodeContent content,
        @Schema(
                        description = "List of child nodes (if no children, then an empty array passed).",
                        requiredMode = Schema.RequiredMode.REQUIRED)
                List<RenderedTreeNode> children) {

    /**
     * The payload of the tree node.
     *
     * <p><b>Data Invariant:</b> This record represents a union type. Either the {@code rawText} field is populated
     * (used for {@link Marker#INFO} or fallback nodes) while the structured fields are {@code null}, OR the structured
     * fields ({@code action}, {@code methodName}, etc.) are populated while {@code rawText} is {@code null}.
     */
    @Introspected
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record NodeContent(
            @Schema(
                            description = "Action prefix, e.g. (devirtualized -> indirect), or Optimization Phases...",
                            example = "(inlined)",
                            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
                    String action,
            @Schema(
                            description = "The target method name, if applicable.",
                            example = " java.util.Random.next(int) ",
                            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
                    String methodName,
            @Schema(
                            description = "The BCI or BCI trace string.",
                            example = "3",
                            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
                    String bci,
            @Schema(
                            description = "Additional property details.",
                            example = "{peelings: 1}",
                            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
                    String additionalInfo,
            @Schema(
                            description = "Raw fallback text if parsing fails or for INFO nodes.",
                            example = "|_ receiver-type profile",
                            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
                    String rawText) {}

    public enum Marker {
        IDENTITY, // no change (gray)
        INSERT, // present in run 2 but not run 1 (green)
        DELETE, // present in run 1 but not run 2 (red)
        RELABEL, // node exists in both but content changed (blue)
        INFO, // profiler info or metadata (italic gray)
        NEUTRAL // default or root
    }
}
