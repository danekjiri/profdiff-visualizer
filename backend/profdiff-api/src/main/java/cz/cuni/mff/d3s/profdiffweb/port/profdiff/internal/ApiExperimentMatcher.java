package cz.cuni.mff.d3s.profdiffweb.port.profdiff.internal;

import cz.cuni.mff.d3s.profdiffweb.model.dto.RenderedTreeNode;
import org.graalvm.profdiff.core.CompilationUnit;
import org.graalvm.profdiff.core.OptimizationContextTree;
import org.graalvm.profdiff.core.OptimizationContextTreeNode;
import org.graalvm.profdiff.core.OptionValues;
import org.graalvm.profdiff.core.inlining.InliningTree;
import org.graalvm.profdiff.core.inlining.InliningTreeNode;
import org.graalvm.profdiff.core.optimization.OptimizationTree;
import org.graalvm.profdiff.core.optimization.OptimizationTreeNode;
import org.graalvm.profdiff.diff.DeltaTree;
import org.graalvm.profdiff.diff.EditScript;
import org.graalvm.profdiff.diff.InliningTreeEditPolicy;
import org.graalvm.profdiff.diff.OptimizationContextTreeEditPolicy;
import org.graalvm.profdiff.diff.OptimizationTreeEditPolicy;
import org.graalvm.profdiff.diff.TreeMatcher;

/**
 * A custom equivalent to Profdiff's internal {@link org.graalvm.profdiff.command.ExperimentMatcher}.
 *
 * <p>While the library's {@code ExperimentMatcher} relies heavily on a {@link org.graalvm.profdiff.core.Writer}
 * to print the calculated differences into a massive, formatted plain-text string, the API requires the diffs
 * to be returned as a structured, hierarchical JSON payload for the frontend UI.
 *
 * <p>It replicates the core diffing logic (matching algorithms, edit scripts, and DeltaTree
 * pruning/expansion) but delegates the final output generation to {@link DeltaTreeRenderer}, which
 * outputs a {@link RenderedTreeNode} DTO instead of text.
 */
public final class ApiExperimentMatcher {
    /// Instantiate the core Profdiff matchers using their respective cost policies.
    private final TreeMatcher<InliningTreeNode> inliningMatcher = new TreeMatcher<>(new InliningTreeEditPolicy());
    private final TreeMatcher<OptimizationTreeNode> optimizationMatcher =
            new TreeMatcher<>(new OptimizationTreeEditPolicy());
    private final TreeMatcher<OptimizationContextTreeNode> contextMatcher =
            new TreeMatcher<>(new OptimizationContextTreeEditPolicy());

    /**
     * Compares two Inlining Trees.
     * <p>If requested via {@code options}, unchanged (identity) nodes are stripped from the payload.
     * The delta is always expanded to unpack bulk subtree edits into individual node operations for the UI.
     */
    public RenderedTreeNode getInliningTreeDiff(InliningTree tree1, InliningTree tree2, OptionValues options) {
        EditScript<InliningTreeNode> script = inliningMatcher.match(tree1.getRoot(), tree2.getRoot());
        DeltaTree<InliningTreeNode> delta = DeltaTree.fromEditScript(script);
        if (options.shouldPruneIdentities()) {
            delta.pruneIdentities();
        }
        delta.expand();

        return DeltaTreeRenderer.convert(delta, options);
    }

    /**
     * Compares two Optimization Trees.
     * <p>If requested via {@code options}, unchanged (identity) nodes are stripped from the payload.
     * The delta is always expanded to unpack bulk subtree edits into individual node operations for the UI.
     */
    public RenderedTreeNode getOptimizationTreeDiff(
            OptimizationTree tree1, OptimizationTree tree2, OptionValues options) {
        EditScript<OptimizationTreeNode> script = optimizationMatcher.match(tree1.getRoot(), tree2.getRoot());
        DeltaTree<OptimizationTreeNode> delta = DeltaTree.fromEditScript(script);
        if (options.shouldPruneIdentities()) {
            delta.pruneIdentities();
        }
        delta.expand();

        return DeltaTreeRenderer.convert(delta, options);
    }

    /**
     * Compares Optimization-Context Trees.
     * <p>Takes {@link CompilationUnit.TreePair}s because the context tree must first be constructed
     * by merging the inlining and optimization trees into a single traversable tree before diffing.
     * Unchanged nodes are stripped if requested, and subtree edits are expanded into node operations.
     */
    public RenderedTreeNode getOptimizationContextTreeDiff(
            CompilationUnit.TreePair treePair1, CompilationUnit.TreePair treePair2, OptionValues options) {

        OptimizationContextTree ctx1 =
                OptimizationContextTree.createFrom(treePair1.getInliningTree(), treePair1.getOptimizationTree());
        OptimizationContextTree ctx2 =
                OptimizationContextTree.createFrom(treePair2.getInliningTree(), treePair2.getOptimizationTree());

        EditScript<OptimizationContextTreeNode> script = contextMatcher.match(ctx1.getRoot(), ctx2.getRoot());
        DeltaTree<OptimizationContextTreeNode> delta = DeltaTree.fromEditScript(script);
        if (options.shouldPruneIdentities()) {
            delta.pruneIdentities();
        }
        delta.expand();

        return DeltaTreeRenderer.convert(delta, options);
    }
}
