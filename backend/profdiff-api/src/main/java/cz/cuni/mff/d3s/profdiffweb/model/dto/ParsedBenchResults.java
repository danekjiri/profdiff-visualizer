package cz.cuni.mff.d3s.profdiffweb.model.dto;

import java.util.List;

/**
 * Helps extract inconsistency warnings when parsing Bench-results.json without throwing.
 *
 * @param metadata Parsed benchmark results metadata.
 * @param warnings A list of non-fatal warnings (e.g., inconsistent suite names).
 */
public record ParsedBenchResults(BenchResultsMetadata metadata, List<WarningMessage> warnings) {}
