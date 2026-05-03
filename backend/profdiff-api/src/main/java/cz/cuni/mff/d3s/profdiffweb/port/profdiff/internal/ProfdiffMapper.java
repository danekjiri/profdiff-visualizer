package cz.cuni.mff.d3s.profdiffweb.port.profdiff.internal;

import cz.cuni.mff.d3s.profdiffweb.model.dto.*;
import java.util.stream.StreamSupport;
import org.graalvm.collections.UnmodifiableEconomicMap;
import org.graalvm.profdiff.core.*;
import org.graalvm.profdiff.core.inlining.InliningTreeNode;
import org.graalvm.profdiff.core.optimization.Optimization;
import org.graalvm.profdiff.core.optimization.OptimizationTreeNode;

/**
 * A stateless utility mapper responsible for converting internal Profdiff CLI objects
 * to application-specific DTOs.
 *
 * <p>Serves as the structural bridge ensuring the REST API contract remains strictly decoupled from Profdiff internals.
 */
public final class ProfdiffMapper {
    private ProfdiffMapper() {}

    /**
     * Bridges the API's logically separated configuration DTO back into the singular format the library demands.
     */
    public static OptionValues toOptionValues(
            ExperimentProcessingOptions renderingOptions, HotCompilationUnitPolicy policy) {
        return new OptionValues(
                policy,
                false,
                true,
                renderingOptions.longBci() != null ? renderingOptions.longBci() : false,
                renderingOptions.sortInliningTree() != null ? renderingOptions.sortInliningTree() : false,
                renderingOptions.sortUnorderedPhases() != null ? renderingOptions.sortUnorderedPhases() : false,
                renderingOptions.removeDetailedPhases() != null ? renderingOptions.removeDetailedPhases() : false,
                renderingOptions.pruneIdentities() != null ? renderingOptions.pruneIdentities() : false,
                renderingOptions.createFragments() != null ? renderingOptions.createFragments() : false,
                renderingOptions.inlinerReasoning() != null ? renderingOptions.inlinerReasoning() : false);
    }

    /**
     * Extracts hotness thresholds from the API DTO into Profdiff's policy object.
     */
    public static HotCompilationUnitPolicy toHotPolicy(HotPolicyOptions dto) {
        HotCompilationUnitPolicy policy = new HotCompilationUnitPolicy();
        if (dto.hotMinLimit() != null) policy.setHotMinLimit(dto.hotMinLimit());
        if (dto.hotMaxLimit() != null) policy.setHotMaxLimit(dto.hotMaxLimit());
        if (dto.hotPercentile() != null) policy.setHotPercentile(dto.hotPercentile());
        return policy;
    }

    /**
     * Converts a {@link Method} and its compilation units into a DTO.
     *
     * <p>Strips away reference to the Experiment, keeping only the flat metadata required for the frontend's method list.
     */
    public static JavaMethod mapMethod(Method profdiffMethod) {
        var units = profdiffMethod.getCompilationUnits().stream()
                .map(u -> new CompilationUnitMetadata(
                        u.getCompilationId(), u.getPeriod(), u.isHot(), u instanceof CompilationFragment))
                .toList();
        return new JavaMethod(profdiffMethod.getMethodName(), units, profdiffMethod.getTotalPeriod());
    }

    /**
     * Aggregates nested experiment statistics into a single, flat summary object for the run overview UI.
     */
    public static RunMetadata mapRunMetadata(BenchmarkRunMetadata benchmarkRunMetadata, Experiment experiment) {
        long compilationUnitsCount = StreamSupport.stream(
                        experiment.getMethodsByName().getValues().spliterator(), false)
                .mapToLong(method -> method.getCompilationUnits().size())
                .sum();

        return new RunMetadata(
                benchmarkRunMetadata,
                experiment.getExecutionId(),
                experiment.getTotalPeriod(),
                experiment.getGraalPeriod(),
                compilationUnitsCount,
                experiment.getMethodsByName().size());
    }

    /**
     * Factory method to create a TopMethod from a {@link ProftoolMethod} and total profiling period.
     */
    public static TopMethod mapTopMethod(ProftoolMethod method, long totalPeriod) {
        double execution = ((double) method.getPeriod() / totalPeriod) * 100;
        double cycles = (double) method.getPeriod() / ProftoolMethod.BILLION;
        return new TopMethod(
                String.format("%.2f%%", execution),
                String.format("%.2f", cycles),
                method.getLevel(),
                method.getCompilationId(),
                method.getName());
    }

    /**
     * Creates a fallback or purely informational content node containing only raw text.
     */
    public static RenderedTreeNode.NodeContent createRawContent(String text) {
        return new RenderedTreeNode.NodeContent(null, null, null, null, text);
    }

    /**
     * Extracts structured data from an InliningTreeNode.
     * Captures the type of inlining action, the target method, and the bytecode index (BCI).
     */
    public static RenderedTreeNode.NodeContent buildInliningContent(InliningTreeNode node) {
        String action = node.getCallsiteKind().prefix();
        String methodName = node.getName() == null ? InliningTreeNode.UNKNOWN_NAME : node.getName();
        String bci = node.getBCI() == Optimization.UNKNOWN_BCI ? null : String.valueOf(node.getBCI());

        return new RenderedTreeNode.NodeContent(action, methodName, bci, null, null);
    }

