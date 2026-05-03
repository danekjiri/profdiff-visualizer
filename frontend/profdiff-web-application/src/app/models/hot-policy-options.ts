/**
 * DTO for Hot Compilation Policy options.
 */
export interface HotPolicyOptions {
  /**
   * Minimum limit for hot compilation.
   */
  hotMinLimit?: number;
  /**
   * Maximum limit for hot compilation.
   */
  hotMaxLimit?: number;
  /**
   * Percentile used for hot compilation.
   */
  hotPercentile?: number;
}
