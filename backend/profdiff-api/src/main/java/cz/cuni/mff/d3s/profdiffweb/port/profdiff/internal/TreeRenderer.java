package cz.cuni.mff.d3s.profdiffweb.port.profdiff.internal;

import cz.cuni.mff.d3s.profdiffweb.model.dto.RenderedTreeNode;
import cz.cuni.mff.d3s.profdiffweb.model.dto.RenderedTreeNode.Marker;
import java.util.ArrayList;
import java.util.List;
import org.graalvm.profdiff.core.OptimizationContextTreeNode;
import org.graalvm.profdiff.core.OptionValues;
import org.graalvm.profdiff.core.TreeNode;
import org.graalvm.profdiff.core.inlining.InliningTreeNode;

/**
 * Converts a standard {@link TreeNode} (Inlining/Optimization) into a {@link RenderedTreeNode} hierarchy.
 *
 * <p>Used for the <b>Report View</b> (single run). Unlike {@link DeltaTreeRenderer}, which processes
 * structural edit operations between two trees, this handles standard AST nodes. The resulting DTO tree
 * is rendered by the same frontend component, just without diff highlighting (all nodes are mapped to
 * {@code NEUTRAL} or {@code INFO}).
 */
public class TreeRenderer {
    private TreeRenderer() {}

    /**
     * Converts the root of a standard Profdiff tree into a DTO node.
     */
    public static <T extends TreeNode<T>> RenderedTreeNode convert(T root, OptionValues options) {
        if (root == null) {
            return new RenderedTreeNode(Marker.NEUTRAL, ProfdiffMapper.createRawContent("(empty)"), List.of());
        }

        LineCapturingWriter writer = new LineCapturingWriter(options);
        return convertNode(root, writer);
    }

    /**
     * Recursively converts a standard tree node and its children into a DTO hierarchy.
     *
     * <p>If the node is an {@link InliningTreeNode}, any attached reasoning or profiling data
     * is captured via the {@link LineCapturingWriter} and appended as virtual {@code INFO} child nodes
     * so it can be rendered as part of the visual tree hierarchy.
     */
    private static <T extends TreeNode<T>> RenderedTreeNode convertNode(T node, LineCapturingWriter writer) {
        Marker marker = node.isInfoNode() ? Marker.INFO : Marker.NEUTRAL;
        RenderedTreeNode.NodeContent content = ProfdiffMapper.createNodeContent(node, writer.getOptionValues());

        List<RenderedTreeNode> children = new ArrayList<>();
        InliningTreeNode inliningNode = getUnderlyingInliningNode(node);
        // extract underlying content such as profile/reasoning as child nodes
        if (inliningNode != null) {
            inliningNode.writeReasoningIfEnabled(writer, null);
            inliningNode.writeReceiverTypeProfile(writer, null);

            for (String line : writer.takeLines()) {
                if (!line.isBlank()) {
                    children.add(
                            new RenderedTreeNode(Marker.INFO, ProfdiffMapper.createRawContent(line.trim()), List.of()));
                }
            }
        }

        for (T child : node.getChildren()) {
            children.add(convertNode(child, writer));
        }

        return new RenderedTreeNode(marker, content, children);
    }

    /**
     * Safely extracts the underlying inlining node to access reasoning and profile data.
     * <p>Necessary because an {@link OptimizationContextTreeNode} wraps the original inlining node.
     */
    private static <T extends TreeNode<T>> InliningTreeNode getUnderlyingInliningNode(T node) {
        if (node == null) return null;

        if (node instanceof InliningTreeNode inNode) return inNode;
        if (node instanceof OptimizationContextTreeNode ctxNode) return ctxNode.getOriginalInliningTreeNode();
        return null;
    }
}