    /**
     * Extracts structured data from an Optimization node.
     * Captures the compiler phase/event, the position in the code, and any attached properties.
     *
     * @param options Formatting options (determines if BCI should be long-form).
     */
    public static RenderedTreeNode.NodeContent buildOptimizationContent(Optimization node, OptionValues options) {
        String action = node.getName();
        if (node.getEventName() != null && !node.getEventName().isEmpty()) {
            action += " " + node.getEventName();
        }

        String bci = null;
        if (node.getPosition() != null && !node.getPosition().isEmpty()) {
            bci = node.getPosition().toString(options.isBCILongForm(), null).trim();
        }

        String props = null;
        if (node.getProperties() != null && !node.getProperties().isEmpty()) {
            props = formatProperties(node.getProperties());
        }

        return new RenderedTreeNode.NodeContent(action, null, bci, props, null);
    }

    /**
     * Stringify Profdiff's UnmodifiableEconomicMap of properties into a JSON-like format.
     */
    private static String formatProperties(UnmodifiableEconomicMap<String, Object> properties) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (String key : properties.getKeys()) {
            if (!first) sb.append(", ");
            Object value = properties.get(key);
            sb.append(key).append(": ");
            if (value instanceof String) {
                sb.append("\"").append(value).append("\"");
            } else {
                sb.append(value);
            }
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * Creates a merged content node for a RELABEL operation, showing the transition from the old to the new state.
     *
     * <p>If the nodes are plain optimization nodes (or an OptimizationContextTreeNode with no inlining node),
     * this method falls back to mapping strictly the {@code right} (new) state, deliberately dropping the old state.
     */
    public static <T extends TreeNode<T>> RenderedTreeNode.NodeContent createRelabelContent(
            T left, T right, OptionValues options) {
        if (left instanceof InliningTreeNode leftInNode && right instanceof InliningTreeNode rightInNode) {
            String action =
                    InliningTreeNode.CallsiteKind.change(leftInNode.getCallsiteKind(), rightInNode.getCallsiteKind());
            String methodName = leftInNode.getName() == null ? InliningTreeNode.UNKNOWN_NAME : leftInNode.getName();
            String bci = leftInNode.getBCI() == Optimization.UNKNOWN_BCI ? null : String.valueOf(leftInNode.getBCI());
            return new RenderedTreeNode.NodeContent(action.trim(), methodName, bci, null, null);
        }

        if (left instanceof OptimizationContextTreeNode leftCtx
                && right instanceof OptimizationContextTreeNode rightCtx) {
            if (leftCtx.getOriginalInliningTreeNode() != null && rightCtx.getOriginalInliningTreeNode() != null) {
                String action = InliningTreeNode.CallsiteKind.change(
                        leftCtx.getOriginalInliningTreeNode().getCallsiteKind(),
                        rightCtx.getOriginalInliningTreeNode().getCallsiteKind());
                String methodName = leftCtx.getOriginalInliningTreeNode().getName() == null
                        ? InliningTreeNode.UNKNOWN_NAME
                        : leftCtx.getOriginalInliningTreeNode().getName();
                String bci = leftCtx.getOriginalInliningTreeNode().getBCI() == Optimization.UNKNOWN_BCI
                        ? null
                        : String.valueOf(leftCtx.getOriginalInliningTreeNode().getBCI());
                return new RenderedTreeNode.NodeContent(action.trim(), methodName, bci, null, null);
            }
        }

        // fallback for optimizations or other node types (loses left state)
        return createNodeContent(right, options);
    }

    /**
     * The main factory method that takes a generic Profdiff's TreeNode and delegates
     * it to the appropriate specific content builder based on its concrete type.
     */
    public static <T extends TreeNode<T>> RenderedTreeNode.NodeContent createNodeContent(T node, OptionValues options) {
        if (node == null) {
            return null;
        }

        if (node.isInfoNode()) {
            return ProfdiffMapper.createRawContent(node.getName());
        }

        if (node instanceof InliningTreeNode inNode) {
            return ProfdiffMapper.buildInliningContent(inNode);
        }

        if (node instanceof OptimizationContextTreeNode ctxNode) {
            if (ctxNode.isInfoNode()) {
                return ProfdiffMapper.createRawContent(ctxNode.getName());
            } else if (ctxNode.getOriginalInliningTreeNode() != null) {
                return ProfdiffMapper.buildInliningContent(ctxNode.getOriginalInliningTreeNode());
            } else if (ctxNode.getOriginalOptimization() != null) {
                return ProfdiffMapper.buildOptimizationContent(ctxNode.getOriginalOptimization(), options);
            }
        }

        if (node instanceof Optimization optNode) {
            return ProfdiffMapper.buildOptimizationContent(optNode, options);
        }
        if (node instanceof OptimizationTreeNode optTreeNode) {
            return new RenderedTreeNode.NodeContent(optTreeNode.getName(), null, null, null, null);
        }

        // fallback
        return ProfdiffMapper.createRawContent(node.getName());
    }
}
