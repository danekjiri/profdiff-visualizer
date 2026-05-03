/**
 * DTO for Tree Rendering options.
 *
 * Does NOT include Hotness options, diffCompilations, or optimizationContextTree.
 */
export interface ExperimentProcessingOptions {
  /**
   * Whether to use long Bytecode Indices.
   */
  longBci?: boolean;
  /**
   * Whether to sort the inlining tree.
   */
  sortInliningTree?: boolean;
  /**
   * Whether to sort unordered phases.
   */
  sortUnorderedPhases?: boolean;
  /**
   * Whether to remove detailed phases from the view.
   */
  removeDetailedPhases?: boolean;
  /**
   * Whether to prune identities in the tree.
   */
  pruneIdentities?: boolean;
  /**
   * Whether to create fragments for the graph.
   */
  createFragments?: boolean;
  /**
   * Whether to include inliner reasoning.
   */
  inlinerReasoning?: boolean;
}
