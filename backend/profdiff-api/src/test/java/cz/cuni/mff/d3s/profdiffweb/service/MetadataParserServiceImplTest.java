package cz.cuni.mff.d3s.profdiffweb.service;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.cuni.mff.d3s.profdiffweb.model.dto.BenchResultsMetadata;
import cz.cuni.mff.d3s.profdiffweb.model.dto.ParsedBenchResults;
import cz.cuni.mff.d3s.profdiffweb.model.dto.ProfileMetadata;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MetadataParserServiceImplTest {

    private MetadataParserService metadataParser;

    private Path runDirectory;
    private Path malformedRunDirectory;

    @BeforeEach
    void setUp() throws URISyntaxException {
        this.metadataParser = new MetadataParserServiceImpl(new ObjectMapper());
        this.runDirectory = Path.of(Objects.requireNonNull(getClass().getResource("/benchmarks/run_20250506_165951_1"))
                .toURI());
        this.malformedRunDirectory = Path.of(Objects.requireNonNull(getClass().getResource("/benchmarks/malformed_run"))
                .toURI());
    }

    @Nested
    class ParseBenchResultsTests {
        @Test
        void parseBenchResults_validFile_shouldReturnMetadata() throws RunsFileParsingException {
            Path validFile = runDirectory.resolve(BenchResultsMetadata.EXPECTED_FILE_NAME);

            ParsedBenchResults parsedResults = metadataParser.parseBenchResults(validFile);

            assertNotNull(parsedResults, "Parsed result wrapper should not be null.");
            assertTrue(parsedResults.warnings().isEmpty(), "Valid file should produce no warnings.");

            var metadata = parsedResults.metadata();
            assertNotNull(metadata, "Metadata should not be null for a valid file.");
            assertEquals("renaissance", metadata.benchmarkSuite(), "Benchmark suite should match.");
            assertEquals("scrabble", metadata.benchmarkName(), "Benchmark name should match.");
            assertEquals(
                    "5d94c2ee4e65e6a0ad564a588d47d76ed003bb7b", metadata.commitHash(), "Commit hash should match.");
            assertEquals("GraalVM CE 21.0.2", metadata.graalVersion(), "Graal version should match.");
            assertFalse(metadata.metricValues().isEmpty(), "Metric values should be populated.");
        }

        @Test
        void parseBenchResults_nonExistentFile_shouldThrowException() {
            Path nonExistentFile = runDirectory.resolve("non-existent-file.json");

            assertThrows(
                    RunsFileParsingException.class,
                    () -> metadataParser.parseBenchResults(nonExistentFile),
                    "Exception should be thrown for a non-existent file.");
        }

        @Test
        void parseBenchResults_malformedFile_shouldThrowException() {
            Path malformedFile = malformedRunDirectory.resolve("bench-results-malformed.json");

            assertThrows(
                    RunsFileParsingException.class,
                    () -> metadataParser.parseBenchResults(malformedFile),
                    "Exception should be thrown for a malformed JSON file.");
        }

        @Test
        void parseBenchResults_missingKey_shouldThrowException(@TempDir Path tempDir) throws IOException {
            Path missingKeyFile = tempDir.resolve("missing-key.json");
            Files.writeString(missingKeyFile, "{\"wrongKey\": [{\"bench-suite\": \"renaissance\"}]}");

            RunsFileParsingException exception = assertThrows(
                    RunsFileParsingException.class,
                    () -> metadataParser.parseBenchResults(missingKeyFile),
                    "Missing core structural key should throw a parsing exception.");
            assertTrue(
                    exception.getMessage().contains("Unexpected JSON structure"),
                    "Error message should mention unexpected structure.");
        }

        @Test
        void parseBenchResults_inconsistentData_shouldReturnWarnings(@TempDir Path tempDir)
                throws IOException, RunsFileParsingException {
            Path inconsistentFile = tempDir.resolve("inconsistent.json");

            String json = "{ \""
                    + BenchResultsMetadata.EXPECTED_FIRST_JSON_KEY
                    + "\": ["
                    + "{\"bench-suite\": \"suiteA\", \"benchmark\": \"bench\", \"metric.value\": 10.0},"
                    + "{\"bench-suite\": \"suiteB\", \"benchmark\": \"bench\", \"metric.value\": 15.0}"
                    + "]}";

            Files.writeString(inconsistentFile, json);

            ParsedBenchResults parsedResults = metadataParser.parseBenchResults(inconsistentFile);

            assertEquals(
                    "suiteA",
                    parsedResults.metadata().benchmarkSuite(),
                    "It should extract the first available valid data.");
            assertFalse(parsedResults.warnings().isEmpty(), "Parser should have collected validation errors.");
            assertTrue(
                    parsedResults.warnings().getFirst().message().contains("should not differ"),
                    "Warning message should highlight the difference.");
        }
    }

    @Nested
    class ParseProfilerMetadataTests {
        @Test
        void parseProfilerMetadata_validFile_shouldReturnMetadata() throws RunsFileParsingException {
            Path validFile = runDirectory.resolve("renaissance:scrabble.json");

            ProfileMetadata metadata = metadataParser.parseProfilerMetadata(validFile);

            assertNotNull(metadata, "Profiler metadata should not be null for a valid file.");
            assertEquals("JIT", metadata.compilationKind(), "Compilation kind should be parsed successfully.");
            assertEquals(352016328276L, metadata.totalPeriod(), "Total period should be parsed successfully.");
        }

        @Test
        void parseProfilerMetadata_nonExistentFile_shouldThrowException() {
            Path nonExistentFile = runDirectory.resolve("non-existent-file.json");

            assertThrows(
                    RunsFileParsingException.class,
                    () -> metadataParser.parseProfilerMetadata(nonExistentFile),
                    "Exception should be thrown for a non-existent file.");
        }

        @Test
        void parseProfilerMetadata_malformedFile_shouldThrowException() {
            Path malformedFile = malformedRunDirectory.resolve("renaissance:scrabble-malformed.json");

            assertThrows(
                    RunsFileParsingException.class,
                    () -> metadataParser.parseProfilerMetadata(malformedFile),
                    "Exception should be thrown for a malformed JSON file.");
        }
    }
}
