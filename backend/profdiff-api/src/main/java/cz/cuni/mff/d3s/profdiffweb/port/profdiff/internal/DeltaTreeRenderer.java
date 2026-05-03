package cz.cuni.mff.d3s.profdiffweb.port.profdiff.internal;

import cz.cuni.mff.d3s.profdiffweb.model.dto.RenderedTreeNode;
import cz.cuni.mff.d3s.profdiffweb.model.dto.RenderedTreeNode.Marker;
import cz.cuni.mff.d3s.profdiffweb.model.dto.RenderedTreeNode.NodeContent;
import java.util.ArrayList;
import java.util.List;
import org.graalvm.profdiff.core.ExperimentId;
import org.graalvm.profdiff.core.OptimizationContextTreeNode;
import org.graalvm.profdiff.core.OptionValues;
import org.graalvm.profdiff.core.TreeNode;
import org.graalvm.profdiff.core.inlining.InliningTreeNode;
import org.graalvm.profdiff.diff.DeltaTree;
import org.graalvm.profdiff.diff.DeltaTreeNode;

/**
 * Converts a {@link DeltaTree} into a fully structured {@link RenderedTreeNode} tree suitable for
 * JSON serialization and interactive rendering on the frontend.
 *
 * <p>A {@link DeltaTree} is parsed from the {@link org.graalvm.profdiff.diff.EditScript} produced
 * by the matcher. Instead of standard AST nodes, its nodes represent edit operations (insert, delete,
 * identity, relabel) between an old and new tree. This class maps those operational states into
 * visual DTOs.
 */
public class DeltaTreeRenderer {
    private DeltaTreeRenderer() {}

    /**
     * Converts the root of the given delta tree into a DTO node.
     */
    public static <T extends TreeNode<T>> RenderedTreeNode convert(DeltaTree<T> deltaTree, OptionValues options) {
        DeltaTreeNode<T> root = deltaTree.getRoot();
        if (root == null) {
            return new RenderedTreeNode(Marker.NEUTRAL, ProfdiffMapper.createRawContent("(empty)"), List.of());
        }

        LineCapturingWriter writer = new LineCapturingWriter(options);
        return convertNode(root, writer);
    }

    /**
     * Recursively converts a delta node into a DTO.
     *
     * <p>Content extraction depends on the edit operation:
     * <li><b>Relabeling</b> - node exists in both trees but differs. The old and new states are merged into a single content node indicating the transition.</li>
     * <li><b>Insertion</b> - node only exists in the new tree. Captures strictly from the right.</li>
     * <li><b>Deletion and Identity</b> - node exists in the old tree (either removed or unchanged). Captures strictly from the left.</li>
     */
    private static <T extends TreeNode<T>> RenderedTreeNode convertNode(
            DeltaTreeNode<T> deltaNode, LineCapturingWriter writer) {
        Marker marker = resolveMarker(deltaNode);
        var options = writer.getOptionValues();
        NodeContent content;

        // extract diffs node content based on tree presence
        if (deltaNode.isRelabeling()) {
            content = ProfdiffMapper.createRelabelContent(deltaNode.getLeft(), deltaNode.getRight(), options);
        } else if (deltaNode.isInsertion()) {
            content = ProfdiffMapper.createNodeContent(deltaNode.getRight(), options);
        } else {
            content = ProfdiffMapper.createNodeContent(deltaNode.getLeft(), options);
        }

        List<RenderedTreeNode> children = new ArrayList<>();

        // extract underlying content such as profile/reasoning
        InliningTreeNode leftNode = getUnderlyingInliningNode(deltaNode.getLeft());
        InliningTreeNode rightNode = getUnderlyingInliningNode(deltaNode.getRight());

        if (deltaNode.isIdentity()) {
            if (leftNode != null) {
                leftNode.writeReasoningIfEnabled(writer, ExperimentId.ONE);
                leftNode.writeReceiverTypeProfile(writer, ExperimentId.ONE);
            }
            if (rightNode != null) {
                rightNode.writeReasoningIfEnabled(writer, ExperimentId.TWO);
                rightNode.writeReceiverTypeProfile(writer, ExperimentId.TWO);
            }
        } else if (deltaNode.isRelabeling()) {
            boolean positivityChanged =
                    leftNode != null && rightNode != null && (leftNode.isPositive() != rightNode.isPositive());
            if (leftNode != null && (options.shouldAlwaysPrintInlinerReasoning() || positivityChanged)) {
                leftNode.writeReasoning(writer, ExperimentId.ONE);
            }
            if (rightNode != null && (options.shouldAlwaysPrintInlinerReasoning() || positivityChanged)) {
                rightNode.writeReasoning(writer, ExperimentId.TWO);
            }

            // profiles unconditionally
            if (leftNode != null) leftNode.writeReceiverTypeProfile(writer, ExperimentId.ONE);
            if (rightNode != null) rightNode.writeReceiverTypeProfile(writer, ExperimentId.TWO);
        } else {
            InliningTreeNode single = leftNode != null ? leftNode : rightNode;
            if (single != null) {
                single.writeReasoningIfEnabled(writer, null);
                single.writeReceiverTypeProfile(writer, null);
            }
        }

        for (String line : writer.takeLines()) {
            if (!line.isBlank()) {
                children.add(
                        new RenderedTreeNode(Marker.INFO, ProfdiffMapper.createRawContent(line.trim()), List.of()));
            }
        }

        // recurse
        for (DeltaTreeNode<T> child : deltaNode.getChildren()) {
            children.add(convertNode(child, writer));
        }

        return new RenderedTreeNode(marker, content, children);
    }

    /**
     * Maps Profdiff's internal delta states to the frontend's visual Marker enum.
     */
    private static <T extends TreeNode<T>> Marker resolveMarker(DeltaTreeNode<T> node) {
        if (node.isInfoNode()) return Marker.INFO;
        if (node.isIdentity()) return Marker.IDENTITY;
        if (node.isInsertion()) return Marker.INSERT;
        if (node.isDeletion()) return Marker.DELETE;
        if (node.isRelabeling()) return Marker.RELABEL;
        return Marker.NEUTRAL;
    }

    /**
     * Safely extracts the underlying inlining node to access reasoning and profile data.
     *
     * @param node The abstract tree node.
     */
    private static <T extends TreeNode<T>> InliningTreeNode getUnderlyingInliningNode(T node) {
        if (node == null) return null;

        if (node instanceof InliningTreeNode inNode) return inNode;
        if (node instanceof OptimizationContextTreeNode ctxNode) return ctxNode.getOriginalInliningTreeNode();
        return null;
    }
}
