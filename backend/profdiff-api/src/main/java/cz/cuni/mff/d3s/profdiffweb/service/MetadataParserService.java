package cz.cuni.mff.d3s.profdiffweb.service;

import cz.cuni.mff.d3s.profdiffweb.model.dto.ParsedBenchResults;
import cz.cuni.mff.d3s.profdiffweb.model.dto.ProfileMetadata;
import java.nio.file.Path;

/** Parses metadata from benchmark and profiler files. */
public interface MetadataParserService {
    /**
     * Parses benchmark results metadata from the specified file.
     *
     * @param filePath The path to the benchmark results file.
     * @throws RunsFileParsingException if there is an error during parsing, such as an unexpected
     *     JSON structure, malformed content, or I/O issues reading the file.
     */
    ParsedBenchResults parseBenchResults(Path filePath) throws RunsFileParsingException;

    /**
     * Parses profiler metadata from the specified file.
     *
     * @param filePath The path to the profiler metadata file.
     * @throws RunsFileParsingException if there is an error during parsing, such as an unexpected
     *     JSON structure, malformed content, or I/O issues reading the file.
     */
    ProfileMetadata parseProfilerMetadata(Path filePath) throws RunsFileParsingException;
}
